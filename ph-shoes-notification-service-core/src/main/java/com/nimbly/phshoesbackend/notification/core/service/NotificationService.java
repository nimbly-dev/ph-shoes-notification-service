package com.nimbly.phshoesbackend.notification.core.service;

import com.nimbly.phshoesbackend.notification.core.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.dto.SendResult;

public interface NotificationService {
    SendResult sendEmailVerification(EmailRequest req);
}
