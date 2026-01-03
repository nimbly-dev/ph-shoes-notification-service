package com.nimbly.phshoesbackend.notification.email.providers.ses.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.model.dto.Attachment;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesEmailProps;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesProps;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SesNotificationServiceImplTest {
    @Test
    void sendsTemplateEmailWhenTemplateIdPresent() throws Exception {
        // Arrange
        SesV2Client sesClient = mock(SesV2Client.class);
        NotificationSesEmailProps emailProps = new NotificationSesEmailProps();
        NotificationSesProps infraProps = new NotificationSesProps();
        ObjectMapper mapper = new ObjectMapper();
        SesNotificationServiceImpl service = new SesNotificationServiceImpl(sesClient, emailProps, infraProps, mapper);
        EmailRequest request = EmailRequest.builder()
                .templateId("template-id")
                .templateVar("name", "PH")
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .tag("source", "test")
                .requestIdHint("req-1")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("id-123").build());

        // Act
        SendResult result = service.sendEmailVerification(request);

        // Assert
        assertThat(result.getMessageId()).isEqualTo("id-123");
        assertThat(result.getProvider()).isEqualTo("ses");
        assertThat(result.getRequestId()).isEqualTo("req-1");
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest sent = captor.getValue();
        assertThat(sent.content().template().templateName()).isEqualTo("template-id");
        assertThat(sent.content().template().templateData())
                .isEqualTo(mapper.writeValueAsString(request.getTemplateVars()));
        assertThat(sent.emailTags()).hasSize(1);
        assertThat(sent.emailTags().get(0).name()).isEqualTo("source");
        assertThat(sent.emailTags().get(0).value()).isEqualTo("test");
    }

    @Test
    void sendsRawEmailWhenAttachmentsPresent() {
        // Arrange
        SesV2Client sesClient = mock(SesV2Client.class);
        NotificationSesEmailProps emailProps = new NotificationSesEmailProps();
        emailProps.setFrom("no-reply@ph-shoes.app");
        emailProps.setSubjectPrefix("[PH]");
        emailProps.setListUnsubscribe("<mailto:unsubscribe@ph-shoes.app>");
        emailProps.setListUnsubscribePost("List-Unsubscribe=One-Click");
        NotificationSesProps infraProps = new NotificationSesProps();
        ObjectMapper mapper = new ObjectMapper();
        SesNotificationServiceImpl service = new SesNotificationServiceImpl(sesClient, emailProps, infraProps, mapper);
        Attachment attachment = Attachment.builder()
                .filename("file.txt")
                .mimeType("text/plain")
                .content("hello".getBytes(StandardCharsets.UTF_8))
                .build();
        EmailRequest request = EmailRequest.builder()
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .textBody("hello")
                .attachment(attachment)
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("raw-1").build());

        // Act
        SendResult result = service.sendEmailVerification(request);

        // Assert
        assertThat(result.getMessageId()).isEqualTo("raw-1");
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest sent = captor.getValue();
        String raw = sent.content().raw().data().asUtf8String();
        assertThat(raw).contains("Subject: [PH] Verify");
        assertThat(raw).contains("List-Unsubscribe: <mailto:unsubscribe@ph-shoes.app>");
        assertThat(raw).contains("aGVsbG8=");
    }
}
