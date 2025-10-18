package com.nimbly.phshoesbackend.notification.email.providers.smtp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.smtp")
@Data
public class NotificationSmtpProps {
    private String defaultFrom;

    /** Enable adding List-Unsubscribe headers (generally for marketing/list mail) */
    private boolean enableListUnsubscribe = false;

    /** Also add RFC 8058 one-click hint: List-Unsubscribe-Post: List-Unsubscribe=One-Click */
    private boolean enableOneClick = true;

    /** mailto address, e.g. unsubscribe@phshoes.app */
    private String unsubscribeMailto;

    /** HTTPS base URL for one-click/standard unsubscribe, e.g. https://phshoes.app/api/v1/unsubscribe */
    private String unsubscribeHttpBase;

    /** stable identifier for the list, e.g. "PH Shoes News <news.phshoes.app>" */
    private String listId;

    /**
     * If true, add List-Unsubscribe to ALL emails including transactional.
     * If false (recommended), you should only set it from callers that send marketing mail
     * by temporarily toggling this prop in that environment or via a dedicated provider bean.
     */
    private boolean forceForAllMail = false;
}
