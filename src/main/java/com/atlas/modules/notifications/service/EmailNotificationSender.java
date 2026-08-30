package com.atlas.modules.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * v1 email delivery: log-only. Real SMTP/provider wiring comes with billing/infra hardening.
 */
@Service
public class EmailNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    public boolean send(UUID userId, String email, String title, String body) {
        log.info("email.notification userId={} to={} title={}", userId, email, title);
        return email != null && !email.isBlank();
    }
}
