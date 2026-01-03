package com.nimbly.phshoesbackend.notification.email.providers.ses.service;

import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.email.providers.ses.config.NotificationSesProps;
import com.nimbly.phshoesbackend.notification.email.providers.ses.util.TemplateJsonSerializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SesV2EmailTransportTest {
    @Test
    void sendsTemplateEmailWhenTemplateIdPresent() {
        // Arrange
        SesV2Client sesClient = mock(SesV2Client.class);
        NotificationSesProps props = new NotificationSesProps();
        props.setConfigurationSet("cfg");
        TemplateJsonSerializer serializer = new TemplateJsonSerializer();
        SesV2EmailTransport transport = new SesV2EmailTransport(sesClient, props, serializer);
        EmailRequest request = EmailRequest.builder()
                .templateId("template-id")
                .templateVar("name", "PH")
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .build();
        ComposedEmail email = new ComposedEmail(
                request,
                "raw",
                "no-reply@ph-shoes.app",
                List.of("user@ph-shoes.app"),
                List.of(),
                List.of(),
                "Verify",
                Map.of("source", "test")
        );
        SendEmailResponse response = SendEmailResponse.builder().messageId("id-123").build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // Act
        SendResult result = transport.send(email);

        // Assert
        assertThat(result.getMessageId()).isEqualTo("id-123");
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest sent = captor.getValue();
        assertThat(sent.destination().toAddresses()).containsExactly("user@ph-shoes.app");
        assertThat(sent.content().template().templateName()).isEqualTo("template-id");
        assertThat(sent.content().template().templateData()).isEqualTo(serializer.toJson(request.getTemplateVars()));
        assertThat(sent.configurationSetName()).isEqualTo("cfg");
    }

    @Test
    void sendsRawEmailWhenTemplateIdMissing() {
        // Arrange
        SesV2Client sesClient = mock(SesV2Client.class);
        NotificationSesProps props = new NotificationSesProps();
        TemplateJsonSerializer serializer = new TemplateJsonSerializer();
        SesV2EmailTransport transport = new SesV2EmailTransport(sesClient, props, serializer);
        EmailRequest request = EmailRequest.builder()
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .build();
        ComposedEmail email = new ComposedEmail(
                request,
                "RAW-MIME",
                "no-reply@ph-shoes.app",
                List.of("user@ph-shoes.app"),
                List.of(),
                List.of(),
                "Verify",
                Map.of()
        );
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("raw-1").build());

        // Act
        SendResult result = transport.send(email);

        // Assert
        assertThat(result.getMessageId()).isEqualTo("raw-1");
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(captor.capture());
        SendEmailRequest sent = captor.getValue();
        assertThat(sent.content().raw().data().asUtf8String()).isEqualTo("RAW-MIME");
    }
}
