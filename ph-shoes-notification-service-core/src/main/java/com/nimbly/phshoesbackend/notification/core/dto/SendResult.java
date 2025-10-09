package com.nimbly.phshoesbackend.notification.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SendResult {
    /**
     * Provider-assigned message identifier (e.g., SES MessageId, SMTP queue id).
     */
    String messageId;

    /**
     * Logical provider name ("ses", "smtp", etc.) for observability.
     */
    String provider;

    /**
     * When the provider accepted the message.
     */
    Instant acceptedAt;

    /**
     * Optional idempotency/request correlation id.
     */
    String requestId;
}