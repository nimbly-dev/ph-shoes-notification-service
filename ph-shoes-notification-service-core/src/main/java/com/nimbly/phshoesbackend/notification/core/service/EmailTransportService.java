package com.nimbly.phshoesbackend.notification.core.service;

import com.nimbly.phshoesbackend.notification.core.exception.NotificationSendException;
import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;

public interface EmailTransportService {
    SendResult send(ComposedEmail email) throws NotificationSendException;
}
