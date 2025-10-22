package ru.alex3koval.transactionalOutBox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.embedded.Connect;
import io.debezium.embedded.EmbeddedEngineConfig;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.alex3koval.eventingContract.dto.EventRDTO;
import ru.alex3koval.eventingImpl.EventingConfiguration;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Import(EventingConfiguration.class)
public class TransactionalOutBoxConfiguration {
    @Bean
    public DebeziumEngine<ChangeEvent<SourceRecord, SourceRecord>> debeziumEngine(
        @Qualifier("debeziumProperties") Properties props,
        HandleCdcEventConsumer handleCdcEventConsumer,
        @Qualifier("cdcEventHandlerExecutorService") ExecutorService cdcEventHandlerExecutorService
    ) {
        return DebeziumEngine
            .create(Connect.class)
            .using(props)
            .notifying(event ->
                CompletableFuture
                    .runAsync(
                        () -> handleCdcEventConsumer.accept(event.value()),
                        cdcEventHandlerExecutorService
                    )
                    .exceptionally(exc -> {
                        log.error(
                            "Не удалось обработать CDC-событие",
                            exc
                        );
                        return null;
                    })
            )
            .build();
    }

    @Bean
    TransactionalOutBoxAppRunner transactionalOutBoxAppRunner(
        DebeziumEngine<ChangeEvent<SourceRecord, SourceRecord>> debeziumEngine
    ) {
        return new TransactionalOutBoxAppRunner(debeziumEngine);
    }

    @Bean("debeziumProperties")
    Properties debeziumConfig(CdcProperties cdcProps) {
        Properties properties = new Properties();

        properties.put(EmbeddedEngineConfig.CONNECTOR_CLASS.name(), PostgresConnector.class.getCanonicalName());
        properties.put("publication.name", cdcProps.publicationName());
        properties.put("slot.name", cdcProps.slotName());
        properties.put("name", cdcProps.name());
        properties.put("plugin.name", cdcProps.pluginName());
        properties.put(EmbeddedEngineConfig.OFFSET_STORAGE.name(), cdcProps.offsetStorageClass());
        properties.put(
            EmbeddedEngineConfig.OFFSET_STORAGE_FILE_FILENAME.name(),
            String.format(
                "%s/%s",
                cdcProps.offsetStorageFileFolderPath(),
                cdcProps.offsetStorageFileName()
            )
        );
        properties.setProperty(
            EmbeddedEngineConfig.OFFSET_FLUSH_INTERVAL_MS.name(),
            cdcProps.offsetFlushIntervalMillis().toString()
        );
        properties.put("database.hostname", cdcProps.dbHost());
        properties.put("database.port", cdcProps.dbPort());
        properties.put("database.user", cdcProps.dbUser());
        properties.put("database.password", cdcProps.dbPassword());
        properties.put("database.dbname", cdcProps.dbName());
        properties.put("topic.prefix", cdcProps.topicPrefix());
        properties.put("schema.include.list", cdcProps.schemaIncludeList());
        properties.put("table.include.list", cdcProps.tableIncludeList());
        properties.put("column.include.list", cdcProps.columnIncludeList());

        properties.setProperty("transforms", "valueToKey,unwrap,renameFields");

        properties.setProperty("transforms.valueToKey.type", "org.apache.kafka.connect.transforms.ValueToKey");
        properties.setProperty("transforms.valueToKey.fields", "op");

        properties.setProperty("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        properties.put("transforms.unwrap.drop.tombstones", "false");
        properties.put("transforms.unwrap.delete.handling.mode", "rewrite");

        properties.setProperty("transforms.renameFields.type", "org.apache.kafka.connect.transforms.ReplaceField$Value");
        properties.setProperty(
            "transforms.renameFields.renames",
            "created_at:createdAt,updated_at:updatedAt"
        );

        return properties;
    }

    @Bean
    HandleCdcEventConsumer handleEventConsumer(
        @Qualifier("pushEventConsumer") Consumer<EventRDTO> pushEvent,
        ObjectMapper mapper
    ) {
        return new HandleCdcEventConsumer(
            pushEvent,
            mapper
        );
    }
}
