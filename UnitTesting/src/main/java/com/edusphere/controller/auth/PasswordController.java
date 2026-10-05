package com.edusphere.controller.auth;

import com.edusphere.dto.ChangePasswordRequest;
import com.edusphere.dto.ForgotPasswordRequest;
import com.edusphere.security.CustomUserDetails;
import com.edusphere.service.ProfileService;
import com.edusphere.service.auth.PasswordResetService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PasswordController {

    private static final String SESSION_RESET_EMAIL        = "resetPasswordEmail";
    private static final String SESSION_RESET_OTP_VERIFIED = "resetOtpVerified";

    private final PasswordResetService passwordResetService;
    private final ProfileService profileService;

    public PasswordController(PasswordResetService passwordResetService,
                              ProfileService profileService) {
        this.passwordResetService = passwordResetService;
        this.profileService = profileService;
    }

    // ===== BƯỚC 1 — NHẬP EMAIL =====

    @GetMapping("/forgot-password")
    public String showForgotPage(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgot(@Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest req,
                                BindingResult bindingResult,
                                HttpSession session) {
        if (bindingResult.hasErrors()) return "auth/forgot-password";

        passwordResetService.processForgotPassword(req.getEmail());
        session.setAttribute(SESSION_RESET_EMAIL, req.getEmail().toLowerCase().trim());
        return "redirect:/forgot-password/verify-otp";
    }

    // ===== BƯỚC 2 — NHẬP OTP =====

    @GetMapping("/forgot-password/verify-otp")
    public String showResetOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";
        model.addAttribute("maskedEmail", maskEmail(email));
        return "auth/forgot-password-otp";
    }

    @PostMapping("/forgot-password/verify-otp")
    public String processResetOtp(@RequestParam("otp") String otp,
                                  HttpSession session,
                                  Model model) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";

        try {
            passwordResetService.verifyResetOtp(email, otp);
            session.setAttribute(SESSION_RESET_OTP_VERIFIED, true);
            return "redirect:/reset-password";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("maskedEmail", maskEmail(email));
            return "auth/forgot-password-otp";
        }
    }

    @PostMapping("/forgot-password/resend-otp")
    public String resendResetOtp(HttpSession session, RedirectAttributes ra) {
        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        if (email == null) return "redirect:/forgot-password";
        passwordResetService.resendResetOtp(email);
        ra.addFlashAttribute("successMessage", "Đã gửi lại mã OTP mới.");
        return "redirect:/forgot-password/verify-otp";
    }

    // ===== BƯỚC 3 — ĐẶT MẬT KHẨU MỚI =====

    @GetMapping("/reset-password")
    public String showResetPasswordPage(HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute(SESSION_RESET_OTP_VERIFIED);
        if (verified == null || !verified) return "redirect:/forgot-password";
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes ra) {
        Boolean verified = (Boolean) session.getAttribute(SESSION_RESET_OTP_VERIFIED);
        if (verified == null || !verified) return "redirect:/forgot-password";

        if (newPassword.length() < 8) {
            model.addAttribute("errorMessage", "Mật khẩu phải có ít nhất 8 ký tự.");
            return "auth/reset-password";
        }

        String email = (String) session.getAttribute(SESSION_RESET_EMAIL);
        try {
            passwordResetService.resetPassword(email, newPassword, confirmPassword);
            session.removeAttribute(SESSION_RESET_EMAIL);
            session.removeAttribute(SESSION_RESET_OTP_VERIFIED);
            ra.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/reset-password";
        }
    }

    // ===== ĐỔI MẬT KHẨU (đã đăng nhập) =====

    @GetMapping("/change-password")
    public String showChangePasswordPage(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String processChangePassword(
            @Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) return "auth/change-password";

        try {
            profileService.changePassword(currentUser.getUserId(), request);
            ra.addFlashAttribute("successMessage", "Đổi mật khẩu thành công!");
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/change-password";
        }
    }

    // ===== HELPER =====

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        int show = Math.min(3, parts[0].length());
        return parts[0].substring(0, show) + "***@" + parts[1];
    }
}
