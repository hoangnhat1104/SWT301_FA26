package com.edusphere.controller.auth;

import com.edusphere.dto.RegisterRequest;
import com.edusphere.service.auth.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý: đăng ký (flow OTP), xác minh OTP, đăng nhập.
 */
@Controller
public class AuthController {

    private final AuthService authService;

    // Key lưu email vào session khi chờ OTP
    private static final String SESSION_PENDING_EMAIL = "pendingRegisterEmail";

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ===================== LOGIN =====================

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String oauth2Error,
                                Model model) {
        if (error != null) {
            String message = switch (error) {
                case "notVerified"    -> "Tài khoản chưa xác minh OTP. Vui lòng kiểm tra email.";
                case "locked"         -> "Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.";
                case "badCredentials" -> "Email hoặc mật khẩu không đúng.";
                default               -> "Đăng nhập thất bại. Vui lòng thử lại.";
            };
            model.addAttribute("errorMessage", message);
        }
        if (logout != null)      model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        if (oauth2Error != null)  model.addAttribute("errorMessage", "Đăng nhập Google thất bại. Vui lòng thử lại.");

        return "auth/login";
    }

    // ===================== REGISTER =====================

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                  BindingResult bindingResult,
                                  Model model,
                                  HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            // Tạo user PENDING + gửi OTP → trả về email
            String email = authService.initiateRegister(request);

            // Lưu email vào session để trang OTP dùng
            session.setAttribute(SESSION_PENDING_EMAIL, email);

            // Chuyển sang trang nhập OTP
            return "redirect:/verify-otp";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }

    // ===================== VERIFY OTP =====================

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(HttpSession session, Model model) {
        String email = (String) session.getAttribute(SESSION_PENDING_EMAIL);

        // Nếu không có email trong session → redirect về đăng ký
        if (email == null || email.isBlank()) {
            return "redirect:/register";
        }

        // Che email: dangcong***@gmail.com
        model.addAttribute("maskedEmail", maskEmail(email));
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(@RequestParam("otp") String otp,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute(SESSION_PENDING_EMAIL);

        if (email == null || email.isBlank()) {
            return "redirect:/register";
        }

        try {
            authService.verifyOtp(email, otp);

            // Xác thực thành công → xóa session, chuyển về login
            session.removeAttribute(SESSION_PENDING_EMAIL);
            redirectAttributes.addFlashAttribute("successMessage",
                "🎉 Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("maskedEmail", maskEmail(email));
            return "auth/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute(SESSION_PENDING_EMAIL);

        if (email == null || email.isBlank()) {
            return "redirect:/register";
        }

        try {
            authService.resendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi lại mã OTP mới. Kiểm tra email của bạn.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/verify-otp";
    }

    // ===================== HELPER =====================

    /**
     * Che một phần email để hiển thị an toàn.
     * Ví dụ: dangconghoangnhat@gmail.com → dan***@gmail.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];
        int showChars = Math.min(3, local.length());
        return local.substring(0, showChars) + "***@" + domain;
    }
}
