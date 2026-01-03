package com.nimbly.phshoesbackend.notification.core.util;

import com.nimbly.phshoesbackend.notification.core.model.dto.Attachment;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.props.NotificationEmailProps;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RawMimeBuilderTest {
    @Test
    void buildsAlternativeMimeWhenNoAttachments() {
        // Arrange
        NotificationEmailProps props = new NotificationEmailProps();
        props.setFrom("no-reply@ph-shoes.app");
        props.setListUnsubscribe("<mailto:unsubscribe@ph-shoes.app>");
        props.setListUnsubscribePost("List-Unsubscribe=One-Click");
        RawMimeBuilder builder = new RawMimeBuilder(props, new EmailAddressFormatter());
        EmailRequest request = EmailRequest.builder()
                .from(EmailAddress.builder().address("from@ph-shoes.app").build())
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .textBody("hello")
                .build();

        // Act
        String mime = builder.build(request, "Verify");

        // Assert
        assertThat(mime).contains("From: from@ph-shoes.app");
        assertThat(mime).contains("List-Unsubscribe: <mailto:unsubscribe@ph-shoes.app>");
        assertThat(mime).contains("Content-Type: multipart/alternative");
        assertThat(mime).doesNotContain("Content-Type: multipart/mixed");
    }

    @Test
    void buildsMixedMimeWhenAttachmentsPresent() {
        // Arrange
        NotificationEmailProps props = new NotificationEmailProps();
        props.setFrom("no-reply@ph-shoes.app");
        RawMimeBuilder builder = new RawMimeBuilder(props, new EmailAddressFormatter());
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

        // Act
        String mime = builder.build(request, "Verify");

        // Assert
        assertThat(mime).contains("From: no-reply@ph-shoes.app");
        assertThat(mime).contains("Content-Type: multipart/mixed");
        assertThat(mime).contains("aGVsbG8=");
    }
}
