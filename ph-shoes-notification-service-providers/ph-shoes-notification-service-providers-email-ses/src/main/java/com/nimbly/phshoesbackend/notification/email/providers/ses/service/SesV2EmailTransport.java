package com.nimbly.phshoesbackend.notification.email.providers.ses.service;

import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesProps;
import com.nimbly.phshoesbackend.notification.email.providers.ses.util.TemplateJsonSerializer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SesV2EmailTransport implements EmailTransportService {

    private final SesV2Client sesClient;
    private final NotificationSesProps props;
    private final TemplateJsonSerializer json;

    public SesV2EmailTransport(SesV2Client sesClient, NotificationSesProps props, TemplateJsonSerializer json) {
        this.sesClient = sesClient;
        this.props = props;
        this.json = json;
    }

    @Override
    public SendResult send(ComposedEmail email) throws NotificationSendException {
        try {
            boolean hasAttachments = email.getRequest().getAttachments() != null && !email.getRequest().getAttachments().isEmpty();
            boolean useTemplate = email.getRequest().getTemplateId() != null && !hasAttachments;

            Destination destination = Destination.builder()
                    .toAddresses(email.getTo() == null ? List.of() : email.getTo())
                    .ccAddresses(email.getCc() == null ? List.of() : email.getCc())
                    .bccAddresses(email.getBcc() == null ? List.of() : email.getBcc())
                    .build();

            EmailContent content = useTemplate
                    ? EmailContent.builder()
                    .template(Template.builder()
                            .templateName(email.getRequest().getTemplateId())
                            .templateData(json.toJson(email.getRequest().getTemplateVars()))
                            .build())
                    .build()
                    : EmailContent.builder()
                    .raw(RawMessage.builder().data(SdkBytes.fromUtf8String(email.getRawMime())).build())
                    .build();

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .destination(destination)
                    .content(content)
                    .fromEmailAddress(email.getFrom());

            String cfg = props.getConfigurationSet();
            if (cfg != null && !cfg.isBlank()) {
                requestBuilder = requestBuilder.configurationSetName(cfg);
            }

            Map<String, String> tags = email.getTags();
            if (tags != null && !tags.isEmpty()) {
                requestBuilder = requestBuilder.emailTags(
                        tags.entrySet().stream()
                                .map(e -> MessageTag.builder().name(e.getKey()).value(e.getValue()).build())
                                .toList()
                );
            }

            SendEmailResponse resp = sesClient.sendEmail(requestBuilder.build());

            return SendResult.builder()
                    .messageId(resp.messageId())
                    .provider("ses")
                    .acceptedAt(Instant.now())
                    .build();

        } catch (SesV2Exception e) {
            throw new NotificationSendException("SES send failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new NotificationSendException("SES send failed: " + e.getMessage(), e);
        }
    }
}