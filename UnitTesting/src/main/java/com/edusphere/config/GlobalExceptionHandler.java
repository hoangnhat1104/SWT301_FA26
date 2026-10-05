package com.edusphere.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Xử lý lỗi toàn cục — không để lộ stack trace ra UI.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 403 — Không có quyền truy cập.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(Model model) {
        model.addAttribute("errorMessage", "Bạn không có quyền truy cập trang này.");
        return "error/403";
    }

    /**
     * Lỗi chung — không hiển thị stack trace.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, Model model) {
        // Log lỗi thực tế (trong production dùng Logger)
        System.err.println("Unhandled exception: " + ex.getMessage());
        model.addAttribute("errorMessage", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
        return "error/403";
    }
}
