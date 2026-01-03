package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import com.nimbly.phshoesbackend.notification.core.model.props.NotificationTransportProps;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.email.providers.smtp.service.SmtpEmailTransportServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;
@AutoConfiguration
@EnableConfigurationProperties({ NotificationSmtpProps.class, NotificationTransportProps.class })
@ConditionalOnProperty(name = "notification.transport", havingValue = "smtp")
public class SmtpTransportAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(NotificationSmtpProps props) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.getHost());
        sender.setPort(props.getPort() == null ? 25 : props.getPort());
        sender.setUsername(props.getUsername());
        sender.setPassword(props.getPassword());

        Properties p = sender.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(props.getAuth())));
        p.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(props.getStarttls())));
        if (props.getConnectionTimeoutMillis() != null) p.put("mail.smtp.connectiontimeout", props.getConnectionTimeoutMillis());
        if (props.getReadTimeoutMillis() != null)       p.put("mail.smtp.timeout", props.getReadTimeoutMillis());
        if (props.getWriteTimeoutMillis() != null)      p.put("mail.smtp.writetimeout", props.getWriteTimeoutMillis());
        return sender;
    }

    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    @ConditionalOnMissingBean(EmailTransportService.class)
    public EmailTransportService smtpEmailTransport(JavaMailSender sender, NotificationTransportProps ignored) {
        return new SmtpEmailTransportServiceImpl(sender);
    }
}
