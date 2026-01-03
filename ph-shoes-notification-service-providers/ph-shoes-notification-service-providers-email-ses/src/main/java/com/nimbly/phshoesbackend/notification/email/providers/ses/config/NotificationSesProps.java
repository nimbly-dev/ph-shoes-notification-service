package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.ses")
@Data
public class NotificationSesProps {
    private String region;
    private String endpoint;
    private String configurationSet;
}
