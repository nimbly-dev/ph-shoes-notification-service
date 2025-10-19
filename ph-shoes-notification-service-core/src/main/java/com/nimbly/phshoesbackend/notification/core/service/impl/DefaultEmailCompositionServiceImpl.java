package com.nimbly.phshoesbackend.notification.core.service.impl;

import com.nimbly.phshoesbackend.notification.core.model.dto.ComposedEmail;
import com.nimbly.phshoesbackend.notification.core.model.dto.EmailRequest;
import com.nimbly.phshoesbackend.notification.core.model.props.NotificationEmailProps;
import com.nimbly.phshoesbackend.notification.core.service.EmailCompositionService;
import com.nimbly.phshoesbackend.notification.core.util.EmailAddressFormatter;
import com.nimbly.phshoesbackend.notification.core.util.EmailSubjectFormatter;
import com.nimbly.phshoesbackend.notification.core.util.RawMimeBuilder;

public class DefaultEmailCompositionServiceImpl implements EmailCompositionService {

    private final NotificationEmailProps emailProps;
    private final EmailSubjectFormatter subjectFormatter;
    private final EmailAddressFormatter addressFormatter;
    private final RawMimeBuilder rawMimeBuilder;

    public DefaultEmailCompositionServiceImpl(NotificationEmailProps emailProps,
                                              EmailSubjectFormatter subjectFormatter,
                                              EmailAddressFormatter addressFormatter,
                                              RawMimeBuilder rawMimeBuilder) {
        this.emailProps = emailProps;
        this.subjectFormatter = subjectFormatter;
        this.addressFormatter = addressFormatter;
        this.rawMimeBuilder = rawMimeBuilder;
    }

    @Override
    public ComposedEmail compose(EmailRequest request) {
        String subject = subjectFormatter.withPrefix(request.getSubject(), emailProps.getSubjectPrefix());
        String raw = rawMimeBuilder.build(request, subject);
        String from = (request.getFrom() != null) ? addressFormatter.format(request.getFrom()) : emailProps.getFrom();

        return new ComposedEmail(
                request,
                raw,
                from,
                addressFormatter.formatAll(request.getTo()),
                addressFormatter.formatAll(request.getCc()),
                addressFormatter.formatAll(request.getBcc()),
                subject,
                request.getTags()
        );
    }
}
