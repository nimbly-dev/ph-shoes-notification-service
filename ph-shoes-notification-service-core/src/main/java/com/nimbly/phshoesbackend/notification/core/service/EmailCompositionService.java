package com.nimbly.phshoesbackend.notification.core.service;


import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;

public interface EmailCompositionService {
    ComposedEmail compose(EmailRequest request);
}