package com.edusphere.controller;

import com.edusphere.dto.UpdateProfileRequest;
import com.edusphere.entity.Users;
import com.edusphere.security.CustomUserDetails;
import com.edusphere.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller xử lý: xem profile, sửa profile, upload avatar.
 * Tất cả endpoint đều yêu cầu đăng nhập (được Spring Security đảm bảo).
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // ===================== VIEW PROFILE =====================

    @GetMapping
    public String showProfile(@AuthenticationPrincipal CustomUserDetails currentUser,
                              Model model) {
        Users user = profileService.getUserById(currentUser.getUserId());
        model.addAttribute("user", user);
        return "profile/profile";
    }

    // ===================== EDIT PROFILE =====================

    @GetMapping("/edit")
    public String showEditProfilePage(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      Model model) {
        Users user = profileService.getUserById(currentUser.getUserId());
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName(user.getFullName());
        request.setPhone(user.getPhone());
        model.addAttribute("updateProfileRequest", request);
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String processEditProfile(
            @Valid @ModelAttribute("updateProfileRequest") UpdateProfileRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", profileService.getUserById(currentUser.getUserId()));
            return "profile/edit";
        }

        try {
            profileService.updateProfile(currentUser.getUserId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
            return "redirect:/profile";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "profile/edit";
        }
    }

    // ===================== CHANGE AVATAR =====================

    @PostMapping("/avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               @AuthenticationPrincipal CustomUserDetails currentUser,
                               HttpServletRequest httpRequest,
                               RedirectAttributes redirectAttributes) {
        try {
            String contextPath = httpRequest.getContextPath();
            profileService.uploadAvatar(currentUser.getUserId(), file, uploadDir, contextPath);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật ảnh đại diện thành công!");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Upload ảnh thất bại. Vui lòng thử lại.");
        }

        return "redirect:/profile";
    }
}
