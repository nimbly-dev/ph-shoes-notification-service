package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.ses")
@Data
public class NotificationSesProps {
    private String region;    // e.g. "ap-southeast-1"
    private String endpoint;  // e.g. "http://localstack:4566" (dev only)
    private String configurationSet; // optional
}
