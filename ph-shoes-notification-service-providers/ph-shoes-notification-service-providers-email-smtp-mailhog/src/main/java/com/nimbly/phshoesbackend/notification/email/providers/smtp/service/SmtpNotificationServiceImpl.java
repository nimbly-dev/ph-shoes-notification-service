package com.nimbly.phshoesbackend.notification.email.providers.smtp.service;


import com.nimbly.phshoesbackend.notification.core.dto.Attachment;
import com.nimbly.phshoesbackend.notification.core.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.smtp.config.NotificationSmtpProps;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class SmtpNotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationSmtpProps props;

    public SmtpNotificationServiceImpl(JavaMailSender mailSender, NotificationSmtpProps props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public SendResult sendEmailVerification(EmailRequest req) throws NotificationSendException {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            if (req.getFrom() != null) {
                helper.setFrom(toInternet(req.getFrom()));
            } else if (props.getDefaultFrom() != null) {
                helper.setFrom(new jakarta.mail.internet.InternetAddress(props.getDefaultFrom()));
            }

            if (req.getTo() != null) for (EmailAddress ea : req.getTo()) helper.addTo(toInternet(ea));
            if (req.getCc() != null) for (EmailAddress ea : req.getCc()) helper.addCc(toInternet(ea));
            if (req.getBcc() != null) for (EmailAddress ea : req.getBcc()) helper.addBcc(toInternet(ea));

            helper.setSubject(req.getSubject());

            String html = req.getHtmlBody();
            String text = req.getTextBody();
            if (html != null && text != null) {
                helper.setText(text, html);
            } else if (html != null) {
                helper.setText(html, true);
            } else {
                helper.setText(text != null ? text : "");
            }

            if (req.getHeaders() != null) {
                for (var e : req.getHeaders().entrySet()) {
                    mime.addHeader(e.getKey(), e.getValue());
                }
            }
            if (req.getAttachments() != null) {
                for (Attachment a : req.getAttachments()) {
                    helper.addAttachment(a.getFilename(),
                            new org.springframework.core.io.ByteArrayResource(a.getContent()),
                            a.getMimeType());
                }
            }

            mailSender.send(mime);

            return SendResult.builder()
                    .messageId(null)
                    .provider("smtp")
                    .acceptedAt(java.time.Instant.now())
                    .requestId(req.getRequestIdHint())
                    .build();

        } catch (Exception e) {
            throw new NotificationSendException("SMTP send failed: " + e.getMessage(), e);
        }
    }



    private static InternetAddress toInternet(EmailAddress ea) throws Exception {
        return (ea.getName() == null || ea.getName().isBlank())
                ? new InternetAddress(ea.getAddress(), false)
                : new InternetAddress(ea.getAddress(), ea.getName());
    }


}