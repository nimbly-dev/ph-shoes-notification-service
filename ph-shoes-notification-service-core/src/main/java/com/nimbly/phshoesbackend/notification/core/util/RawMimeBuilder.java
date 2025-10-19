package com.nimbly.phshoesbackend.notification.core.util;

import com.nimbly.phshoesbackend.notification.core.model.dto.Attachment;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.props.NotificationEmailProps;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class RawMimeBuilder{

    private final NotificationEmailProps emailProps;
    private final EmailAddressFormatter addressFormatter;

    public RawMimeBuilder(NotificationEmailProps emailProps, EmailAddressFormatter addressFormatter) {
        this.emailProps = emailProps;
        this.addressFormatter = addressFormatter;
    }

    public String build(EmailRequest request, String finalSubject) {
        String boundary = "mixed_" + System.currentTimeMillis();
        String altBoundary = "alt_" + System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        String from = (request.getFrom() != null) ? addressFormatter.format(request.getFrom()) : emailProps.getFrom();

        sb.append("From: ").append(from).append("\r\n");
        if (request.getTo() != null && !request.getTo().isEmpty())
            sb.append("To: ").append(String.join(", ", addressFormatter.formatAll(request.getTo()))).append("\r\n");
        if (request.getCc() != null && !request.getCc().isEmpty())
            sb.append("Cc: ").append(String.join(", ", addressFormatter.formatAll(request.getCc()))).append("\r\n");
        sb.append("Subject: ").append(finalSubject == null ? "" : finalSubject).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");

        if (emailProps.getListUnsubscribe() != null && !emailProps.getListUnsubscribe().isBlank()) {
            sb.append("List-Unsubscribe: ").append(emailProps.getListUnsubscribe()).append("\r\n");
        }
        if (emailProps.getListUnsubscribePost() != null && !emailProps.getListUnsubscribePost().isBlank()) {
            sb.append("List-Unsubscribe-Post: ").append(emailProps.getListUnsubscribePost()).append("\r\n");
        }

        boolean withAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();
        if (!withAttachments) {
            sb.append("Content-Type: multipart/alternative; boundary=\"").append(altBoundary).append("\"\r\n\r\n");
        } else {
            sb.append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n\r\n");
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Type: multipart/alternative; boundary=\"").append(altBoundary).append("\"\r\n\r\n");
        }

        if (request.getTextBody() != null) {
            sb.append("--").append(altBoundary).append("\r\n");
            sb.append("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
            sb.append(request.getTextBody()).append("\r\n\r\n");
        }
        if (request.getHtmlBody() != null) {
            sb.append("--").append(altBoundary).append("\r\n");
            sb.append("Content-Type: text/html; charset=UTF-8\r\n\r\n");
            sb.append(request.getHtmlBody()).append("\r\n\r\n");
        }
        sb.append("--").append(altBoundary).append("--\r\n");

        if (withAttachments) {
            for (Attachment att : request.getAttachments()) {
                sb.append("--").append(boundary).append("\r\n");
                sb.append("Content-Type: ").append(att.getMimeType()).append("\r\n");
                sb.append("Content-Disposition: attachment; filename=\"").append(att.getFilename()).append("\"\r\n");
                sb.append("Content-Transfer-Encoding: base64\r\n\r\n");
                sb.append(Base64.getEncoder().encodeToString(att.getContent())).append("\r\n\r\n");
            }
            sb.append("--").append(boundary).append("--\r\n");
        }

        return sb.toString();
    }
}