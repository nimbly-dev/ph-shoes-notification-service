package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
@Data
public class NotificationSesEmailProps {
    private String from;
    private String subjectPrefix;
    private String listUnsubscribe;
    private String listUnsubscribePost;
}
