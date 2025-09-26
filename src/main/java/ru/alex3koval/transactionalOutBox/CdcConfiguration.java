package ru.alex3koval.transactionalOutBox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.embedded.Connect;
import io.debezium.embedded.EmbeddedEngineConfig;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ru.alex3koval.eventingContract.dto.EventRDTO;
import ru.alex3koval.eventingImpl.EventingConfiguration;
import ru.alex3koval.transactionalOutBox.core.StringConverters;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Configuration
@RequiredArgsConstructor
@Import(EventingConfiguration.class)
public class CdcConfiguration {
    @Bean
    public DebeziumEngine<ChangeEvent<SourceRecord, SourceRecord>> debeziumEngine(
        @Qualifier("debeziumProperties") Properties props,
        HandleCdcEventConsumer handleCdcEventConsumer,
        @Qualifier("cdcEventHandlerExecutorService") ExecutorService cdcEventHandlerExecutorService,
        @Qualifier("consoleLogger") Logger logger
    ) {
        return DebeziumEngine
            .create(Connect.class)
            .using(props)
            .notifying(event ->
                CompletableFuture
                    .supplyAsync(() -> {
                        handleCdcEventConsumer.accept(event.value());
                        return null;
                    }, cdcEventHandlerExecutorService)
                    .exceptionally(exc -> {
                        logger.error(
                            "{}\n{}",
                            exc.getMessage(),
                            StringConverters.stackTracesToString(exc.getStackTrace())
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

    @Qualifier("debeziumProperties")
    @Bean
    Properties debeziumConfig(CdcProperties cdcProps) throws ClassNotFoundException {
        Properties properties = new Properties();

        properties.put(EmbeddedEngineConfig.CONNECTOR_CLASS.name(), PostgresConnector.class);
        properties.put("publication.name", cdcProps.publicationName());
        properties.put("slot.name", cdcProps.slotName());
        properties.put("name", cdcProps.name());
        properties.put("plugin.name", cdcProps.pluginName());
        properties.put(EmbeddedEngineConfig.OFFSET_STORAGE.name(), Class.forName(cdcProps.offsetStorageClass()));
        properties.put(EmbeddedEngineConfig.OFFSET_STORAGE_FILE_FILENAME.name(), cdcProps.offsetStorageFileName());
        properties.put(EmbeddedEngineConfig.OFFSET_FLUSH_INTERVAL_MS.name(), cdcProps.offsetFlushIntervalMillis());
        properties.put("database.hostname", cdcProps.dbHost());
        properties.put("database.port", cdcProps.dbPort());
        properties.put("database.user", cdcProps.dbUser());
        properties.put("database.password", cdcProps.dbPassword());
        properties.put("database.dbname", cdcProps.dbName());
        properties.put("database.server.name", cdcProps.databaseServerName());
        properties.put("table.whitelist", cdcProps.tableWhitelist());
        properties.put("column.include.list", cdcProps.columnIncludeList());

        return properties;
    }

    @Qualifier("cdcEventHandlerExecutorService")
    @Bean
    ExecutorService cdcEventHandlerExecutor() {
        return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
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
