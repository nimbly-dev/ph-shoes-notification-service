package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.smtp")
@Data
public class NotificationSmtpProps {
    private String host = "localhost";
    private Integer port = 1025;
    private String username;
    private String password;
    private Boolean auth = false;
    private Boolean starttls = false;
    private Integer connectionTimeoutMillis = 2000;
    private Integer readTimeoutMillis = 2000;
    private Integer writeTimeoutMillis = 2000;
}
