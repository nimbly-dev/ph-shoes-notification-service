package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.service.SesNotificationServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.SesV2ClientBuilder;

import java.io.File;
import java.net.URI;
import java.time.Duration;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "notification.provider", havingValue = "ses")
@EnableConfigurationProperties({ NotificationSesProps.class, NotificationSesEmailProps.class })
public class SesNotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SesV2Client.class)
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

        URI endpoint = normalizeEndpoint(infra.getEndpoint());
        if (endpoint != null) {
            builder = builder
                    .endpointOverride(endpoint)
                    // LocalStack-friendly static creds
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }

        SesV2Client client = builder.build();
        log.info("[SES] region={} endpoint={}", regionStr, endpoint == null ? "(aws)" : endpoint);
        return client;
    }

    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    public NotificationService notificationService(
            SesV2Client ses,
            NotificationSesEmailProps emailProps,
            NotificationSesProps infraProps,
            ObjectMapper mapper
    ) {
        return new SesNotificationServiceImpl(ses, emailProps, infraProps, mapper);
    }

    /** Ensure scheme/port and map 'localstack' -> 'localhost' when running outside containers. */
    private static URI normalizeEndpoint(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String url = raw.matches("^[a-zA-Z]+://.*") ? raw : "http://" + raw;

        boolean inContainer = new File("/.dockerenv").exists();
        if (url.startsWith("http://localstack") && !inContainer) {
            url = url.replaceFirst("http://localstack", "http://localhost");
        }

        // Add default LocalStack edge port if none specified
        if (url.matches("^http://(localhost|localstack)(/.*)?$")) {
            url = url.replaceFirst("^(http://[^/:]+)", "$1:4566");
        }

        return URI.create(url);
    }
}
