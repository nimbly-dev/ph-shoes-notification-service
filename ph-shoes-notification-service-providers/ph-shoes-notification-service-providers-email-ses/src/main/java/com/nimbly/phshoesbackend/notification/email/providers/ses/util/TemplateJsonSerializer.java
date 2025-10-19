package com.nimbly.phshoesbackend.notification.email.providers.ses.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TemplateJsonSerializer {
    public String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append('"').append(escape(String.valueOf(v))).append('"');
        }
        return sb.append('}').toString();
    }
    private String escape(String s) { return s.replace("\\","\\\\").replace("\"","\\\""); }
}
