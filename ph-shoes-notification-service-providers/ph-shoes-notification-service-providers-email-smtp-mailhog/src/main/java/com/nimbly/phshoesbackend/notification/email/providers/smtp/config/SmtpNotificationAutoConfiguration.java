package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.smtp.service.SmtpNotificationServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@AutoConfiguration
@ConditionalOnProperty(name = "notification.provider", havingValue = "smtp")
@ConditionalOnMissingBean(NotificationService.class)
public class SmtpNotificationAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "notification.smtp")
    public NotificationSmtpProps notificationSmtpProps() {
        return new NotificationSmtpProps();
    }

    @Bean
    public NotificationService notificationService(JavaMailSender mailSender, NotificationSmtpProps props) {
        return new SmtpNotificationServiceImpl(mailSender, props);
    }
}
