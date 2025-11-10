package com.nimbly.phshoesbackend.notification.core.ses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.ses.config.SesWebhookProperties;
import com.nimbly.phshoesbackend.services.common.core.model.SuppressionEntry;
import com.nimbly.phshoesbackend.services.common.core.model.SuppressionReason;
import com.nimbly.phshoesbackend.services.common.core.repository.SuppressionRepository;
import com.nimbly.phshoesbackend.services.common.core.security.EmailCrypto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class SesWebhookProcessor {

    private static final String SOURCE_BOUNCE = "ses-bounce";
    private static final String SOURCE_COMPLAINT = "ses-complaint";

    private final SuppressionRepository suppressionRepository;
    private final EmailCrypto emailCrypto;
    private final SesWebhookProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Entry point used by HTTP controllers or queue consumers that receive SNS payloads.
     */
    public void process(String rawPayload, String snsMessageTypeHeader) {
        if (!StringUtils.hasText(rawPayload)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty SNS payload");
        }

        SnsEnvelope envelope = parseEnvelope(rawPayload);
        if (StringUtils.hasText(snsMessageTypeHeader) &&
                !snsMessageTypeHeader.equalsIgnoreCase(envelope.type())) {
            log.warn("sns.message_type_mismatch header={} body={}", snsMessageTypeHeader, envelope.type());
        }

        if (!topicAllowed(envelope.topicArn())) {
            log.warn("sns.topic_rejected topicArn={}", envelope.topicArn());
            return;
        }

        if (props.isVerifySignature() && !verifySignature(envelope)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SNS signature");
        }

        switch (envelope.messageType()) {
            case NOTIFICATION -> handleNotification(envelope);
            case SUBSCRIPTION_CONFIRMATION -> confirmSubscription(envelope);
            case UNSUBSCRIBE_CONFIRMATION -> log.warn("sns.unsubscribe_confirmation topicArn={}", envelope.topicArn());
            case UNKNOWN -> log.warn("sns.unknown_type type={} messageId={}", envelope.type(), envelope.messageId());
        }
    }

    private void handleNotification(SnsEnvelope envelope) {
        if (!StringUtils.hasText(envelope.message())) {
            log.warn("sns.notification.missing_body id={}", envelope.messageId());
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(envelope.message());
        } catch (JsonProcessingException e) {
            log.warn("sns.notification.bad_json id={} msg={}", envelope.messageId(), e.getMessage());
            return;
        }

        String notificationType = root.path("notificationType").asText("");
        if ("Bounce".equalsIgnoreCase(notificationType)) {
            handleBounce(root, envelope);
        } else if ("Complaint".equalsIgnoreCase(notificationType)) {
            handleComplaint(root);
        } else {
            log.debug("sns.notification.ignored type={} id={}", notificationType, envelope.messageId());
        }
    }

    private void handleBounce(JsonNode payload, SnsEnvelope envelope) {
        JsonNode bounce = payload.path("bounce");
        String bounceType = bounce.path("bounceType").asText("");
        String bounceSubType = bounce.path("bounceSubType").asText("");

        JsonNode recipients = bounce.path("bouncedRecipients");
        if (recipients == null || !recipients.isArray() || recipients.isEmpty()) {
            log.warn("sns.bounce.no_recipients id={}", envelope.messageId());
            return;
        }

        if (!isHardBounce(bounceType)) {
            log.info("sns.bounce.soft type={} subType={} id={}", bounceType, bounceSubType, envelope.messageId());
            return;
        }

        String messageId = payload.path("mail").path("messageId").asText("");
        recipients.forEach(recipient -> {
            String email = recipient.path("emailAddress").asText("");
            if (!StringUtils.hasText(email)) {
                return;
            }
            String normalized = emailCrypto.normalize(email);
            if (!StringUtils.hasText(normalized)) {
                return;
            }

            SuppressionEntry entry = new SuppressionEntry();
            entry.setEmailHash(emailCrypto.hash(normalized));
            entry.setReason(SuppressionReason.BOUNCE_HARD);
            entry.setSource(SOURCE_BOUNCE);
            entry.setCreatedAt(Instant.now());
            entry.setNotes(buildBounceNote(
                    bounceType,
                    bounceSubType,
                    recipient.path("diagnosticCode").asText(""),
                    messageId));
            suppressionRepository.put(entry);
            log.info("sns.bounce.hard_suppressed hashPrefix={} type={} subType={}",
                    shortHash(entry.getEmailHash()), bounceType, bounceSubType);
        });
    }

    private void handleComplaint(JsonNode payload) {
        JsonNode recipients = payload.path("complaint").path("complainedRecipients");
        if (recipients == null || !recipients.isArray() || recipients.isEmpty()) {
            log.warn("sns.complaint.no_recipients");
            return;
        }
        recipients.forEach(recipient -> {
            String email = recipient.path("emailAddress").asText("");
            String normalized = emailCrypto.normalize(email);
            if (!StringUtils.hasText(normalized)) {
                return;
            }
            SuppressionEntry entry = new SuppressionEntry();
            entry.setEmailHash(emailCrypto.hash(normalized));
            entry.setReason(SuppressionReason.COMPLAINT);
            entry.setSource(SOURCE_COMPLAINT);
            entry.setCreatedAt(Instant.now());
            entry.setNotes("SES complaint notification");
            suppressionRepository.put(entry);
            log.info("sns.complaint.suppressed hashPrefix={}", shortHash(entry.getEmailHash()));
        });
    }

    private void confirmSubscription(SnsEnvelope envelope) {
        if (!props.isAutoConfirmSubscriptions()) {
            log.info("sns.subscription.skip_auto_confirm topicArn={}", envelope.topicArn());
            return;
        }
        if (!StringUtils.hasText(envelope.subscribeURL())) {
            log.warn("sns.subscription.missing_url topicArn={}", envelope.topicArn());
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(envelope.subscribeURL()))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("sns.subscription.confirmed topicArn={}", envelope.topicArn());
            } else {
                log.warn("sns.subscription.confirm_failed topicArn={} status={}", envelope.topicArn(), response.statusCode());
            }
        } catch (Exception e) {
            log.warn("sns.subscription.confirm_error topicArn={} msg={}", envelope.topicArn(), e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean verifySignature(SnsEnvelope envelope) {
        if (!StringUtils.hasText(envelope.signature()) || !StringUtils.hasText(envelope.signingCertUrl())) {
            log.warn("sns.signature.missing_fields");
            return false;
        }

        URI certUri;
        try {
            certUri = new URI(envelope.signingCertUrl());
        } catch (URISyntaxException e) {
            log.warn("sns.signature.bad_cert_url url={}", envelope.signingCertUrl());
            return false;
        }
        if (!"https".equalsIgnoreCase(certUri.getScheme()) ||
                certUri.getHost() == null ||
                !certUri.getHost().toLowerCase(Locale.ROOT).endsWith(".amazonaws.com")) {
            log.warn("sns.signature.untrusted_cert url={}", envelope.signingCertUrl());
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(certUri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("sns.signature.cert_fetch_failed status={}", response.statusCode());
                return false;
            }

            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(response.body()));

            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(certificate.getPublicKey());
            signature.update(buildStringToSign(envelope).getBytes(StandardCharsets.UTF_8));

            boolean valid = signature.verify(Base64.getDecoder().decode(envelope.signature()));
            if (!valid) {
                log.warn("sns.signature.invalid");
            }
            return valid;
        } catch (GeneralSecurityException | java.io.IOException | InterruptedException e) {
            log.warn("sns.signature.verify_error msg={}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private boolean topicAllowed(String topicArn) {
        if (CollectionUtils.isEmpty(props.getAllowedTopics())) {
            return true;
        }
        return props.getAllowedTopics().contains(topicArn) || props.getAllowedTopics().contains("*");
    }

    private SnsEnvelope parseEnvelope(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, SnsEnvelope.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SNS payload");
        }
    }

    private static boolean isHardBounce(String bounceType) {
        if (!StringUtils.hasText(bounceType)) {
            return false;
        }
        String normalized = bounceType.toLowerCase(Locale.ROOT);
        return "permanent".equals(normalized) || "undetermined".equals(normalized);
    }

    private static String buildBounceNote(String type,
                                          String subType,
                                          String diagnostic,
                                          String messageId) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(type);
        if (StringUtils.hasText(subType)) {
            sb.append(" subType=").append(subType);
        }
        if (StringUtils.hasText(diagnostic)) {
            sb.append(" diag=").append(diagnostic);
        }
        if (StringUtils.hasText(messageId)) {
            sb.append(" messageId=").append(messageId);
        }
        return sb.toString();
    }

    private static String shortHash(String hash) {
        if (!StringUtils.hasText(hash)) {
            return "(blank)";
        }
        return hash.length() <= 8 ? hash : hash.substring(0, 8);
    }

    private static String buildStringToSign(SnsEnvelope envelope) {
        StringBuilder sb = new StringBuilder();
        switch (envelope.messageType()) {
            case NOTIFICATION -> {
                appendField(sb, "Message", envelope.message());
                appendField(sb, "MessageId", envelope.messageId());
                appendField(sb, "Subject", envelope.subject());
                appendField(sb, "Timestamp", envelope.timestamp());
                appendField(sb, "TopicArn", envelope.topicArn());
                appendField(sb, "Type", envelope.type());
            }
            case SUBSCRIPTION_CONFIRMATION, UNSUBSCRIBE_CONFIRMATION -> {
                appendField(sb, "Message", envelope.message());
                appendField(sb, "MessageId", envelope.messageId());
                appendField(sb, "SubscribeURL", envelope.subscribeURL());
                appendField(sb, "Timestamp", envelope.timestamp());
                appendField(sb, "Token", envelope.token());
                appendField(sb, "TopicArn", envelope.topicArn());
                appendField(sb, "Type", envelope.type());
            }
            case UNKNOWN -> {}
        }
        return sb.toString();
    }

    private static void appendField(StringBuilder builder, String name, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(name).append('\n').append(value).append('\n');
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SnsEnvelope(
            @JsonProperty("Type") String type,
            @JsonProperty("MessageId") String messageId,
            @JsonProperty("TopicArn") String topicArn,
            @JsonProperty("Message") String message,
            @JsonProperty("Timestamp") String timestamp,
            @JsonProperty("Signature") String signature,
            @JsonProperty("SignatureVersion") String signatureVersion,
            @JsonProperty("SigningCertURL") String signingCertUrl,
            @JsonProperty("SubscribeURL") String subscribeURL,
            @JsonProperty("Token") String token,
            @JsonProperty("Subject") String subject
    ) {
        MessageType messageType() {
            if (!StringUtils.hasText(type)) {
                return MessageType.UNKNOWN;
            }
            return switch (type) {
                case "Notification" -> MessageType.NOTIFICATION;
                case "SubscriptionConfirmation" -> MessageType.SUBSCRIPTION_CONFIRMATION;
                case "UnsubscribeConfirmation" -> MessageType.UNSUBSCRIBE_CONFIRMATION;
                default -> MessageType.UNKNOWN;
            };
        }
    }

    private enum MessageType {
        NOTIFICATION,
        SUBSCRIPTION_CONFIRMATION,
        UNSUBSCRIBE_CONFIRMATION,
        UNKNOWN
    }
}
