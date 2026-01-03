package com.nimbly.phshoesbackend.notification.core.ses;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.ses.config.SesWebhookProperties;
import com.nimbly.phshoesbackend.commons.core.model.SuppressionEntry;
import com.nimbly.phshoesbackend.commons.core.model.SuppressionReason;
import com.nimbly.phshoesbackend.commons.core.repository.SuppressionRepository;
import com.nimbly.phshoesbackend.commons.core.security.EmailCrypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void rejectsEmptyPayload() {
        // Arrange
        String payload = " ";

        // Act + Assert
        assertThatThrownBy(() -> processor.process(payload, "Notification"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsInvalidEnvelopeJson() {
        // Arrange
        String payload = "not-json";

        // Act + Assert
        assertThatThrownBy(() -> processor.process(payload, "Notification"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ignoresUnknownNotificationType() {
        // Arrange
        String payload = """
                {
                  "notificationType": "Delivery"
                }
                """;

        // Act
        processor.process(buildEnvelopeJson(payload), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void skipsTopicsNotInAllowList() {
        // Arrange
        properties.setAllowedTopics(List.of("arn:aws:sns:us-east-1:123:other"));

        // Act
        processor.process(buildEnvelopeJson(hardBouncePayload()), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void rejectsWhenSignatureVerificationFails() {
        // Arrange
        properties.setVerifySignature(true);
        SesWebhookProcessor verifyingProcessor = new SesWebhookProcessor(
                suppressionRepository,
                emailCrypto,
                properties,
                objectMapper
        );
        String payload = """
                {
                  "Type": "Notification",
                  "MessageId": "id-123",
                  "TopicArn": "arn:aws:sns:us-east-1:123:topic",
                  "Message": "{}",
                  "Timestamp": "2024-01-01T00:00:00.000Z"
                }
                """;

        // Act + Assert
        assertThatThrownBy(() -> verifyingProcessor.process(payload, "Notification"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ignoresMissingNotificationBody() {
        // Arrange
        String payload = "";

        // Act
        processor.process(buildEnvelopeJson(payload), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void logsMismatchHeaderButStillProcessesNotification() {
        // Arrange
        String payload = buildEnvelopeJson(hardBouncePayload());

        // Act
        processor.process(payload, "SubscriptionConfirmation");

        // Assert
        verify(suppressionRepository).put(any(SuppressionEntry.class));
    }

    @Test
    void skipsBounceWhenNoRecipients() {
        // Arrange
        String payload = """
                {
                  "notificationType": "Bounce",
                  "bounce": {
                    "bounceType": "Permanent",
                    "bouncedRecipients": []
                  },
                  "mail": {
                    "messageId": "msg-1"
                  }
                }
                """;

        // Act
        processor.process(buildEnvelopeJson(payload), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void skipsBounceWhenEmailBlank() {
        // Arrange
        String payload = """
                {
                  "notificationType": "Bounce",
                  "bounce": {
                    "bounceType": "Permanent",
                    "bouncedRecipients": [
                      {
                        "emailAddress": " "
                      }
                    ]
                  },
                  "mail": {
                    "messageId": "msg-1"
                  }
                }
                """;

        // Act
        processor.process(buildEnvelopeJson(payload), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void skipsComplaintWhenNoRecipients() {
        // Arrange
        String payload = """
                {
                  "notificationType": "Complaint",
                  "complaint": {
                    "complainedRecipients": []
                  }
                }
                """;

        // Act
        processor.process(buildEnvelopeJson(payload), "Notification");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void acceptsAllowedTopicWildcard() {
        // Arrange
        properties.setAllowedTopics(List.of("*"));

        // Act
        processor.process(buildEnvelopeJson(hardBouncePayload()), "Notification");

        // Assert
        verify(suppressionRepository).put(any(SuppressionEntry.class));
    }

    @Test
    void skipsAutoConfirmWhenDisabled() {
        // Arrange
        SesWebhookProperties localProps = new SesWebhookProperties();
        localProps.setVerifySignature(false);
        localProps.setAutoConfirmSubscriptions(false);
        SesWebhookProcessor localProcessor = new SesWebhookProcessor(
                suppressionRepository,
                emailCrypto,
                localProps,
                objectMapper
        );
        String payload = """
                {
                  "Type": "SubscriptionConfirmation",
                  "MessageId": "id-123",
                  "TopicArn": "arn:aws:sns:us-east-1:123:topic",
                  "Message": "confirm",
                  "SubscribeURL": "https://example.com/confirm",
                  "Timestamp": "2024-01-01T00:00:00.000Z"
                }
                """;

        // Act
        localProcessor.process(payload, "SubscriptionConfirmation");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void ignoresSubscriptionWhenUrlMissing() {
        // Arrange
        String payload = """
                {
                  "Type": "SubscriptionConfirmation",
                  "MessageId": "id-123",
                  "TopicArn": "arn:aws:sns:us-east-1:123:topic",
                  "Message": "confirm",
                  "Timestamp": "2024-01-01T00:00:00.000Z"
                }
                """;

        // Act
        processor.process(payload, "SubscriptionConfirmation");

        // Assert
        verifyNoInteractions(suppressionRepository);
    }

    @Test
    void ignoresUnknownSnsType() {
        // Arrange
        String payload = """
                {
                  "Type": "Other",
                  "MessageId": "id-123",
                  "TopicArn": "arn:aws:sns:us-east-1:123:topic",
                  "Message": "msg",
                  "Timestamp": "2024-01-01T00:00:00.000Z"
                }
                """;

        // Act
        processor.process(payload, "Other");

        // Assert
        verifyNoInteractions(suppressionRepository);
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
