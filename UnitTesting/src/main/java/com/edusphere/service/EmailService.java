package com.edusphere.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Service gửi email.
 * @Async để không block luồng chính khi gửi email.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi mã OTP xác thực đăng ký.
     */
    @Async
    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        String subject = "EduSphere — Mã xác thực OTP của bạn";
        String body = buildOtpEmailBody(fullName, otp);
        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Gửi email xác minh tài khoản (link).
     */
    @Async
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String verifyLink = baseUrl + "/verify-email?token=" + token;
        String subject = "EduSphere — Xác minh tài khoản của bạn";
        String body = buildVerificationEmailBody(fullName, verifyLink);
        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Gửi OTP reset mật khẩu.
     */
    @Async
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otp) {
        String subject = "EduSphere — Mã OTP đặt lại mật khẩu";
        String body = buildPasswordResetOtpBody(fullName, otp);
        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Gửi email reset mật khẩu (link cũ — giữ lại để tương thích).
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "EduSphere — Đặt lại mật khẩu";
        String body = buildPasswordResetEmailBody(fullName, resetLink);
        sendHtmlEmail(toEmail, subject, body);
    }

    // ---- Private helpers ----

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            // Log lỗi nhưng không ném exception ra ngoài để không làm hỏng luồng chính
            // Trong production nên dùng logger thay System.err
            System.err.println("Lỗi gửi email tới " + to + ": " + e.getMessage());
        }
    }

    private String buildOtpEmailBody(String fullName, String otp) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333; background:#f5f5f5;">
                  <div style="max-width:520px; margin:40px auto; background:#fff; border-radius:16px;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08); overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#1976d2,#42a5f5); padding:28px 32px;">
                      <h2 style="color:#fff; margin:0; font-size:1.4rem;">🎓 EduSphere</h2>
                      <p style="color:rgba(255,255,255,0.85); margin:4px 0 0; font-size:0.9rem;">Xác thực tài khoản</p>
                    </div>
                    <div style="padding:32px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p style="margin:12px 0;">Đây là mã OTP để xác thực tài khoản EduSphere của bạn:</p>
                      <div style="text-align:center; margin:28px 0;">
                        <div style="display:inline-block; background:#f0f6ff; border:2px dashed #1976d2;
                                    border-radius:12px; padding:18px 40px;">
                          <span style="font-size:2.4rem; font-weight:900; letter-spacing:10px; color:#1565c0;">%s</span>
                        </div>
                      </div>
                      <p style="color:#e53935; font-weight:600; text-align:center; font-size:0.88rem;">
                        ⏱ Mã này có hiệu lực trong <strong>10 phút</strong>
                      </p>
                      <p style="margin-top:20px; color:#78909c; font-size:0.85rem;">
                        Nếu bạn không thực hiện đăng ký, hãy bỏ qua email này.
                      </p>
                    </div>
                    <div style="background:#f8f9fa; padding:16px 32px; border-top:1px solid #eee;">
                      <small style="color:#bbb;">EduSphere — Nền tảng học trực tuyến</small>
                    </div>
                  </div>
                </body></html>
                """.formatted(fullName, otp);
    }

    private String buildVerificationEmailBody(String fullName, String verifyLink) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; padding:20px; border:1px solid #e0e0e0; border-radius:8px;">
                    <h2 style="color:#4A90E2;">Chào mừng đến với EduSphere!</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Cảm ơn bạn đã đăng ký. Vui lòng nhấp vào nút bên dưới để xác minh địa chỉ email:</p>
                    <div style="text-align:center; margin:30px 0;">
                      <a href="%s"
                         style="background-color:#4A90E2; color:#fff; padding:14px 28px;
                                text-decoration:none; border-radius:5px; font-size:16px;">
                        Xác minh Email
                      </a>
                    </div>
                    <p>Link này sẽ hết hạn sau <strong>24 giờ</strong>.</p>
                    <p>Nếu bạn không tạo tài khoản, hãy bỏ qua email này.</p>
                    <hr/>
                    <small style="color:#999;">EduSphere — Nền tảng học trực tuyến</small>
                  </div>
                </body></html>
                """.formatted(fullName, verifyLink);
    }

    private String buildPasswordResetOtpBody(String fullName, String otp) {
        return """
                <html><body style="font-family:Arial,sans-serif;color:#333;background:#f5f5f5;">
                  <div style="max-width:520px;margin:40px auto;background:#fff;border-radius:16px;
                              box-shadow:0 4px 20px rgba(0,0,0,0.08);overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#e53935,#ef5350);padding:28px 32px;">
                      <h2 style="color:#fff;margin:0;font-size:1.4rem;">🔑 EduSphere</h2>
                      <p style="color:rgba(255,255,255,0.85);margin:4px 0 0;font-size:0.9rem;">Đặt lại mật khẩu</p>
                    </div>
                    <div style="padding:32px;">
                      <p>Xin chào <strong>%s</strong>,</p>
                      <p style="margin:12px 0;">Mã OTP để đặt lại mật khẩu EduSphere của bạn:</p>
                      <div style="text-align:center;margin:28px 0;">
                        <div style="display:inline-block;background:#fff5f5;border:2px dashed #e53935;
                                    border-radius:12px;padding:18px 40px;">
                          <span style="font-size:2.4rem;font-weight:900;letter-spacing:10px;color:#c62828;">%s</span>
                        </div>
                      </div>
                      <p style="color:#e53935;font-weight:600;text-align:center;font-size:0.88rem;">
                        ⏱ Mã này có hiệu lực trong <strong>10 phút</strong>
                      </p>
                      <p style="margin-top:20px;color:#78909c;font-size:0.85rem;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                      </p>
                    </div>
                    <div style="background:#f8f9fa;padding:16px 32px;border-top:1px solid #eee;">
                      <small style="color:#bbb;">EduSphere — Nền tảng học trực tuyến</small>
                    </div>
                  </div>
                </body></html>
                """.formatted(fullName, otp);
    }

    private String buildPasswordResetEmailBody(String fullName, String resetLink) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #333;">
                  <div style="max-width:600px; margin:auto; padding:20px; border:1px solid #e0e0e0; border-radius:8px;">
                    <h2 style="color:#E25C4A;">Đặt lại mật khẩu EduSphere</h2>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                    <div style="text-align:center; margin:30px 0;">
                      <a href="%s"
                         style="background-color:#E25C4A; color:#fff; padding:14px 28px;
                                text-decoration:none; border-radius:5px; font-size:16px;">
                        Đặt lại mật khẩu
                      </a>
                    </div>
                    <p>Link này sẽ hết hạn sau <strong>30 phút</strong>.</p>
                    <p>Nếu bạn không yêu cầu, hãy bỏ qua email này. Mật khẩu của bạn sẽ không thay đổi.</p>
                    <hr/>
                    <small style="color:#999;">EduSphere — Nền tảng học trực tuyến</small>
                  </div>
                </body></html>
                """.formatted(fullName, resetLink);
    }
}
