package com.nimbly.phshoesbackend.notification.email.providers.ses.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SesNotificationServiceImpl implements NotificationService {

    private final SesV2Client ses;
    private final NotificationSesProps props;
    private final ObjectMapper mapper;

    public SesNotificationServiceImpl(SesV2Client ses, NotificationSesProps props, ObjectMapper mapper) {
        this.ses = ses;
        this.props = props;
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

            EmailContent content;
            if (req.getTemplateId() != null) {
                Template template = Template.builder()
                        .templateName(req.getTemplateId())
                        .templateData(req.getTemplateVars() == null ? "{}" : mapper.writeValueAsString(req.getTemplateVars()))
                        .build();
                content = EmailContent.builder().template(template).build();
            } else {
                Content subj = Content.builder().data(req.getSubject()).build();
                Body body = Body.builder()
                        .text(req.getTextBody() != null ? Content.builder().data(req.getTextBody()).build() : null)
                        .html(req.getHtmlBody() != null ? Content.builder().data(req.getHtmlBody()).build() : null)
                        .build();
                Message msg = Message.builder().subject(subj).body(body).build();
                content = EmailContent.builder().simple(msg).build();
            }

            String from = req.getFrom() != null ? address(req.getFrom()) : props.getDefaultFrom();

            SendEmailRequest.Builder builder = SendEmailRequest.builder()
                    .destination(dest)
                    .content(content)
                    .fromEmailAddress(from);

            if (props.getConfigurationSetName() != null) {
                builder = builder.configurationSetName(props.getConfigurationSetName());
            }

            if (req.getTags() != null && !req.getTags().isEmpty()) {
                builder = builder.emailTags(req.getTags().entrySet().stream()
                        .map(e -> MessageTag.builder().name(e.getKey()).value(e.getValue()).build())
                        .toList());
            }

            SendEmailResponse resp = ses.sendEmail(builder.build());

            return SendResult.builder()
                    .messageId(resp.messageId())
                    .provider("ses")
                    .acceptedAt(java.time.Instant.now())
                    .requestId(req.getRequestIdHint())
                    .build();
        } catch (software.amazon.awssdk.services.sesv2.model.SesV2Exception e) {
            throw new NotificationSendException("SES send failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            throw new NotificationSendException("SES send failed: " + e.getMessage(), e);
        }
    }


    private static List<String> mapAddrs(List<EmailAddress> list) {
        return (list == null) ? List.of() : list.stream().map(SesNotificationServiceImpl::address).toList();
    }

    private static String address(EmailAddress ea) {
        if (ea == null) return null;
        return (ea.getName() == null || ea.getName().isBlank())
                ? ea.getAddress()
                : String.format("%s <%s>", ea.getName(), ea.getAddress());
    }

}
