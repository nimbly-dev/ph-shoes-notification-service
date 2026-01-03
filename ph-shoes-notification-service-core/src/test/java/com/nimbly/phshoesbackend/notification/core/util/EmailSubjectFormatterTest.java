package com.nimbly.phshoesbackend.notification.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailSubjectFormatterTest {
    private final EmailSubjectFormatter formatter = new EmailSubjectFormatter();

    @Test
    void returnsSubjectWhenPrefixBlank() {
        // Arrange
        String subject = "Welcome";

        // Act
        String result = formatter.withPrefix(subject, " ");

        // Assert
        assertThat(result).isEqualTo("Welcome");
    }

    @Test
    void usesPrefixWhenSubjectBlank() {
        // Arrange
        String prefix = "[PH]";

        // Act
        String result = formatter.withPrefix(" ", prefix);

        // Assert
        assertThat(result).isEqualTo("[PH]");
    }

    @Test
    void combinesPrefixAndSubject() {
        // Arrange
        String subject = "Verify Email";
        String prefix = "[PH]";

        // Act
        String result = formatter.withPrefix(subject, prefix);

        // Assert
        assertThat(result).isEqualTo("[PH] Verify Email");
    }
}
