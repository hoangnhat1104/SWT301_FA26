package fu.de190259;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccountService")
class AccountServiceTest {

    // ── Hằng dùng chung ────────────────────────────────────────────────────
    static final String USER  = "alice_01";
    static final String EMAIL = "alice@example.com";
    static final String PASS  = "Secret@123";
    static final String WRONG = "Wrong@123";
    static final LocalDate DOB       = LocalDate.of(2000, 1, 15);
    static final String PHONE        = "0912345678";
    static final LocalDate CHILD_DOB = LocalDate.now().minusYears(10);

    AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService(); // mỗi test một service mới -> độc lập
    }

    /** Đăng ký tài khoản mẫu thành công. */
    void registerDefault() {
        assertEquals(ResultCode.SUCCESS, service.register(USER, EMAIL, PASS, PASS, DOB, PHONE));
    }

    // ======================================================================
    @Nested
    @DisplayName("register()")
    class Register {

        // ── Thành công ────────────────────────────────────────────────────

        @Test
        void register_ValidData_CreatesActiveAccountWithHashedPassword() {
            ResultCode result = service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);

            assertEquals(ResultCode.SUCCESS, result);
            // trạng thái tài khoản
            Account acc = service.findByUsername(USER).orElseThrow();
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            // mật khẩu phải được băm
            assertNotEquals(PASS, acc.getCurrentPasswordHash());
            assertEquals(64, acc.getCurrentPasswordHash().length());
            assertEquals(1, acc.getPasswordHistory().size());
        }

        @Test
        void register_UpperCaseEmail_StoredAsLowerCase() {
            service.register(USER, "Alice@Example.COM", PASS, PASS, DOB, PHONE);
            Account acc = service.findByUsername(USER).orElseThrow();
            assertEquals("alice@example.com", acc.getEmail());
        }

        @Test
        void register_TwoAccountsSamePassword_HaveDifferentSaltAndHash() {
            registerDefault();
            service.register("bob_02", "bob@example.com", PASS, PASS, DOB, null);
            Account alice = service.findByUsername(USER).orElseThrow();
            Account bob   = service.findByUsername("bob_02").orElseThrow();
            assertNotEquals(alice.getSalt(), bob.getSalt());
            assertNotEquals(alice.getCurrentPasswordHash(), bob.getCurrentPasswordHash());
        }

        // ── Input không hợp lệ (MethodSource) ────────────────────────────

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("fu.de190259.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInput_ReturnsExpectedCode(String desc, String username, String email,
                                                       String password, String confirm,
                                                       LocalDate dob, String phone,
                                                       ResultCode expected) {
            ResultCode result = service.register(username, email, password, confirm, dob, phone);

            assertEquals(expected, result);
            // không được tạo tài khoản khi thất bại
            assertTrue(service.findByUsername(username).isEmpty(), "Không được tạo tài khoản khi lỗi");
        }

        // ── NullAndEmptySource ────────────────────────────────────────────

        @ParameterizedTest(name = "[{index}] username = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_UsernameNullEmptyBlank_ReturnsInvalidInput(String username) {
            assertEquals(ResultCode.INVALID_INPUT,
                    service.register(username, EMAIL, PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] email = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_EmailNullEmptyBlank_ReturnsInvalidInput(String email) {
            assertEquals(ResultCode.INVALID_INPUT,
                    service.register(USER, email, PASS, PASS, DOB, PHONE));
        }

        @ParameterizedTest(name = "[{index}] password = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void register_PasswordNullEmptyBlank_ReturnsInvalidInput(String password) {
            assertEquals(ResultCode.INVALID_INPUT,
                    service.register(USER, EMAIL, password, PASS, DOB, PHONE));
            assertEquals(ResultCode.INVALID_INPUT,
                    service.register(USER, EMAIL, PASS, password, DOB, PHONE));
        }

        // ── Phone tùy chọn ────────────────────────────────────────────────

        @ParameterizedTest(name = "[{index}] phone = \"{0}\" được chấp nhận")
        @NullAndEmptySource
        void register_PhoneNullOrEmpty_Success(String phone) {
            assertEquals(ResultCode.SUCCESS,
                    service.register(USER, EMAIL, PASS, PASS, DOB, phone));
        }

        // ── Trùng username / email (không phân biệt hoa/thường) ──────────

        @ParameterizedTest(name = "[{index}] trùng username \"{0}\"")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void register_DuplicateUsernameIgnoreCase_ReturnsDuplicateUsername(String username) {
            registerDefault();
            assertEquals(ResultCode.DUPLICATE_USERNAME,
                    service.register(username, "other@example.com", PASS, PASS, DOB, null));
        }

        @ParameterizedTest(name = "[{index}] trùng email \"{0}\"")
        @ValueSource(strings = {"alice@example.com", "ALICE@EXAMPLE.COM", "Alice@Example.Com"})
        void register_DuplicateEmailIgnoreCase_ReturnsDuplicateEmail(String email) {
            registerDefault();
            assertEquals(ResultCode.DUPLICATE_EMAIL,
                    service.register("bob_02", email, PASS, PASS, DOB, null));
            assertTrue(service.findByUsername("bob_02").isEmpty());
        }

        // ── Biên tuổi (ngày tương đối so với hôm nay) ────────────────────

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18,  0, SUCCESS",      // đúng 18 tuổi hôm nay
                "18,  1, UNDERAGE",     // 18 tuổi trừ 1 ngày
                "18, -1, SUCCESS",      // 18 tuổi + 1 ngày
                "0,   0, UNDERAGE",     // sinh hôm nay
                "0,   1, INVALID_INPUT" // ngày sinh ở tương lai
        })
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate dob = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            assertEquals(expected, service.register(USER, EMAIL, PASS, PASS, dob, null));
        }

        // ── Thứ tự ưu tiên ───────────────────────────────────────────────

        @Test
        void register_DuplicateUsernameButInvalidEmail_ReturnsInvalidEmailFirst() {
            // BR-REG-04 (email) kiểm tra trước BR-REG-03 (trùng username)
            registerDefault();
            assertEquals(ResultCode.INVALID_EMAIL,
                    service.register(USER, "bad-email", PASS, PASS, DOB, null));
        }
    }

    // ======================================================================
    // Provider dùng chung cho Register.register_InvalidInput_ReturnsExpectedCode
    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // từng quy tắc riêng lẻ
                Arguments.of("dob null",          USER,    EMAIL,           PASS,     PASS,     null,      PHONE,        ResultCode.INVALID_INPUT),
                Arguments.of("username sai",      "1alice", EMAIL,          PASS,     PASS,     DOB,       PHONE,        ResultCode.INVALID_USERNAME),
                Arguments.of("email sai",         USER,    "alice@example", PASS,     PASS,     DOB,       PHONE,        ResultCode.INVALID_EMAIL),
                Arguments.of("mật khẩu yếu",      USER,    EMAIL,           "password","password", DOB,   PHONE,        ResultCode.WEAK_PASSWORD),
                Arguments.of("confirm lệch",      USER,    EMAIL,           PASS,     "Secret@124", DOB,   PHONE,        ResultCode.PASSWORD_MISMATCH),
                Arguments.of("chưa đủ tuổi",      USER,    EMAIL,           PASS,     PASS,     CHILD_DOB, PHONE,        ResultCode.UNDERAGE),
                Arguments.of("phone sai đầu số",  USER,    EMAIL,           PASS,     PASS,     DOB,       "0112345678", ResultCode.INVALID_PHONE),
                Arguments.of("phone blank",       USER,    EMAIL,           PASS,     PASS,     DOB,       "   ",        ResultCode.INVALID_PHONE),
                // thứ tự ưu tiên khi vi phạm nhiều quy tắc
                Arguments.of("username sai + email sai -> INVALID_USERNAME", "1alice", "bad", PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("email sai + mk yếu -> INVALID_EMAIL",          USER, "bad", "weak", "weak", DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("mk yếu + confirm lệch -> WEAK_PASSWORD",       USER, EMAIL, "weak", "other", DOB, PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("confirm lệch + chưa đủ tuổi -> PASSWORD_MISMATCH", USER, EMAIL, PASS, "x", CHILD_DOB, PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("chưa đủ tuổi + phone sai -> UNDERAGE",         USER, EMAIL, PASS, PASS, CHILD_DOB, "123", ResultCode.UNDERAGE),
                Arguments.of("thiếu email + username sai -> INVALID_INPUT",  "1alice", "", PASS, PASS, DOB, PHONE, ResultCode.INVALID_INPUT)
        );
    }
}
