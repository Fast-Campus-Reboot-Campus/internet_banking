package com.bank.loan.advisory.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Outbox JSON payload → Avro GenericRecord 변환기.
 *
 * Outbox 테이블은 String JSON으로 저장 (가독성·DB 쿼리 가능).
 * Kafka 발행 시점에 Avro로 변환 — 스키마 레지스트리에 등록·검증됨.
 *
 * 스키마 파일 위치: classpath:avro/*.avsc
 * GenericRecord 사용 이유: 코드 생성(avro-plugin) 없이 런타임에 스키마 로드 가능.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdvisoryAvroConverter {

    private final ObjectMapper objectMapper;

    private static final Map<String, String> TOPIC_TO_SCHEMA = Map.of(
            AdvisoryKafkaOutboxMessage.TOPIC_QUARANTINE, "avro/quarantine_triggered_v1.avsc",
            AdvisoryKafkaOutboxMessage.TOPIC_REPORT,     "avro/report_published_v1.avsc"
    );

    /**
     * JSON payload를 해당 토픽의 Avro GenericRecord로 변환.
     * 스키마에 없는 필드는 무시. 스키마 필드 누락 시 예외.
     */
    public GenericRecord toGenericRecord(String topic, String jsonPayload) {
        String schemaFile = TOPIC_TO_SCHEMA.get(topic);
        if (schemaFile == null) {
            throw new IllegalArgumentException("Avro 스키마 미등록 토픽: " + topic);
        }

        Schema schema = loadSchema(schemaFile);
        JsonNode node;
        try {
            node = objectMapper.readTree(jsonPayload);
        } catch (IOException e) {
            throw new RuntimeException("Avro 변환 실패 — JSON 파싱 오류: " + jsonPayload, e);
        }

        GenericRecord record = new GenericData.Record(schema);
        for (Schema.Field field : schema.getFields()) {
            JsonNode value = node.get(field.name());
            if (value == null || value.isNull()) {
                record.put(field.name(), null);
                continue;
            }
            record.put(field.name(), convertField(field.schema(), value));
        }
        return record;
    }

    private Object convertField(Schema fieldSchema, JsonNode value) {
        // UNION(nullable) 처리: ["null", "long"] → 실제 타입 추출
        if (fieldSchema.getType() == Schema.Type.UNION) {
            fieldSchema = fieldSchema.getTypes().stream()
                    .filter(s -> s.getType() != Schema.Type.NULL)
                    .findFirst()
                    .orElseThrow();
        }
        return switch (fieldSchema.getType()) {
            case LONG    -> value.asLong();
            case INT     -> value.asInt();
            case STRING  -> value.asText();
            case BOOLEAN -> value.asBoolean();
            case ARRAY   -> {
                List<Object> list = new java.util.ArrayList<>();
                Schema itemSchema = fieldSchema.getElementType();
                value.forEach(item -> list.add(convertField(itemSchema, item)));
                yield new GenericData.Array<>(fieldSchema, list);
            }
            default -> value.asText();
        };
    }

    private Schema loadSchema(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Avro 스키마 파일 없음: " + resourcePath);
            }
            return new Schema.Parser().parse(is);
        } catch (IOException e) {
            throw new RuntimeException("Avro 스키마 로드 실패: " + resourcePath, e);
        }
    }
}
