package com.griddynamics.forge.market_presence_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Fallback implementation of EmailService — active when app.email.smtp.enabled is false
 * (or absent). Writes confirmation details to the application log so the apply flow works
 * end-to-end in dev/CI without an SMTP server.
 *
 * When app.email.smtp.enabled=true Spring selects SmtpEmailService instead and this
 * bean is not registered.
 */
@Service
@ConditionalOnProperty(name = "app.email.smtp.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle) {
        log.info("""
                [EMAIL] Application confirmation
                  To      : {}
                  Subject : Application Received — {}
                  Body    : Dear {}, thank you for applying for the {} position at Forge AI. \
                We have received your application and our recruitment team will be in touch within 5 business days.
                """,
                toEmail, jobTitle, candidateName, jobTitle);
    }
}
