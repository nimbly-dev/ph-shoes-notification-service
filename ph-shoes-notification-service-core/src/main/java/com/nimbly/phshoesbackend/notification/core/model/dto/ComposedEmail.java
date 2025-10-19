package com.nimbly.phshoesbackend.notification.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ComposedEmail {
    private EmailRequest request;
    private String rawMime;
    private String from;
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private Map<String, String> tags;
}
