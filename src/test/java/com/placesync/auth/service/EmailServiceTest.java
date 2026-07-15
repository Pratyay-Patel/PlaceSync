package com.placesync.auth.service;

import com.placesync.common.metrics.PlaceSyncMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock SesClient sesClient;
    @Mock TemplateEngine templateEngine;
    @Mock PlaceSyncMetrics placeSyncMetrics;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(sesClient, templateEngine, placeSyncMetrics);
        ReflectionTestUtils.setField(emailService, "from", "noreply@placesync.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>test</html>");
    }

    @Test
    void sendEmailVerification_validInput_callsSesClient() {
        emailService.sendEmailVerification("student@example.com", "token123");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendPasswordResetEmail_validInput_callsSesClient() {
        emailService.sendPasswordResetEmail("student@example.com", "reset-token");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendRecruiterApprovedEmail_validInput_callsSesClient() {
        emailService.sendRecruiterApprovedEmail("recruiter@company.com", "Jane Doe");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendRecruiterRejectedEmail_validInput_callsSesClient() {
        emailService.sendRecruiterRejectedEmail("recruiter@company.com", "Jane Doe", "Incomplete profile");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendApplicationConfirmation_validInput_callsSesClient() {
        emailService.sendApplicationConfirmation("student@example.com", "Software Engineer", "Acme Corp");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendApplicationStatusUpdate_validInput_callsSesClient() {
        emailService.sendApplicationStatusUpdate("student@example.com", "SHORTLISTED");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendInterviewScheduled_validInput_callsSesClient() {
        emailService.sendInterviewScheduled("student@example.com", 1, "2025-08-01T10:00:00Z", "https://meet.example.com/x");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendInterviewRescheduled_validInput_callsSesClient() {
        emailService.sendInterviewRescheduled("student@example.com", 1, "2025-08-05T10:00:00Z");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendInterviewCancelled_validInput_callsSesClient() {
        emailService.sendInterviewCancelled("student@example.com", "Recruiter unavailable");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendAccountLocked_validInput_callsSesClient() {
        emailService.sendAccountLocked("student@example.com");

        verify(sesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void send_sdkExceptionThrown_logsWarnAndDoesNotPropagate() {
        doThrow(SdkException.create("SES error", new RuntimeException())).when(sesClient).sendEmail(any(SendEmailRequest.class));

        assertThatNoException().isThrownBy(() -> emailService.sendEmailVerification("bad@example.com", "token"));
    }
}
