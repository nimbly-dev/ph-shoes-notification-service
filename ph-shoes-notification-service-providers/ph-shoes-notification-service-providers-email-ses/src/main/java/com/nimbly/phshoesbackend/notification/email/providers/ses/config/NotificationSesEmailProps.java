package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
@Data
public class NotificationSesEmailProps {
    private String from;                  // "PH Shoes <no-reply@ph-shoes.app>"
    private String subjectPrefix;         // "[PH-Shoes]"
    private String listUnsubscribe;       // "<mailto:...>, <https://.../unsubscribe>"
    private String listUnsubscribePost;   // "List-Unsubscribe=One-Click"
}
