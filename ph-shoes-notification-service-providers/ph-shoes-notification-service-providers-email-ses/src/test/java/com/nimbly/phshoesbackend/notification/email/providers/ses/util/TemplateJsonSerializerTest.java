package com.nimbly.phshoesbackend.notification.email.providers.ses.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateJsonSerializerTest {
    @Test
    void returnsEmptyJsonWhenMapMissing() {
        // Arrange
        TemplateJsonSerializer serializer = new TemplateJsonSerializer();

        // Act
        String json = serializer.toJson(null);

        // Assert
        assertThat(json).isEqualTo("{}");
    }

    @Test
    void serializesPrimitivesAndEscapesStrings() {
        // Arrange
        TemplateJsonSerializer serializer = new TemplateJsonSerializer();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", "A\"B");
        values.put("count", 2);
        values.put("flag", true);
        values.put("path", "C:\\temp");

        // Act
        String json = serializer.toJson(values);

        // Assert
        assertThat(json).contains("\"name\":\"A\\\"B\"");
        assertThat(json).contains("\"count\":2");
        assertThat(json).contains("\"flag\":true");
        assertThat(json).contains("\"path\":\"C:\\\\temp\"");
    }
}
