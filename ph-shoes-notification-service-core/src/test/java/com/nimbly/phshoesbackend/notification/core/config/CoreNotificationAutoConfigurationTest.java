package com.nimbly.phshoesbackend.notification.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.commons.core.repository.SuppressionRepository;
import com.nimbly.phshoesbackend.commons.core.security.EmailCrypto;
import com.nimbly.phshoesbackend.notification.core.ses.SesWebhookController;
import com.nimbly.phshoesbackend.notification.core.ses.SesWebhookProcessor;
import com.nimbly.phshoesbackend.notification.core.service.EmailCompositionService;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.core.util.EmailAddressFormatter;
import com.nimbly.phshoesbackend.notification.core.util.EmailSubjectFormatter;
import com.nimbly.phshoesbackend.notification.core.util.RawMimeBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CoreNotificationAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoreNotificationAutoConfiguration.class))
            .withBean(SuppressionRepository.class, () -> mock(SuppressionRepository.class))
            .withBean(EmailCrypto.class, () -> mock(EmailCrypto.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void createsCoreBeansWhenDependenciesPresent() {
        // Arrange
        ApplicationContextRunner runner = contextRunner
                .withBean(EmailTransportService.class, () -> mock(EmailTransportService.class));

        // Act + Assert
        runner.run(context -> {
            assertThat(context).hasSingleBean(EmailAddressFormatter.class);
            assertThat(context).hasSingleBean(EmailSubjectFormatter.class);
            assertThat(context).hasSingleBean(RawMimeBuilder.class);
            assertThat(context).hasSingleBean(EmailCompositionService.class);
            assertThat(context).hasSingleBean(NotificationService.class);
            assertThat(context).hasSingleBean(SesWebhookProcessor.class);
        });
    }

    @Test
    void skipsNotificationServiceWhenTransportMissing() {
        // Arrange
        ApplicationContextRunner runner = contextRunner;

        // Act + Assert
        runner.run(context -> assertThat(context).doesNotHaveBean(NotificationService.class));
    }

    @Test
    void createsWebhookControllerWhenWebEnabled() {
        // Arrange
        WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CoreNotificationAutoConfiguration.class))
                .withBean(SuppressionRepository.class, () -> mock(SuppressionRepository.class))
                .withBean(EmailCrypto.class, () -> mock(EmailCrypto.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(EmailTransportService.class, () -> mock(EmailTransportService.class))
                .withPropertyValues("notification.ses.webhook.enabled=true");

        // Act + Assert
        webRunner.run(context -> assertThat(context).hasSingleBean(SesWebhookController.class));
    }
}
