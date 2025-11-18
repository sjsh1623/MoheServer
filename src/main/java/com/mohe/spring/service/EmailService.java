package com.mohe.spring.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerificationEmail(String email, String verificationCode) {
        String verificationSection = """
            <div style="background-color:#f1f5f9; border:1px solid #e2e8f0; border-radius:20px; padding:28px 24px; text-align:center;">
                <p style="font-size:13px; letter-spacing:0.4em; color:#94a3b8; margin:0 0 12px 0;">CODE</p>
                <p style="font-size:42px; font-weight:700; letter-spacing:0.4em; color:#0f172a; margin:0 0 8px 0;">%s</p>
                <p style="font-size:14px; color:#475467; margin:0;">5분 안에 입력해주세요.</p>
            </div>
        """.formatted(verificationCode);

        String htmlContent = buildTossStyleEmail(
            "MOHE 이메일 인증 코드",
            "이메일 인증이 필요해요",
            "회원가입을 위해 아래 인증 코드를 입력해주세요.",
            verificationSection,
            "타인에게 인증 코드를 공유하지 말아 주세요."
        );

        try {
            sendHtmlEmail(email, "[MOHE] 이메일 인증 코드", htmlContent, null);
            logger.info("Verification email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send verification email to {}", email, e);
            // In development, log the code instead of sending email
            System.out.println("Verification code for " + email + ": " + verificationCode);
        }
    }

    @Async
    public void sendPasswordResetEmail(String email, String resetToken) {
        String resetUrl = "https://mohe.app/reset-password?token=" + resetToken;
        String resetSection = """
            <div style="text-align:center;">
                <a href="%s" style="display:inline-block; background:linear-gradient(135deg, #1d4ed8, #2563eb, #3b82f6); color:#ffffff; padding:16px 32px; border-radius:18px; font-size:16px; font-weight:600; text-decoration:none;">비밀번호 재설정</a>
                <p style="font-size:13px; color:#94a3b8; margin:20px 0 8px 0;">버튼이 보이지 않나요?</p>
                <div style="font-size:13px; color:#475467; line-height:1.6; word-break:break-all; background-color:#f8fafc; border-radius:16px; padding:16px 20px;">%s</div>
            </div>
        """.formatted(resetUrl, resetUrl);

        String htmlContent = buildTossStyleEmail(
            "비밀번호 재설정 안내",
            "안전하게 새 비밀번호를 만들어주세요",
            "비밀번호 재설정 요청을 확인했어요. 아래 버튼을 눌러 새로운 비밀번호를 설정하면 바로 로그인할 수 있습니다.",
            resetSection,
            "이 링크는 1시간 동안만 유효합니다. 본인이 요청하지 않았다면 계정 보안을 위해 즉시 비밀번호를 변경하고 고객센터로 알려주세요."
        );

        try {
            sendHtmlEmail(email, "[MOHE] 비밀번호 재설정", htmlContent, null);
            logger.info("Password reset email sent to {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}", email, e);
            // In development, log the token instead of sending email
            System.out.println("Password reset token for " + email + ": " + resetToken);
        }
    }

    private String buildTossStyleEmail(String preheader, String headline, String description, String primaryContent, String helperNote) {
        String finalPreheader = preheader != null ? preheader : description;
        int currentYear = Year.now().getValue();

        return """
            <html lang="ko">
            <body style="margin:0; padding:32px 16px; background-color:#f7f9fc; font-family:'Pretendard', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color:#1f2d3d;">
                <div style="max-width:560px; margin:0 auto;">
                    <div style="display:none; max-height:0; overflow:hidden; opacity:0; color:transparent;">%s</div>
                    <div style="display:flex; align-items:center; gap:12px; margin-bottom:16px;">
                        <div style="width:48px; height:48px; border-radius:16px; background:linear-gradient(135deg, #1d4ed8, #3b82f6); display:flex; align-items:center; justify-content:center; color:#ffffff; font-weight:700; font-size:18px;">M</div>
                        <div>
                            <p style="font-size:12px; letter-spacing:0.2em; text-transform:uppercase; color:#94a3b8; margin:0;">MOHE</p>
                        </div>
                    </div>
                    <div style="background-color:#ffffff; border-radius:28px; padding:40px 36px; box-shadow:0 24px 60px rgba(15, 23, 42, 0.08);">
                        <p style="font-size:12px; letter-spacing:0.4em; color:#94a3b8; text-transform:uppercase; margin:0 0 12px 0;">NOTICE</p>
                        <h1 style="font-size:28px; line-height:1.3; color:#0f172a; margin:0 0 12px 0;">%s</h1>
                        <p style="font-size:16px; line-height:1.7; color:#475467; margin:0 0 28px 0;">%s</p>
                        %s
                        <div style="margin-top:32px; padding:20px; border-radius:16px; background-color:#f8fafc;">
                            <p style="font-size:13px; line-height:1.6; color:#64748b; margin:0;">%s</p>
                        </div>
                    </div>
                    <p style="margin-top:16px; font-size:12px; color:#94a3b8; text-align:center;">© %d MOHE. All rights reserved.</p>
                </div>
            </body>
            </html>
        """.formatted(finalPreheader, headline, description, primaryContent, helperNote, currentYear);
    }

    /**
     * Send HTML email with content sanitization
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent, String from) {
        validateEmail(to);
        String sanitizedContent = sanitizeHtmlContent(htmlContent);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(new InternetAddress("with.mohe@gmail.com", "Mohe "));
            helper.setSubject(subject);
            helper.setText(sanitizedContent, true); // true = HTML content

            if (from != null) {
                helper.setFrom(from);
            }

            mailSender.send(mimeMessage);
            logger.info("HTML email sent to {} with subject: {}", to, subject);
        } catch (Exception e) {
            logger.error("Failed to send HTML email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send plain text email
     */
    @Async
    public void sendTextEmail(String to, String subject, String text, String from) {
        validateEmail(to);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        if (from != null) {
            message.setFrom(from);
        }

        mailSender.send(message);
        logger.info("Text email sent to {} with subject: {}", to, subject);
    }

    /**
     * Sanitize HTML content to prevent XSS attacks
     */
    private String sanitizeHtmlContent(String htmlContent) {
        return Jsoup.clean(htmlContent, Safelist.relaxed()
            .addTags("style")
            .addAttributes(":all", "style")
            .addProtocols("a", "href", "http", "https", "mailto")
        );
    }

    /**
     * Basic email validation
     */
    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }
}
