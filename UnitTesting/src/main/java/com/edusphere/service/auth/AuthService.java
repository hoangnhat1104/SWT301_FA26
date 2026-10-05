package com.edusphere.service.auth;

import com.edusphere.dto.RegisterRequest;
import com.edusphere.entity.*;
import com.edusphere.repository.*;
import com.edusphere.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Date;

/**
 * Service xử lý đăng ký theo flow OTP:
 *
 * 1. User điền form đăng ký
 * 2. Validate input, kiểm tra email trùng
 * 3. Tạo user với status = PENDING
 * 4. Gán role STUDENT
 * 5. Tạo mã OTP 6 số, lưu vào email_verification_tokens (dùng cột token)
 * 6. Gửi OTP về email
 * 7. User nhập OTP vào trang xác thực
 * 8. Nếu đúng → status = ACTIVE → đăng ký thành công
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.email-verification.expiration-minutes}")
    private int emailVerificationExpirationMinutes;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * BƯỚC 1 — Xử lý form đăng ký.
     * Tạo user PENDING, sinh OTP, gửi email.
     * Trả về email để controller chuyển sang trang nhập OTP.
     *
     * @throws IllegalArgumentException nếu email trùng hoặc password không khớp
     */
    @Transactional
    public String initiateRegister(RegisterRequest request) {

        // Validate confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        String email = request.getEmail().toLowerCase().trim();

        // Nếu email đã tồn tại với status ACTIVE → từ chối
        if (userRepository.existsByEmail(email)) {
            Users existing = userRepository.findByEmail(email).get();
            if ("ACTIVE".equals(existing.getStatus())) {
                throw new IllegalArgumentException("Email này đã được đăng ký. Vui lòng dùng email khác.");
            }
            // Nếu PENDING → xóa user cũ để tạo lại (user chưa verify)
            emailVerificationTokenRepository.deleteByUserId(existing);
            userRepository.delete(existing);
            userRepository.flush();
        }

        // Tạo user mới với status PENDING
        Users user = new Users();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setStatus("PENDING");
        user.setCreatedAt(new Date());
        user = userRepository.save(user);

        // Gán role STUDENT
        Roles studentRole = roleRepository.findByRoleName("STUDENT")
                .orElseThrow(() -> new RuntimeException("Role STUDENT không tồn tại."));
        UserRolesPK pk = new UserRolesPK(user.getUserId(), studentRole.getRoleId());
        userRoleRepository.save(new UserRoles(pk, new Date()));

        // Tạo OTP 6 số ngẫu nhiên bảo mật
        String otp = generateOtp();

        // Thời hạn OTP: 10 phút (đủ để user nhập)
        long expiryMs = 10L * 60 * 1000;
        Date expiresAt = new Date(System.currentTimeMillis() + expiryMs);

        EmailVerificationTokens otpToken = new EmailVerificationTokens(otp, expiresAt, user);
        emailVerificationTokenRepository.save(otpToken);

        // Gửi OTP qua email (async)
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        return email;
    }

    /**
     * BƯỚC 2 — Xác thực OTP do user nhập.
     * Nếu đúng → kích hoạt tài khoản (status = ACTIVE).
     *
     * @throws IllegalArgumentException nếu OTP sai, hết hạn, hoặc đã dùng
     */
    @Transactional
    public void verifyOtp(String email, String otpInput) {

        Users user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        // Lấy OTP token mới nhất chưa verify
        EmailVerificationTokens otpToken = emailVerificationTokenRepository
                .findByToken(otpInput.trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không đúng. Vui lòng kiểm tra lại."));

        // Kiểm tra OTP thuộc đúng user này
        if (!otpToken.getUserId().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ.");
        }

        // Kiểm tra đã dùng rồi chưa
        if (otpToken.getVerifiedAt() != null) {
            throw new IllegalArgumentException("Mã OTP này đã được sử dụng.");
        }

        // Kiểm tra hết hạn
        if (otpToken.getExpiresAt().before(new Date())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn (10 phút). Vui lòng đăng ký lại.");
        }

        // Kích hoạt tài khoản
        user.setStatus("ACTIVE");
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        // Đánh dấu OTP đã dùng
        otpToken.setVerifiedAt(new Date());
        emailVerificationTokenRepository.save(otpToken);
    }

    /**
     * Gửi lại OTP mới cho email (khi OTP cũ hết hạn).
     */
    @Transactional
    public void resendOtp(String email) {
        Users user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));

        if ("ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("Tài khoản này đã được xác minh rồi.");
        }

        // Xóa OTP cũ
        emailVerificationTokenRepository.deleteByUserId(user);

        // Tạo OTP mới
        String otp = generateOtp();
        long expiryMs = 10L * 60 * 1000;
        Date expiresAt = new Date(System.currentTimeMillis() + expiryMs);

        emailVerificationTokenRepository.save(new EmailVerificationTokens(otp, expiresAt, user));
        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
    }

    /**
     * Sinh mã OTP 6 số ngẫu nhiên bảo mật.
     * Dùng SecureRandom — KHÔNG dùng Math.random().
     */
    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(otp);
    }
}
