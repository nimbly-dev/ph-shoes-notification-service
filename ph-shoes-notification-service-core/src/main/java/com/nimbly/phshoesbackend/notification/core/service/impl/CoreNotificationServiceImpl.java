package com.nimbly.phshoesbackend.notification.core.service.impl;

import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;
import com.nimbly.phshoesbackend.notification.core.service.EmailCompositionService;
import com.nimbly.phshoesbackend.notification.core.service.EmailTransportService;
import com.nimbly.phshoesbackend.notification.core.service.NotificationService;
import org.springframework.stereotype.Service;

@Service
public class CoreNotificationServiceImpl implements NotificationService {

    private final EmailCompositionService composer;
    private final EmailTransportService emailTransport;

    public CoreNotificationServiceImpl(EmailCompositionService composer, EmailTransportService emailTransport) {
        this.composer = composer;
        this.emailTransport = emailTransport;
    }

    @Override
    public SendResult sendEmailVerification(EmailRequest req) throws NotificationSendException {
        ComposedEmail email = composer.compose(req);
        return emailTransport.send(email);
    }
}