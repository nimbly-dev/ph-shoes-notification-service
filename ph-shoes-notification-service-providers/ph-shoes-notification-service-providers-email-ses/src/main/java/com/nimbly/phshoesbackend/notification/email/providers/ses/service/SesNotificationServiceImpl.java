package com.nimbly.phshoesbackend.notification.email.providers.ses.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesEmailProps;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesProps;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
public class SesNotificationServiceImpl implements NotificationService {

    private final SesV2Client ses;
    private final NotificationSesEmailProps emailProps;
    private final NotificationSesProps infraProps;
    private final ObjectMapper mapper;

    public SesNotificationServiceImpl(SesV2Client ses,
                                      NotificationSesEmailProps emailProps,
                                      NotificationSesProps infraProps,
                                      ObjectMapper mapper) {
        this.ses = ses;
        this.emailProps = emailProps;
        this.infraProps = infraProps;
        this.mapper = mapper;
    }

    @Override
    public SendResult sendEmailVerification(EmailRequest req) throws NotificationSendException {
        try {
            Destination dest = Destination.builder()
                    .toAddresses(mapAddrs(req.getTo()))
                    .ccAddresses(mapAddrs(req.getCc()))
                    .bccAddresses(mapAddrs(req.getBcc()))
                    .build();

            String subject = req.getSubject();
            if (emailProps.getSubjectPrefix() != null && !emailProps.getSubjectPrefix().isBlank()) {
                subject = (subject == null || subject.isBlank())
                        ? emailProps.getSubjectPrefix()
                        : emailProps.getSubjectPrefix() + " " + subject;
            }

            boolean hasAttachments = hasAttachments(req);
            boolean hasCustomHeaders = hasCustomHeaders(req);

            EmailContent content;
            if (req.getTemplateId() != null && !hasAttachments && !hasCustomHeaders) {
                Template template = Template.builder()
                        .templateName(req.getTemplateId())
                        .templateData(req.getTemplateVars() == null ? "{}" : mapper.writeValueAsString(req.getTemplateVars()))
                        .build();
                content = EmailContent.builder().template(template).build();
            } else if (hasAttachments || hasCustomHeaders) {
                content = EmailContent.builder().raw(buildRawMessage(req, subject)).build();
            } else {
                Content subj = Content.builder().data(nullSafe(subject)).charset("UTF-8").build();
                Body body = Body.builder()
                        .text(req.getTextBody() != null ? Content.builder().data(req.getTextBody()).charset("UTF-8").build() : null)
                        .html(req.getHtmlBody() != null ? Content.builder().data(req.getHtmlBody()).charset("UTF-8").build() : null)
                        .build();
                Message msg = Message.builder().subject(subj).body(body).build();
                // NOTE: Simple() path does NOT support custom headers; raw path is forced when headers are present.
                content = EmailContent.builder().simple(msg).build();
            }

            String from = (req.getFrom() != null) ? address(req.getFrom()) : emailProps.getFrom();

            SendEmailRequest.Builder builder = SendEmailRequest.builder()
                    .destination(dest)
                    .content(content)
                    .fromEmailAddress(from);

            if (infraProps.getConfigurationSet() != null && !infraProps.getConfigurationSet().isBlank()) {
                builder = builder.configurationSetName(infraProps.getConfigurationSet());
            }

            if (req.getTags() != null && !req.getTags().isEmpty()) {
                builder = builder.emailTags(toTags(req.getTags()));
            }

            SendEmailResponse resp = ses.sendEmail(builder.build());

            return SendResult.builder()
                    .messageId(resp.messageId())
                    .provider("ses")
                    .acceptedAt(Instant.now())
                    .requestId(req.getRequestIdHint())
                    .build();

        } catch (SesV2Exception e) {
            throw new NotificationSendException("SES send failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new NotificationSendException("SES send failed: " + e.getMessage(), e);
        }
    }

    private static List<String> mapAddrs(List<EmailAddress> list) {
        return (list == null || list.isEmpty())
                ? List.of()
                : list.stream().map(SesNotificationServiceImpl::address).toList();
    }

    private static String address(EmailAddress ea) {
        if (ea == null) return null;
        return (ea.getName() == null || ea.getName().isBlank())
                ? ea.getAddress()
                : String.format("%s <%s>", ea.getName(), ea.getAddress());
    }

    private static List<MessageTag> toTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> MessageTag.builder().name(e.getKey()).value(e.getValue()).build())
                .toList();
    }

    private static boolean hasAttachments(EmailRequest req) {
        return req.getAttachments() != null && !req.getAttachments().isEmpty();
    }

    private static boolean hasCustomHeaders(EmailRequest req) {
        return req.getHeaders() != null && !req.getHeaders().isEmpty();
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }

    /** Minimal RAW builder for attachments + (optional) text/html + unsubscribe headers. */
    private RawMessage buildRawMessage(EmailRequest req, String subject) {
        String boundary = "mixed_" + System.currentTimeMillis();
        String altBoundary = "alt_" + System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        String from = (req.getFrom() != null) ? address(req.getFrom()) : emailProps.getFrom();

        sb.append("From: ").append(from).append("\r\n");
        if (req.getTo() != null && !req.getTo().isEmpty())
            sb.append("To: ").append(String.join(", ", mapAddrs(req.getTo()))).append("\r\n");
        if (req.getCc() != null && !req.getCc().isEmpty())
            sb.append("Cc: ").append(String.join(", ", mapAddrs(req.getCc()))).append("\r\n");
        sb.append("Subject: ").append(nullSafe(subject)).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");

        appendHeader(sb, req, "List-Unsubscribe", emailProps.getListUnsubscribe());
        appendHeader(sb, req, "List-Unsubscribe-Post", emailProps.getListUnsubscribePost());

        sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n\r\n");

        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Type: multipart/alternative; boundary=\"").append(altBoundary).append("\"\r\n\r\n");

        if (req.getTextBody() != null) {
            sb.append("--").append(altBoundary).append("\r\n");
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
            sb.append(req.getTextBody()).append("\r\n\r\n");
        }
        if (req.getHtmlBody() != null) {
            sb.append("--").append(altBoundary).append("\r\n");
            sb.append("Content-Type: text/html; charset=UTF-8\r\n\r\n");
            sb.append(req.getHtmlBody()).append("\r\n\r\n");
        }
        sb.append("--").append(altBoundary).append("--\r\n");

        req.getAttachments().forEach(att -> {
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: ").append(att.getMimeType()).append("\r\n");
            sb.append("Content-Disposition: attachment; filename=\"").append(att.getFilename()).append("\"\r\n");
            sb.append("Content-Transfer-Encoding: base64\r\n\r\n");
            sb.append(Base64.getEncoder().encodeToString(att.getContent())).append("\r\n\r\n");
        });

        sb.append("--").append(boundary).append("--\r\n");

        return RawMessage.builder()
                .data(software.amazon.awssdk.core.SdkBytes.fromUtf8String(sb.toString()))
                .build();
    }

    private static void appendHeader(StringBuilder sb, EmailRequest req, String headerName, String fallback) {
        String value = resolveHeader(req.getHeaders(), headerName, fallback);
        if (value != null && !value.isBlank()) {
            sb.append(headerName).append(": ").append(value).append("\r\n");
        }
    }

    private static String resolveHeader(Map<String, String> headers, String headerName, String fallback) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(headerName)
                        && entry.getValue() != null
                        && !entry.getValue().isBlank()) {
                    return entry.getValue();
                }
            }
        }
        return fallback;
    }
}
