package com.nimbly.phshoesbackend.notification.core.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailRequest {

    @Valid
    EmailAddress from;

    @Singular("to")
    List<@Valid EmailAddress> to;

    @Singular("cc")
    List<@Valid EmailAddress> cc;

    @Singular("bcc")
    List<@Valid EmailAddress> bcc;

    @NotBlank
    String subject;

    // Either provide bodies directly...
    String textBody;
    String htmlBody;

    // ...or use templates:
    String templateId;

    @Singular("templateVar")
    Map<String, Object> templateVars;

    @Singular("header")
    Map<String, String> headers;

    @Singular("tag")
    Map<String, String> tags;

    @Singular("attachment")
    List<Attachment> attachments;

    /**
     * Optional idempotency key the caller can set so providers reuse the same message id
     * if a retry happens.
     */
    String requestIdHint;
}