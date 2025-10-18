package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.service.SesNotificationServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

import java.net.URI;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "notification.provider", havingValue = "ses")
@ConditionalOnMissingBean(NotificationService.class)
@EnableConfigurationProperties({ NotificationSesProps.class, NotificationSesEmailProps.class })
public class SesNotificationAutoConfiguration {

    @Bean
    public SesV2Client sesV2Client(NotificationSesProps infra) {
        SdkHttpClient http = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .build();

        ClientOverrideConfiguration override = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(3))
                .apiCallTimeout(Duration.ofSeconds(10))
                .build();

        String regionStr = (infra.getRegion() != null && !infra.getRegion().isBlank())
                ? infra.getRegion()
                : System.getProperty("AWS_REGION",
                System.getenv().getOrDefault("AWS_REGION", "ap-southeast-1"));

        SesV2ClientBuilder builder = SesV2Client.builder()
                .httpClient(http)
                .overrideConfiguration(override)
                .region(Region.of(regionStr))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (infra.getEndpoint() != null && !infra.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(infra.getEndpoint()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test"))); // LocalStack-friendly
        }

        return builder.build();
    }

    @Bean
    public NotificationService notificationService(
            SesV2Client ses,
            NotificationSesEmailProps emailProps,
            NotificationSesProps infraProps,
            ObjectMapper mapper
    ) {
        return new SesNotificationServiceImpl(ses, emailProps, infraProps, mapper);
    }
}