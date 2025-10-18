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
import java.net.InetAddress;
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

        SesV2ClientBuilder b = SesV2Client.builder()
                .httpClient(http)
                .overrideConfiguration(override)
                .region(Region.of(regionStr))
                .credentialsProvider(DefaultCredentialsProvider.create());

        ResolvedEndpoint resolved = resolveEndpoint(infra.getEndpoint());
        if (resolved.uri != null) {
            b = b.endpointOverride(resolved.uri)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test","test")));
        }

        SesV2Client client = b.build();
        log.info("[SES] region={} endpoint={} (reason: {})",
                regionStr,
                resolved.uri == null ? "(aws)" : resolved.uri,
                resolved.reason);
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

    // ---------- helpers ----------

    private record ResolvedEndpoint(URI uri, String reason) {}

    private static ResolvedEndpoint resolveEndpoint(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ResolvedEndpoint(null, "no endpoint override");
        }

        // add scheme if missing
        String url = raw.matches("^[a-zA-Z]+://.*") ? raw : "http://" + raw;

        boolean inContainer = new File("/.dockerenv").exists();
        String host = URI.create(url).getHost();
        int port = URI.create(url).getPort();

        // default LocalStack port if none provided
        if (port == -1 && ("localhost".equals(host) || "localstack".equals(host))) {
            url = url.replaceFirst("^(http://[^/:]+)", "$1:4566");
            host = URI.create(url).getHost();
            port = URI.create(url).getPort();
        }

        // Host JVM: translate localstack -> localhost
        if (!inContainer && "localstack".equalsIgnoreCase(host)) {
            String mapped = url.replaceFirst("://localstack", "://localhost");
            return new ResolvedEndpoint(URI.create(mapped), "host maps localstack→localhost");
        }

        // In container: ensure localstack DNS actually resolves; if not, fall back to host.docker.internal
        if (inContainer && "localstack".equalsIgnoreCase(host)) {
            if (!resolves(host)) {
                String fallback = url.replaceFirst("://localstack", "://host.docker.internal");
                return new ResolvedEndpoint(URI.create(fallback), "container fallback to host.docker.internal");
            }
        }

        // Also guard against someone passing just "localstack" with no scheme/port
        if ("localstack".equalsIgnoreCase(host) && !resolves(host)) {
            // If we got here, we're probably on the host (or miswired container).
            String mapped = url.replaceFirst("://localstack", inContainer ? "://host.docker.internal" : "://localhost");
            if (!mapped.contains(":")) mapped = mapped.replaceFirst("^(http://[^/]+)", "$1:4566");
            return new ResolvedEndpoint(URI.create(mapped), "auto-mapped unresolved localstack");
        }

        return new ResolvedEndpoint(URI.create(url), "as-configured");
    }

    private static boolean resolves(String h) {
        try { InetAddress.getByName(h); return true; } catch (Exception ignored) { return false; }
    }
}