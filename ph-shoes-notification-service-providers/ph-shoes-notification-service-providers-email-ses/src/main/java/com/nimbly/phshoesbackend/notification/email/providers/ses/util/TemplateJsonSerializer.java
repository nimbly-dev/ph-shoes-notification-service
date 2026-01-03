package com.nimbly.phshoesbackend.notification.email.providers.ses.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateJsonSerializer {
    public String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) sb.append("null");
            else if (value instanceof Number || value instanceof Boolean) sb.append(value.toString());
            else sb.append('"').append(escape(String.valueOf(value))).append('"');
        }
        return sb.append('}').toString();
    }
    private String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\""); }
}
