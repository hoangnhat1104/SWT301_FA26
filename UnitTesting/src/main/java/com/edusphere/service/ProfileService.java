package com.edusphere.service;

import com.edusphere.dto.ChangePasswordRequest;
import com.edusphere.dto.UpdateProfileRequest;
import com.edusphere.entity.Users;
import com.edusphere.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Service xử lý nghiệp vụ hồ sơ người dùng.
 */
@Service
public class ProfileService {

    // Định dạng file ảnh được phép upload
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024; // 2MB

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lấy thông tin user để hiển thị profile.
     */
    @Transactional(readOnly = true)
    public Users getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + userId));
    }

    /**
     * Cập nhật thông tin hồ sơ (fullName, phone).
     */
    @Transactional
    public void updateProfile(Integer userId, UpdateProfileRequest request) {
        Users user = getUserById(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    /**
     * Đổi mật khẩu cho user đã đăng nhập.
     *
     * Xử lý đặc biệt: user đăng nhập bằng Google có passwordHash = NULL.
     *
     * @throws IllegalArgumentException nếu current password sai hoặc password mới không khớp
     */
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        Users user = getUserById(userId);

        // Trường hợp Google-only user (passwordHash = NULL)
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException(
                "Tài khoản của bạn đăng nhập bằng Google và chưa có mật khẩu. " +
                "Vui lòng liên hệ hỗ trợ để thiết lập mật khẩu."
            );
        }

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng.");
        }

        // Kiểm tra confirm password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(new Date());
        userRepository.save(user);
    }

    /**
     * Upload avatar mới cho user.
     * - Validate file type và size
     * - Tạo tên file an toàn (UUID, không tin tưởng tên gốc)
     * - Lưu file vào thư mục uploads
     * - Cập nhật avatar_url trong DB
     *
     * @param uploadDir đường dẫn thư mục lưu ảnh (từ application.properties)
     * @param contextPath context path của ứng dụng (để tạo URL)
     */
    @Transactional
    public String uploadAvatar(Integer userId, MultipartFile file,
                               String uploadDir, String contextPath) throws IOException {
        // Validate file không rỗng
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn file ảnh.");
        }

        // Validate file size
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new IllegalArgumentException("File ảnh không được vượt quá 2MB.");
        }

        // Validate file type (MIME type)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh: JPG, PNG, GIF, WEBP.");
        }

        // Lấy extension từ tên file gốc (chỉ lấy extension, không tin tưởng tên gốc)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "avatar.jpg");
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }

        // Tạo tên file an toàn bằng UUID
        String safeFileName = "avatar_" + userId + "_" + UUID.randomUUID() + extension;

        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir);
        Files.createDirectories(uploadPath);

        // Lưu file
        Path filePath = uploadPath.resolve(safeFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Tạo URL để lưu vào DB
        // URL pattern: /EduSphere/uploads/avatars/filename.jpg
        String avatarUrl = contextPath + "/uploads/avatars/" + safeFileName;

        // Cập nhật DB
        Users user = getUserById(userId);
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(new Date());
        userRepository.save(user);

        return avatarUrl;
    }
}
