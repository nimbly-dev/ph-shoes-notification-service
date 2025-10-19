package com.nimbly.phshoesbackend.notification.email.providers.smtp.service;

import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class SmtpEmailTransportServiceImpl implements EmailTransportService {

    private final JavaMailSender mailSender;

    public SmtpEmailTransportServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public SendResult send(ComposedEmail email) throws NotificationSendException {
        try {
            Session session = (mailSender instanceof JavaMailSenderImpl impl && impl.getSession() != null)
                    ? impl.getSession()
                    : Session.getInstance(System.getProperties());

            MimeMessage mime = new MimeMessage(
                    session,
                    new ByteArrayInputStream(email.getRawMime().getBytes(StandardCharsets.UTF_8))
            );

            mailSender.send(mime);

            return SendResult.builder()
                    .messageId("smtp-" + System.currentTimeMillis())
                    .provider("smtp")
                    .acceptedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            throw new NotificationSendException("SMTP send failed: " + e.getMessage(), e);
        }
    }
}