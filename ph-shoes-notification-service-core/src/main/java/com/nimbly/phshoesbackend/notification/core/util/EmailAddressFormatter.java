package com.nimbly.phshoesbackend.notification.core.util;

import com.nimbly.phshoesbackend.notification.core.model.dto.EmailAddress;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailAddressFormatter  {
    public String format(EmailAddress value) {
        if (value == null) return null;
        String name = value.getName();
        String addr = value.getAddress();
        return (name == null || name.isBlank()) ? addr : name + " <" + addr + ">";
    }
    public List<String> formatAll(List<EmailAddress> values) {
        return (values == null || values.isEmpty()) ? List.of() : values.stream().map(this::format).toList();
    }
}
