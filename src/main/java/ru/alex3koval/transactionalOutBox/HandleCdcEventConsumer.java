package ru.alex3koval.transactionalOutBox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.data.Envelope;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import ru.alex3koval.eventingContract.dto.EventRDTO;
import ru.alex3koval.eventingContract.vo.EventStatus;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class HandleCdcEventConsumer implements Consumer<SourceRecord> {
    private final Consumer<EventRDTO> pushEvent;
    private final ObjectMapper mapper;

    @Override
    public void accept(SourceRecord record) {
        if (record.value() instanceof Struct recordStruct) {
            Object operation = recordStruct.get(Envelope.FieldName.OPERATION);

            if (operation == Envelope.Operation.READ || operation == Envelope.Operation.DELETE) {
                return;
            }

            Object recordAfterChange = recordStruct.get(Envelope.FieldName.AFTER);

            if (recordAfterChange == null) {
                return;
            }

            Struct structAfterChange = (Struct) recordAfterChange;

            Optional<EventStatus> status = EventStatus
                .of(Short.parseShort(structAfterChange.get("status").toString()));

            status.orElseThrow();

            if (!status.get().isCreated()) {
                return;
            }

            Map<String, String> events = structAfterChange.schema().fields().stream()
                .flatMap(field -> {
                    Object fieldValue = structAfterChange.get(field.name());

                    return fieldValue != null
                        ? Stream.of(
                            new AbstractMap.SimpleEntry<>(
                                field.name(),
                                fieldValue.toString()
                            )
                        )
                        : Stream.empty();
                })
                .collect(
                    Collectors.toMap(
                        AbstractMap.SimpleEntry::getKey,
                        AbstractMap.SimpleEntry::getValue
                    )
                );

            try {
                EventRDTO rdto = mapper.readValue(
                    mapper.writeValueAsString(events),
                    EventRDTO.class
                );

                pushEvent.accept(rdto);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
