package com.nimbly.phshoesbackend.notification.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * Optional neutral event shape if you publish/consume notification lifecycle events.
 */
@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationEvent {
    NotificationEventType type;
    String email;
    String provider;
    String rawPayloadId;       // e.g., SNS message id or webhook delivery id
    Instant occurredAt;

    /**
     * Free-form details (bounce subtype, complaint feedback type, smtp response, etc.).
     */
    Map<String, Object> details;
}