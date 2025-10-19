package com.nimbly.phshoesbackend.notification.core.model.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
@Data
public class NotificationEmailProps {
    private String from;
    private String subjectPrefix;
    private String listUnsubscribe;
    private String listUnsubscribePost;
}
