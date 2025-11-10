package com.nimbly.phshoesbackend.notification.core.ses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.ses.config.SesWebhookProperties;
import com.nimbly.phshoesbackend.services.common.core.model.SuppressionEntry;
import com.nimbly.phshoesbackend.services.common.core.model.SuppressionReason;
import com.nimbly.phshoesbackend.services.common.core.repository.SuppressionRepository;
import com.nimbly.phshoesbackend.services.common.core.security.EmailCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SesWebhookProcessorTest {

    private final SuppressionRepository suppressionRepository = mock(SuppressionRepository.class);
    private final EmailCrypto emailCrypto = mock(EmailCrypto.class);
    private final SesWebhookProperties properties = new SesWebhookProperties();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private SesWebhookProcessor processor;

    @BeforeEach
    void setUp() {
        properties.setVerifySignature(false);
        processor = new SesWebhookProcessor(suppressionRepository, emailCrypto, properties, objectMapper);
        when(emailCrypto.normalize(anyString())).thenAnswer(inv -> {
            String input = inv.getArgument(0);
            return input == null ? null : input.trim().toLowerCase();
        });
        when(emailCrypto.hash(anyString())).thenReturn("hash-value");
    }

    @Test
    void suppressesHardBounce() {
        processor.process(buildEnvelopeJson(hardBouncePayload()), "Notification");

        ArgumentCaptor<SuppressionEntry> captor = ArgumentCaptor.forClass(SuppressionEntry.class);
        verify(suppressionRepository).put(captor.capture());

        SuppressionEntry entry = captor.getValue();
        assertThat(entry.getReason()).isEqualTo(SuppressionReason.BOUNCE_HARD);
        assertThat(entry.getSource()).isEqualTo("ses-bounce");
        assertThat(entry.getEmailHash()).isEqualTo("hash-value");
    }

    @Test
    void ignoresSoftBounce() {
        processor.process(buildEnvelopeJson(softBouncePayload()), "Notification");
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void suppressesComplaint() {
        processor.process(buildEnvelopeJson(complaintPayload()), "Notification");

        ArgumentCaptor<SuppressionEntry> captor = ArgumentCaptor.forClass(SuppressionEntry.class);
        verify(suppressionRepository).put(captor.capture());

        SuppressionEntry entry = captor.getValue();
        assertThat(entry.getReason()).isEqualTo(SuppressionReason.COMPLAINT);
        assertThat(entry.getSource()).isEqualTo("ses-complaint");
    }

    private String buildEnvelopeJson(String messagePayload) {
        try {
            String messageField = objectMapper.writeValueAsString(messagePayload);
            return """
                    {
                      "Type": "Notification",
                      "MessageId": "id-123",
                      "TopicArn": "arn:aws:sns:us-east-1:123:topic",
                      "Message": %s,
                      "Timestamp": "2024-01-01T00:00:00.000Z",
                      "Signature": "skip",
                      "SigningCertURL": "https://sns.us-east-1.amazonaws.com/cert.pem"
                    }
                    """.formatted(messageField);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String hardBouncePayload() {
        return """
                {
                  "notificationType": "Bounce",
                  "bounce": {
                    "bounceType": "Permanent",
                    "bounceSubType": "General",
                    "bouncedRecipients": [
                      {
                        "emailAddress": "user@example.com",
                        "diagnosticCode": "550 5.1.1"
                      }
                    ]
                  },
                  "mail": {
                    "messageId": "msg-1"
                  }
                }
                """;
    }

    private String softBouncePayload() {
        return """
                {
                  "notificationType": "Bounce",
                  "bounce": {
                    "bounceType": "Transient",
                    "bounceSubType": "General",
                    "bouncedRecipients": [
                      {
                        "emailAddress": "user@example.com"
                      }
                    ]
                  },
                  "mail": {}
                }
                """;
    }

    private String complaintPayload() {
        return """
                {
                  "notificationType": "Complaint",
                  "complaint": {
                    "complainedRecipients": [
                      { "emailAddress": "user@example.com" }
                    ]
                  }
                }
                """;
    }
}
