package com.nimbly.phshoesbackend.notification.core.service.impl;

import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.service.EmailCompositionService;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CoreNotificationServiceImplTest {
    @Test
    void composesAndSendsEmail() {
        // Arrange
        EmailCompositionService compositionService = mock(EmailCompositionService.class);
        EmailTransportService transportService = mock(EmailTransportService.class);
        CoreNotificationServiceImpl service = new CoreNotificationServiceImpl(compositionService, transportService);
        EmailRequest request = EmailRequest.builder()
                .to(EmailAddress.builder().address("user@ph-shoes.app").build())
                .subject("Verify")
                .build();
        ComposedEmail composedEmail = new ComposedEmail(
                request,
                "raw",
                "no-reply@ph-shoes.app",
                List.of("user@ph-shoes.app"),
                List.of(),
                List.of(),
                "Verify",
                null
        );
        SendResult expected = SendResult.builder()
                .messageId("id-123")
                .provider("smtp")
                .acceptedAt(Instant.now())
                .build();
        when(compositionService.compose(request)).thenReturn(composedEmail);
        when(transportService.send(composedEmail)).thenReturn(expected);

        // Act
        SendResult result = service.sendEmailVerification(request);

        // Assert
        assertThat(result).isEqualTo(expected);
        verify(compositionService).compose(request);
        verify(transportService).send(composedEmail);
    }
}
