package com.placesync.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    public void sendEmailVerification(String to, String verificationToken) {
        Context ctx = new Context();
        ctx.setVariable("verificationLink", baseUrl + "/api/v1/auth/verify-email?token=" + verificationToken);
        send(to, "Verify your PlaceSync email address", "email/email-verification", ctx);
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetToken) {
        Context ctx = new Context();
        ctx.setVariable("resetLink", baseUrl + "/api/v1/auth/reset-password?token=" + resetToken);
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
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent template={} to={}", template, to);
        } catch (MailException | MessagingException e) {
            log.warn("Failed to send email template={} to={}: {}", template, to, e.getMessage());
        }
    }
}
