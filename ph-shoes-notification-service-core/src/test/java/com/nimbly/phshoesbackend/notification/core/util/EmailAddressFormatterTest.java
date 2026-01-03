package com.nimbly.phshoesbackend.notification.core.util;

import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAddressFormatterTest {
    private final EmailAddressFormatter formatter = new EmailAddressFormatter();

    @Test
    void formatsAddressWithName() {
        // Arrange
        EmailAddress address = EmailAddress.builder()
                .name("PH Shoes")
                .address("no-reply@ph-shoes.app")
                .build();

        // Act
        String formatted = formatter.format(address);

        // Assert
        assertThat(formatted).isEqualTo("PH Shoes <no-reply@ph-shoes.app>");
    }

    @Test
    void formatsAddressWithoutName() {
        // Arrange
        EmailAddress address = EmailAddress.builder()
                .address("support@ph-shoes.app")
                .build();

        // Act
        String formatted = formatter.format(address);

        // Assert
        assertThat(formatted).isEqualTo("support@ph-shoes.app");
    }

    @Test
    void formatsAllAddressesWithEmptyInput() {
        // Arrange
        List<EmailAddress> addresses = List.of();

        // Act
        List<String> formatted = formatter.formatAll(addresses);

        // Assert
        assertThat(formatted).isEmpty();
    }
}
