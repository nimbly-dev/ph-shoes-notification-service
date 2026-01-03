package com.nimbly.phshoesbackend.notification.core.service.impl;

import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.props.NotificationEmailProps;
import com.nimbly.phshoesbackend.notification.core.util.EmailAddressFormatter;
import com.nimbly.phshoesbackend.notification.core.util.EmailSubjectFormatter;
import com.nimbly.phshoesbackend.notification.core.util.RawMimeBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultEmailCompositionServiceImplTest {
    @Test
    void composesEmailWithPrefixAndFormattedRecipients() {
        // Arrange
        NotificationEmailProps props = new NotificationEmailProps();
        props.setFrom("no-reply@ph-shoes.app");
        props.setSubjectPrefix("[PH]");
        EmailAddressFormatter addressFormatter = new EmailAddressFormatter();
        EmailSubjectFormatter subjectFormatter = new EmailSubjectFormatter();
        RawMimeBuilder rawMimeBuilder = new RawMimeBuilder(props, addressFormatter);
        DefaultEmailCompositionServiceImpl composer = new DefaultEmailCompositionServiceImpl(
                props,
                subjectFormatter,
                addressFormatter,
                rawMimeBuilder
        );
        EmailRequest request = EmailRequest.builder()
                .from(EmailAddress.builder().address("support@ph-shoes.app").build())
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .textBody("hello")
                .build();

        // Act
        ComposedEmail composed = composer.compose(request);

        // Assert
        assertThat(composed.getSubject()).isEqualTo("[PH] Verify");
        assertThat(composed.getFrom()).isEqualTo("support@ph-shoes.app");
        assertThat(composed.getTo()).containsExactly("user@ph-shoes.app");
        assertThat(composed.getRawMime()).contains("Subject: [PH] Verify");
    }
}
