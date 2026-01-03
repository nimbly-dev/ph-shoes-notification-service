package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpTransportAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SmtpTransportAutoConfiguration.class))
            .withPropertyValues("notification.transport=smtp");

    @Test
    void createsJavaMailSenderFromProps() {
        // Arrange
        ApplicationContextRunner runner = contextRunner
                .withPropertyValues(
                        "notification.smtp.host=mailhog",
                        "notification.smtp.port=2525",
                        "notification.smtp.username=user",
                        "notification.smtp.password=pass",
                        "notification.smtp.auth=true",
                        "notification.smtp.starttls=true",
                        "notification.smtp.connection-timeout-millis=1000",
                        "notification.smtp.read-timeout-millis=1500",
                        "notification.smtp.write-timeout-millis=2000"
                );

        // Act + Assert
        runner.run(context -> {
            assertThat(context).hasSingleBean(JavaMailSender.class);
            JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
            assertThat(sender.getHost()).isEqualTo("mailhog");
            assertThat(sender.getPort()).isEqualTo(2525);
            assertThat(sender.getUsername()).isEqualTo("user");
            assertThat(sender.getPassword()).isEqualTo("pass");
            assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.auth"))).isEqualTo("true");
            assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.starttls.enable"))).isEqualTo("true");
            assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.connectiontimeout"))).isEqualTo("1000");
            assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.timeout"))).isEqualTo("1500");
            assertThat(String.valueOf(sender.getJavaMailProperties().get("mail.smtp.writetimeout"))).isEqualTo("2000");
        });
    }

    @Test
    void reusesExistingJavaMailSender() {
        // Arrange
        ApplicationContextRunner runner = contextRunner
                .withBean(JavaMailSender.class, JavaMailSenderImpl::new);

        // Act + Assert
        runner.run(context -> assertThat(context).hasSingleBean(JavaMailSender.class));
    }

    @Test
    void createsEmailTransportServiceWhenJavaMailSenderPresent() {
        // Arrange
        ApplicationContextRunner runner = contextRunner
                .withBean(JavaMailSender.class, JavaMailSenderImpl::new);

        // Act + Assert
        runner.run(context -> assertThat(context).hasSingleBean(EmailTransportService.class));
    }
}
