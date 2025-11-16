package com.nimbly.phshoesbackend.notification.core.ses.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "notification.ses.webhook")
public class SesWebhookProperties {

    /**
     * When true, exposes {@code notification.ses.webhook.path} as an SNS endpoint.
     */
    private boolean enabled = false;

    /**
     * When true, validates SNS signatures against the published certificate.
     */
    private boolean verifySignature = true;

    /**
     * Automatically confirm SubscriptionConfirmation messages (dev/staging convenience).
     */
    private boolean autoConfirmSubscriptions = true;

    /**
     * Optional allowlist of SNS TopicArns. Empty list accepts any topic.
     */
    private List<String> allowedTopics = new ArrayList<>();

    /**
     * Path that receives SNS payloads when enabled.
     */
    private String path = "/internal/webhooks/ses";
}
