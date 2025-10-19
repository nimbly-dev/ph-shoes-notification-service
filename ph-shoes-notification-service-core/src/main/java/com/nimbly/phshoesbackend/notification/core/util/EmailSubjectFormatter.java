package com.nimbly.phshoesbackend.notification.core.util;

import org.springframework.stereotype.Component;

@Component
public class EmailSubjectFormatter  {
    public String withPrefix(String subject, String prefix) {
        if (prefix == null || prefix.isBlank()) return subject == null ? "" : subject;
        return (subject == null || subject.isBlank()) ? prefix : prefix + " " + subject;
    }
}
