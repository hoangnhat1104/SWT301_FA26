package fu.de190259;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Nghiệp vụ quản lý tài khoản, lưu trong bộ nhớ (HashMap).
 * Các phương thức bonus (changePassword, requestPasswordReset, resetPassword) giữ stub.
 */
public class AccountService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    private final Map<String, Account> accountsByUsername = new HashMap<>(); // key: username lowercase
    private final Map<String, String>  usernameByEmail   = new HashMap<>(); // email lowercase -> username key
    private final Map<String, String>  usernameByToken   = new HashMap<>(); // token -> username key
    private final Map<String, String>  tokenByUsername   = new HashMap<>(); // username key -> token hiện hành

    public AccountService() {}

    // ================= Đăng ký (BR-REG-01..10) =================
    /**
     * Thứ tự kiểm tra bắt buộc:
     * REG-01 → 02 → 04 → 06 → 07 → 08 → 09 → 03 → 05 → 10
     */
    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // BR-REG-01: bắt buộc không rỗng, ngày sinh không null và không ở tương lai
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }

        // BR-REG-02: định dạng username
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // BR-REG-04: định dạng email
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // BR-REG-06: mật khẩu đủ mạnh
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // BR-REG-07: xác nhận mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // BR-REG-08: đủ tuổi tối thiểu
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }

        // BR-REG-09: phone tùy chọn — null hoặc "" bỏ qua; "   " (blank) là INVALID_PHONE
        if (phone != null && !phone.isEmpty() && !AccountValidator.isValidPhone(phone)) {
            return ResultCode.INVALID_PHONE;
        }

        String userKey  = key(username);
        String emailKey = key(email);

        // BR-REG-03: trùng username (không phân biệt hoa/thường)
        if (accountsByUsername.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // BR-REG-05: trùng email (không phân biệt hoa/thường)
        if (usernameByEmail.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // BR-REG-10: tạo tài khoản, lưu hash, email lưu lowercase
        String salt    = PasswordHasher.generateSalt();
        Account account = new Account(username, emailKey, dateOfBirth, phone,
                salt, PasswordHasher.hash(salt, password));
        accountsByUsername.put(userKey, account);
        usernameByEmail.put(emailKey, userKey);
        return ResultCode.SUCCESS;
    }

    // ================= Đăng nhập =================
    public ResultCode login(String username, String password) {
        throw new UnsupportedOperationException("TODO");
    }

    // ================= Quản trị & truy vấn =================
    public ResultCode disableAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode unlockAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public Optional<Account> findByUsername(String username) {
        if (isBlank(username)) return Optional.empty();
        return Optional.ofNullable(accountsByUsername.get(key(username)));
    }

    public boolean isLocked(String username) {
        return findByUsername(username).map(Account::isLocked).orElse(false);
    }

    // ================= BONUS stubs =================
    public ResultCode changePassword(String username, String oldPassword,
                                     String newPassword, String confirmPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    public TokenResult requestPasswordReset(String email) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode resetPassword(String token, String newPassword, String confirmPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    // ================= Helpers =================
    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
