package com.nimbly.phshoesbackend.notification.core.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class EmailAddress {
    String name;

    @NotBlank
    @Email
    String address;
}
