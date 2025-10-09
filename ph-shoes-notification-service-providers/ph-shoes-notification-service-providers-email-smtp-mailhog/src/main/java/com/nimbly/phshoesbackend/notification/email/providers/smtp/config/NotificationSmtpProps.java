package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.smtp")
@Data
public class NotificationSmtpProps {
    private String defaultFrom;
}
