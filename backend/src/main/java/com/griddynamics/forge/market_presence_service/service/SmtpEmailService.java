package com.griddynamics.forge.market_presence_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Production implementation of EmailService (REQ-JP-07).
 *
 * Active only when app.email.smtp.enabled=true.  Configure the underlying
 * JavaMailSender via standard spring.mail.* properties in application.yml or
 * as environment variables.
 *
 * Local development: start Mailhog (docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog)
 * and set app.email.smtp.enabled=true — emails appear at http://localhost:8025.
 *
 * Production: set spring.mail.host/port/username/password to your SMTP provider
 * (SendGrid, SES, Mailgun, etc.) and set app.email.smtp.enabled=true.
 *
 * Failures are caught and logged; they never propagate to interrupt the apply flow.
 */
@Service
@ConditionalOnProperty(name = "app.email.smtp.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String         fromAddress;
    private final String         portalBaseUrl;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.email.smtp.from:noreply@forge-ai.com}") String fromAddress,
            @Value("${app.channels.careers-portal.base-url:http://localhost:5173}") String portalBaseUrl) {
        this.mailSender    = mailSender;
        this.fromAddress   = fromAddress;
        this.portalBaseUrl = portalBaseUrl;
    }

    @Override
    public void sendApplicationConfirmation(String toEmail, String candidateName, String jobTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Application Received — " + jobTitle);
            helper.setText(plainText(candidateName, jobTitle), html(toEmail, candidateName, jobTitle));

            mailSender.send(message);
            log.info("[EMAIL] Confirmation sent  to={} job='{}'", toEmail, jobTitle);

        } catch (MessagingException e) {
            log.error("[EMAIL] Failed to send confirmation to={} job='{}' error={}",
                    toEmail, jobTitle, e.getMessage());
        }
    }

    // ── Templates ────────────────────────────────────────────────────────────

    private String plainText(String name, String jobTitle) {
        return """
                Dear %s,

                Thank you for applying for the %s position at Forge AI.

                We have received your application and our recruitment team will review it within 5 business days.

                Track your application status at: %s/applications

                Best regards,
                The Forge AI Recruitment Team
                """.formatted(name, jobTitle, portalBaseUrl);
    }

    private String html(String toEmail, String name, String jobTitle) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 16px;">
                    <tr><td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%%;">

                        <!-- Header -->
                        <tr>
                          <td style="background:#1e40af;padding:24px 32px;border-radius:12px 12px 0 0;">
                            <span style="color:white;font-size:22px;font-weight:700;letter-spacing:-0.5px;">
                              FORGE <span style="color:#93c5fd;">AI</span>
                            </span>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="background:#ffffff;padding:36px 32px;border:1px solid #e2e8f0;border-top:none;">
                            <h2 style="margin:0 0 4px;color:#1e293b;font-size:20px;">Application Received ✓</h2>
                            <p style="margin:0 0 20px;color:#64748b;font-size:14px;">
                              Confirmation for: <strong>%s</strong>
                            </p>
                            <p style="color:#334155;">Dear <strong>%s</strong>,</p>
                            <p style="color:#475569;line-height:1.6;">
                              Thank you for applying for the <strong>%s</strong> position at Forge AI.
                              We have received your application and our recruitment team will review it
                              within <strong>5 business days</strong>.
                            </p>

                            <!-- CTA -->
                            <table cellpadding="0" cellspacing="0" style="margin:28px 0;">
                              <tr>
                                <td style="background:#1d4ed8;border-radius:8px;padding:12px 24px;">
                                  <a href="%s/applications"
                                     style="color:white;text-decoration:none;font-weight:600;font-size:14px;">
                                    Track My Application →
                                  </a>
                                </td>
                              </tr>
                            </table>

                            <p style="color:#64748b;font-size:14px;line-height:1.6;">
                              If you have any questions please contact us at
                              <a href="mailto:careers@forge-ai.com" style="color:#1d4ed8;">careers@forge-ai.com</a>.
                            </p>
                            <p style="color:#64748b;font-size:14px;">
                              Best regards,<br>
                              <strong>The Forge AI Recruitment Team</strong>
                            </p>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8fafc;padding:16px 32px;border-radius:0 0 12px 12px;
                                     border:1px solid #e2e8f0;border-top:none;">
                            <p style="margin:0;color:#94a3b8;font-size:11px;">
                              This email was sent to %s as part of your job application at Forge AI.
                              If you did not submit this application please disregard this message.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(jobTitle, name, jobTitle, portalBaseUrl, toEmail);
    }
}
