package com.nimbly.phshoesbackend.notification.email.providers.smtp.service;

import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpEmailTransportServiceImplTest {
    @Test
    void sendsEmailUsingJavaMailSender() throws Exception {
        // Arrange
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage(any(InputStream.class))).thenReturn(mimeMessage);
        SmtpEmailTransportServiceImpl service = new SmtpEmailTransportServiceImpl(mailSender);
        EmailRequest request = EmailRequest.builder().subject("Verify").build();
        ComposedEmail email = new ComposedEmail(
                request,
                "RAW",
                "no-reply@ph-shoes.app",
                List.of("user@ph-shoes.app"),
                List.of(),
                List.of(),
                "Verify",
                null
        );

        // Act
        SendResult result = service.send(email);

        // Assert
        assertThat(result.getProvider()).isEqualTo("smtp");
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void wrapsMailSenderFailures() {
        // Arrange
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage(any(InputStream.class)))
                .thenThrow(new RuntimeException("fail"));
        SmtpEmailTransportServiceImpl service = new SmtpEmailTransportServiceImpl(mailSender);
        EmailRequest request = EmailRequest.builder().subject("Verify").build();
        ComposedEmail email = new ComposedEmail(
                request,
                "RAW",
                "no-reply@ph-shoes.app",
                List.of("user@ph-shoes.app"),
                List.of(),
                List.of(),
                "Verify",
                null
        );

        // Act + Assert
        assertThatThrownBy(() -> service.send(email))
                .isInstanceOf(NotificationSendException.class)
                .hasMessageContaining("SMTP send failed");
    }
}
