package com.bank.ai.rag.store;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * float[] ↔ PostgreSQL vector 문자열 형식 "[v1,v2,...]" 변환.
 * PostgreSQL vector 타입은 text 입력을 그대로 받으므로 별도 JDBC 확장 없이 동작한다.
 */
@Converter
public class VectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        return toVectorString(attribute);
    }

    /** float[] → PostgreSQL vector 리터럴 {@code [v1,v2,...]} */
    public static String toVectorString(float[] v) {
        if (v == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String inner = dbData.startsWith("[") ? dbData.substring(1, dbData.length() - 1) : dbData;
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
