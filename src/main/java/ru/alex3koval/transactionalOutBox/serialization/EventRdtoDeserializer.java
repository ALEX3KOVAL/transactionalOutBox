package ru.alex3koval.transactionalOutBox.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import ru.alex3koval.eventingContract.dto.EventRDTO;
import ru.alex3koval.eventingContract.vo.EventStatus;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class EventRdtoDeserializer extends JsonDeserializer<EventRDTO> {
    @Override
    public EventRDTO deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        short rawEventStatus = node.get("status").shortValue();
        LocalDateTime createdAt = Instant
            .ofEpochMilli(node.get("createdAt").longValue())
            .atZone(ZoneOffset.UTC)
            .toLocalDateTime();

        return new EventRDTO(
            node.get("id").asText(),
            node.get("name").asText(),
            node.get("topic").asText(),
            node.get("json").asText(),
            EventStatus
                .of(rawEventStatus)
                .orElseThrow(() ->
                    new RuntimeException(String.format("Не удалось десериализовать из: %s", rawEventStatus))
                ),
            createdAt
        );
    }
}
