package com.nimbly.phshoesbackend.notification.core.model.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
@Data
public class NotificationTransportProps {
    /**
     * Transport: "sesv2" or "smtp". Default: sesv2.
     */
    private String transport = "sesv2";
}
