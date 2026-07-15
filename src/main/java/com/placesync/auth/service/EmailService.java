package com.placesync.auth.service;

import com.placesync.common.metrics.PlaceSyncMetrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final TemplateEngine templateEngine;
    private final PlaceSyncMetrics placeSyncMetrics;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async
    public void sendEmailVerification(String to, String verificationToken) {
        Context ctx = new Context();
        ctx.setVariable("verificationLink", frontendUrl + "/verify-email?token=" + verificationToken);
        send(to, "Verify your PlaceSync email address", "email/email-verification", ctx);
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        Context ctx = new Context();
        ctx.setVariable("resetLink", frontendUrl + "/reset-password?token=" + resetToken);
        send(to, "Reset your PlaceSync password", "email/password-reset", ctx);
    }

    @Async
    public void sendAccountLocked(String to) {
        send(to, "Your PlaceSync account has been locked", "email/account-locked", new Context());
    }

    @Async
    public void sendRecruiterApprovedEmail(String to, String recruiterName) {
        Context ctx = new Context();
        ctx.setVariable("recruiterName", recruiterName);
        send(to, "Your PlaceSync recruiter profile has been approved", "email/recruiter-approved", ctx);
    }

    @Async
    public void sendRecruiterRejectedEmail(String to, String recruiterName, String reason) {
        Context ctx = new Context();
        ctx.setVariable("recruiterName", recruiterName);
        ctx.setVariable("reason", reason);
        send(to, "Update on your PlaceSync recruiter verification", "email/recruiter-rejected", ctx);
    }

    @Async
    public void sendApplicationConfirmation(String to, String jobTitle, String companyName) {
        Context ctx = new Context();
        ctx.setVariable("jobTitle", jobTitle);
        ctx.setVariable("companyName", companyName);
        send(to, "Application submitted — " + jobTitle + " at " + companyName, "email/application-confirmation", ctx);
    }

    @Async
    public void sendApplicationStatusUpdate(String to, String newStatus) {
        Context ctx = new Context();
        ctx.setVariable("newStatus", newStatus);
        send(to, "Your application status has been updated", "email/application-status-update", ctx);
    }

    @Async
    public void sendInterviewScheduled(String to, int round, String scheduledAt, String meetingLink) {
        Context ctx = new Context();
        ctx.setVariable("round", round);
        ctx.setVariable("scheduledAt", scheduledAt);
        ctx.setVariable("meetingLink", meetingLink);
        send(to, "Interview scheduled — Round " + round, "email/interview-scheduled", ctx);
    }

    @Async
    public void sendInterviewRescheduled(String to, int round, String newScheduledAt) {
        Context ctx = new Context();
        ctx.setVariable("round", round);
        ctx.setVariable("newScheduledAt", newScheduledAt);
        send(to, "Interview rescheduled — Round " + round, "email/interview-rescheduled", ctx);
    }

    @Async
    public void sendInterviewCancelled(String to, String reason) {
        Context ctx = new Context();
        ctx.setVariable("reason", reason);
        send(to, "Your interview has been cancelled", "email/interview-cancelled", ctx);
    }

    private void send(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx); // NOSONAR S5145: user data is intentionally rendered into email content, not into logs
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(from)
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(html).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
            log.info("Email sent template={}", template);
        } catch (SdkException e) {
            placeSyncMetrics.recordEmailFailure();
            log.warn("Failed to send email template={}", template, e);
        }
    }
}
