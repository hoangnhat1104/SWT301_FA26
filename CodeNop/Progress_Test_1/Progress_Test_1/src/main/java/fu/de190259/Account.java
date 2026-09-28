package fu.de190259;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể tài khoản.
 * Getter public; các hàm thay đổi trạng thái để package-private
 * (chỉ AccountService được gọi).
 */
public class Account {

    private final String username;
    private final String email;
    private final LocalDate dateOfBirth;
    private final String phone;
    private final String salt;

    /** Phần tử cuối = mật khẩu hiện tại. */
    private final List<String> passwordHistory = new ArrayList<>();

    private AccountStatus status = AccountStatus.ACTIVE;
    private int failedAttempts;
    private boolean locked;

    Account(String username, String email, LocalDate dateOfBirth, String phone,
            String salt, String passwordHash) {
        this.username = username;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.salt = salt;
        this.passwordHistory.add(passwordHash);
    }

    // ── Getters public ──────────────────────────────────────────────────────

    public String getUsername()       { return username; }
    public String getEmail()          { return email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPhone()          { return phone; }
    public String getSalt()           { return salt; }
    public AccountStatus getStatus()  { return status; }
    public int getFailedAttempts()    { return failedAttempts; }
    public boolean isLocked()         { return locked; }

    /** Trả hash mật khẩu hiện tại (phần tử cuối của lịch sử). */
    public String getCurrentPasswordHash() {
        return passwordHistory.get(passwordHistory.size() - 1);
    }

    /** Trả bản sao để không lộ list nội bộ. */
    public List<String> getPasswordHistory() {
        return List.copyOf(passwordHistory);
    }

    // ── Package-private: thay đổi trạng thái ────────────────────────────────

    void setStatus(AccountStatus status)  { this.status = status; }
    void incrementFailedAttempts()        { failedAttempts++; }
    void resetFailedAttempts()            { failedAttempts = 0; }
    void lock()                           { locked = true; }

    void unlock() {
        locked = false;
        failedAttempts = 0;
    }

    /**
     * Thêm hash mới vào lịch sử, giữ tối đa maxHistory phần tử
     * (bỏ phần tử cũ nhất khi vượt quá).
     */
    void changePasswordHash(String newHash, int maxHistory) {
        passwordHistory.add(newHash);
        while (passwordHistory.size() > maxHistory) {
            passwordHistory.remove(0);
        }
    }
}
