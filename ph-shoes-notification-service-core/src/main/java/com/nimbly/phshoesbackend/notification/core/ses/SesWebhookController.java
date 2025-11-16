package com.nimbly.phshoesbackend.notification.core.ses;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapter that hands SES SNS payloads to {@link SesWebhookProcessor}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("${notification.ses.webhook.path:/internal/webhooks/ses}")
public class SesWebhookController {

    private static final String HEADER_SNS_MESSAGE_TYPE = "x-amz-sns-message-type";
    private final SesWebhookProcessor sesWebhookProcessor;

    @PostMapping(consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Void> handleWebhook(
            @RequestBody(required = false) String body,
            @RequestHeader(value = HEADER_SNS_MESSAGE_TYPE, required = false) String messageType) {
        sesWebhookProcessor.process(body, messageType);
        return ResponseEntity.noContent().build();
    }
}
