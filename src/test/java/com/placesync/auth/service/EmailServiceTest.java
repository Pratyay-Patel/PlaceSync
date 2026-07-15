package com.placesync.auth.service;

import jakarta.mail.internet.MimeMessage;
import com.placesync.common.metrics.PlaceSyncMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;
    @Mock MimeMessage mimeMessage;
    @Mock PlaceSyncMetrics placeSyncMetrics;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine, placeSyncMetrics);
        ReflectionTestUtils.setField(emailService, "from", "noreply@placesync.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");
    }

    @Test
    void sendEmailVerification_validInput_callsMailSender() {
        emailService.sendEmailVerification("student@example.com", "token123");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendPasswordResetEmail_validInput_callsMailSender() {
        emailService.sendPasswordResetEmail("student@example.com", "reset-token");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendRecruiterApprovedEmail_validInput_callsMailSender() {
        emailService.sendRecruiterApprovedEmail("recruiter@company.com", "Jane Doe");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendRecruiterRejectedEmail_validInput_callsMailSender() {
        emailService.sendRecruiterRejectedEmail("recruiter@company.com", "Jane Doe", "Incomplete profile");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendApplicationConfirmation_validInput_callsMailSender() {
        emailService.sendApplicationConfirmation("student@example.com", "Software Engineer", "Acme Corp");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendApplicationStatusUpdate_validInput_callsMailSender() {
        emailService.sendApplicationStatusUpdate("student@example.com", "SHORTLISTED");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInterviewScheduled_validInput_callsMailSender() {
        emailService.sendInterviewScheduled("student@example.com", 1, "2025-08-01T10:00:00Z", "https://meet.example.com/x");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInterviewRescheduled_validInput_callsMailSender() {
        emailService.sendInterviewRescheduled("student@example.com", 1, "2025-08-05T10:00:00Z");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendInterviewCancelled_validInput_callsMailSender() {
        emailService.sendInterviewCancelled("student@example.com", "Recruiter unavailable");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendAccountLocked_validInput_callsMailSender() {
        emailService.sendAccountLocked("student@example.com");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void send_mailExceptionThrown_logsWarnAndDoesNotPropagate() {
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatNoException().isThrownBy(() -> emailService.sendEmailVerification("bad@example.com", "token"));
    }
}
