package com.nimbly.phshoesbackend.notification.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Attachment {

    @NotBlank
    String filename;

    /**
     * Raw bytes of the attachment. Providers may stream this differently internally.
     */
    byte[] content;

    /**
     * Example: "application/pdf", "image/png", "text/plain".
     */
    @NotBlank
    String mimeType;
}