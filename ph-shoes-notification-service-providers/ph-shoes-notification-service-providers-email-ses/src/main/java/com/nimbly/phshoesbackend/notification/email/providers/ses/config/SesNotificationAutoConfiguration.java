package com.nimbly.phshoesbackend.notification.email.providers.ses.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import com.nimbly.phshoesbackend.notification.email.providers.ses.service.SesNotificationServiceImpl;
import com.nimbly.phshoesbackend.notification.email.providers.ses.service.SesV2EmailTransport;
import com.nimbly.phshoesbackend.notification.email.providers.ses.util.TemplateJsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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

import java.net.URI;
import java.time.Duration;

@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "notification.provider", havingValue = "ses")
@EnableConfigurationProperties({ NotificationSesProps.class, NotificationSesEmailProps.class })
public class SesNotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SesV2Client.class)
    public SesV2Client sesV2Client(NotificationSesProps infraProperties) {
        SdkHttpClient httpClient = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .build();

        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(Duration.ofSeconds(3))
                .apiCallTimeout(Duration.ofSeconds(10))
                .build();

        String regionName = (infraProperties.getRegion() != null && !infraProperties.getRegion().isBlank())
                ? infraProperties.getRegion()
                : System.getProperty("AWS_REGION",
                System.getenv().getOrDefault("AWS_REGION", "ap-southeast-1"));

        SesV2ClientBuilder clientBuilder = SesV2Client.builder()
                .httpClient(httpClient)
                .overrideConfiguration(overrideConfiguration)
                .region(Region.of(regionName))
                .credentialsProvider(DefaultCredentialsProvider.create());

        URI endpointUri = normalizeEndpoint(infraProperties.getEndpoint());
        if (endpointUri != null) {
            clientBuilder = clientBuilder
                    .endpointOverride(endpointUri)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
        }

        SesV2Client sesClient = clientBuilder.build();
        log.info("[SES] region={} endpoint={}", regionName, endpointUri == null ? "(aws)" : endpointUri);
        return sesClient;
    }

    @Bean
    @ConditionalOnMissingBean(TemplateJsonSerializer.class)
    public TemplateJsonSerializer templateJsonSerializer() {
        return new TemplateJsonSerializer();
    }

    @Bean
    @ConditionalOnExpression("'${notification.transport:}'=='ses' or '${notification.transport:}'=='sesv2'")
    @ConditionalOnMissingBean(EmailTransportService.class)
    public EmailTransportService sesV2EmailTransport(SesV2Client sesClient,
                                                     NotificationSesProps props,
                                                     TemplateJsonSerializer json) {
        return new SesV2EmailTransport(sesClient, props, json);
    }

    @Bean
    @ConditionalOnMissingBean(NotificationService.class)
    public NotificationService notificationService(
            SesV2Client sesClient,
            NotificationSesEmailProps emailProperties,
            NotificationSesProps infraProperties,
            ObjectMapper objectMapper
    ) {
        return new SesNotificationServiceImpl(sesClient, emailProperties, infraProperties, objectMapper);
    }

    private static URI normalizeEndpoint(String rawEndpoint) {
        if (rawEndpoint == null || rawEndpoint.isBlank()) return null;

        String urlWithScheme = rawEndpoint.matches("^[a-zA-Z]+://.*") ? rawEndpoint : "http://" + rawEndpoint;
        URI initialUri = URI.create(urlWithScheme);

        boolean needsDefaultPort = initialUri.getPort() == -1
                && ("localhost".equalsIgnoreCase(initialUri.getHost()) || "localstack".equalsIgnoreCase(initialUri.getHost()));

        if (needsDefaultPort) {
            urlWithScheme = urlWithScheme.replaceFirst("^(http://[^/:]+)", "$1:4566");
        }

        return URI.create(urlWithScheme);
    }
}
