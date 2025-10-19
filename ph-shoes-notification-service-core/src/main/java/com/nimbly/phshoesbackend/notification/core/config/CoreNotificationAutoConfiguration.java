package com.nimbly.phshoesbackend.notification.core.config;

import com.nimbly.phshoesbackend.notification.core.model.props.NotificationEmailProps;
import com.nimbly.phshoesbackend.notification.core.model.props.NotificationTransportProps;
import com.nimbly.phshoesbackend.notification.core.service.EmailCompositionService;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.core.service.impl.CoreNotificationServiceImpl;
import com.nimbly.phshoesbackend.notification.core.service.impl.DefaultEmailCompositionServiceImpl;
import com.nimbly.phshoesbackend.notification.core.util.EmailAddressFormatter;
import com.nimbly.phshoesbackend.notification.core.util.EmailSubjectFormatter;
import com.nimbly.phshoesbackend.notification.core.util.RawMimeBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties({ NotificationEmailProps.class, NotificationTransportProps.class })
public class CoreNotificationAutoConfiguration {

    // --- utilities ---

    @Bean
    @ConditionalOnMissingBean
    public EmailAddressFormatter emailAddressFormatter() {
        return new EmailAddressFormatter();
    }

    @Bean
    @ConditionalOnMissingBean
    public EmailSubjectFormatter subjectFormatter() {
        return new EmailSubjectFormatter();
    }

    @Bean
    @ConditionalOnMissingBean
    public RawMimeBuilder rawMimeBuilder(NotificationEmailProps emailProps, EmailAddressFormatter formatter) {
        return new RawMimeBuilder(emailProps, formatter);
    }

    // --- composition ---
    @Bean
    @ConditionalOnMissingBean
    public EmailCompositionService emailCompositionService(NotificationEmailProps emailProps,
                                                           EmailSubjectFormatter subjectFormatter,
                                                           EmailAddressFormatter addressFormatter,
                                                           RawMimeBuilder rawMimeBuilder) {
        return new DefaultEmailCompositionServiceImpl(emailProps, subjectFormatter, addressFormatter, rawMimeBuilder);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    @ConditionalOnBean(EmailTransportService.class)
    public NotificationService notificationService(EmailCompositionService compositionService,
                                                   EmailTransportService transportService) {
        return new CoreNotificationServiceImpl(compositionService, transportService);
    }
}