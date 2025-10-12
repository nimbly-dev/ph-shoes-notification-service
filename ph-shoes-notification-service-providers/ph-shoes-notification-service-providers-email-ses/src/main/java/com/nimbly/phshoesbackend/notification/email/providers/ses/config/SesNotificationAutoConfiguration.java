package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.service.SesNotificationServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@AutoConfiguration
@ConditionalOnProperty(name = "notification.provider", havingValue = "ses")
@ConditionalOnMissingBean(NotificationService.class)
public class SesNotificationAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "notification.ses")
    public NotificationSesProps notificationSesProps() {
        return new NotificationSesProps();
    }

    @Bean
    public SesV2Client sesV2Client(NotificationSesProps props) {
        return SesV2Client.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public NotificationService notificationService(SesV2Client ses, NotificationSesProps props, ObjectMapper mapper) {
        return new SesNotificationServiceImpl(ses, props, mapper);
    }
}
