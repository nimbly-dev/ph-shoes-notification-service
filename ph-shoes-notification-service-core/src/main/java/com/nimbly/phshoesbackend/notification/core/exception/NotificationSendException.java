package com.nimbly.phshoesbackend.notification.core.exception;

public class NotificationSendException extends RuntimeException {
    public NotificationSendException(String message, Exception e) {
        super(message);
    }
}
