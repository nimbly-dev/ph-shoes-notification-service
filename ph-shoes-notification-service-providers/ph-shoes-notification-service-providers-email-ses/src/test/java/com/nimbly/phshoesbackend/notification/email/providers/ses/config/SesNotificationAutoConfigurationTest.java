package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.util.TemplateJsonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class SesNotificationAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SesNotificationAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues(
                    "notification.provider=ses",
                    "notification.transport=ses"
            );

    @Test
    void createsSesBeansWhenEnabled() {
        // Arrange
        ApplicationContextRunner runner = contextRunner
                .withPropertyValues("notification.ses.endpoint=localhost");

        // Act + Assert
        runner.run(context -> {
            assertThat(context).hasSingleBean(TemplateJsonSerializer.class);
            assertThat(context).hasSingleBean(EmailTransportService.class);
            assertThat(context).hasSingleBean(NotificationService.class);
        });
    }

    @Test
    void normalizeEndpointAddsDefaultPortForLocalhost() throws Exception {
        // Arrange
        Method method = SesNotificationAutoConfiguration.class
                .getDeclaredMethod("normalizeEndpoint", String.class);
        method.setAccessible(true);

        // Act
        URI uri = (URI) method.invoke(null, "localhost");

        // Assert
        assertThat(uri).isNotNull();
        assertThat(uri.getHost()).isEqualTo("localhost");
        assertThat(uri.getPort()).isEqualTo(4566);
    }

    @Test
    void normalizeEndpointReturnsNullForBlankInput() throws Exception {
        // Arrange
        Method method = SesNotificationAutoConfiguration.class
                .getDeclaredMethod("normalizeEndpoint", String.class);
        method.setAccessible(true);

        // Act
        URI uri = (URI) method.invoke(null, " ");

        // Assert
        assertThat(uri).isNull();
    }
}
