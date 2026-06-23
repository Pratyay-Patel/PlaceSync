package com.placesync.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    // Phase 5: replace with real Spring Mail + Gmail SMTP implementation.
    // For now these methods log the token so developers can manually verify
    // email flows during local development without an SMTP server.

    public void sendEmailVerification(String to, String verificationToken) {
        log.info("[EMAIL STUB] Verification token for {}: {}", to, verificationToken);
    }

    public void sendPasswordResetEmail(String to, String resetToken) {
        log.info("[EMAIL STUB] Password reset token for {}: {}", to, resetToken);
    }

    public void sendRecruiterApprovedEmail(String to, String recruiterName) {
        log.info("[EMAIL STUB] Recruiter approved email → {}", to);
    }

    public void sendRecruiterRejectedEmail(String to, String recruiterName, String reason) {
        log.info("[EMAIL STUB] Recruiter rejected email → {}", to);
    }
}
