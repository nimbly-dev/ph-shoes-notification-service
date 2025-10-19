package com.nimbly.phshoesbackend.notification.core.service;

import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.dto.SendResult;

public interface NotificationService {
    SendResult sendEmailVerification(EmailRequest req);
}
