package com.edusphere.service.auth;

import com.edusphere.entity.PasswordResetTokens;
import com.edusphere.entity.Users;
import com.edusphere.repository.PasswordResetTokenRepository;
import com.edusphere.repository.UserRepository;
import com.edusphere.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;

/**
 * Flow quên mật khẩu bằng OTP:
 * 1. User nhập email → gửi OTP 6 số về mail
 * 2. User nhập OTP → xác thực → được phép đặt mật khẩu mới
 * 3. User nhập mật khẩu mới → lưu DB, đánh dấu token đã dùng
 */
@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // OTP hết hạn sau 10 phút
    private static final long OTP_EXPIRY_MS = 10L * 60 * 1000;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * BƯỚC 1 — Nhận email, tạo OTP, gửi mail.
     * Luôn trả về thành công dù email có tồn tại hay không (bảo mật).
     */
    @Transactional
    public void processForgotPassword(String email) {
        Optional<Users> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) return; // Không tiết lộ email có tồn tại không

        Users user = userOpt.get();

        // Xóa OTP cũ nếu có
        passwordResetTokenRepository.deleteByUserId(user);

        // Tạo OTP 6 số
        String otp = String.valueOf(100000 + SECURE_RANDOM.nextInt(900000));
        Date expiresAt = new Date(System.currentTimeMillis() + OTP_EXPIRY_MS);

        PasswordResetTokens token = new PasswordResetTokens(otp, expiresAt, user);
        passwordResetTokenRepository.save(token);

        // Gửi OTP qua email (async)
        emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    /**
     * BƯỚC 2 — Xác thực OTP.
     * Trả về email của user nếu OTP đúng (để dùng ở bước 3).
     *
     * @throws IllegalArgumentException nếu OTP sai/hết hạn/đã dùng
     */
    @Transactional
    public String verifyResetOtp(String email, String otp) {
        Users user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        PasswordResetTokens token = passwordResetTokenRepository
                .findByToken(otp.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không đúng."));

        // Kiểm tra OTP thuộc đúng user này
        if (!token.getUserId().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ.");
        }

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Mã OTP này đã được sử dụng.");
        }

        if (token.getExpiresAt().before(new Date())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Đánh dấu OTP đã xác thực (nhưng chưa dùng xong — chờ bước đặt mật khẩu)
        // Lưu verified time tạm bằng cách để nguyên, controller sẽ dùng session

        return user.getEmail();
    }

    /**
     * BƯỚC 3 — Đặt mật khẩu mới sau khi OTP đã xác thực.
     *
     * @throws IllegalArgumentException nếu password không khớp
     */
    @Transactional
    public void resetPassword(String email, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        Users user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        // Tìm OTP token còn hiệu lực (chưa dùng)
        PasswordResetTokens token = passwordResetTokenRepository
                .findLatestValidByUserId(user)
                .orElseThrow(() -> new IllegalArgumentException("Phiên đặt lại mật khẩu đã hết hạn."));

        // Cập nhật mật khẩu
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        // Đánh dấu token đã dùng
        token.setUsedAt(new Date());
        passwordResetTokenRepository.save(token);
    }

    /**
     * Gửi lại OTP mới.
     */
    @Transactional
    public void resendResetOtp(String email) {
        processForgotPassword(email);
    }
}
