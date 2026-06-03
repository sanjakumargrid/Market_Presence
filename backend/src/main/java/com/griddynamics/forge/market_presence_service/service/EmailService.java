package com.griddynamics.forge.market_presence_service.service;

/**
 * Abstraction for sending transactional emails (REQ-JP-07).
 *
 * The active implementation is resolved by Spring at startup:
 *  - Development/test: LoggingEmailService — writes to the application log only.
 *  - Production: replace with an SMTP or SendGrid implementation that honours
 *    the same interface without touching any other class.
 */
public interface EmailService {

    /**
     * Send an application-received confirmation to the candidate.
     *
     * @param toEmail       Recipient email address
     * @param candidateName Candidate's full name (used in salutation)
     * @param jobTitle      Title of the job applied for (used in subject and body)
     */
    void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle);
}
