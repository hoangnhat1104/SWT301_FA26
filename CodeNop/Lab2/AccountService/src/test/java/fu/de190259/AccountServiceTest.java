package fu.de190259;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService service;

    @BeforeEach
    void setUp() {
        service = new AccountService();
    }

    // ---------- isValidEmail: valid ----------

    @ParameterizedTest(name = "Email hợp lệ: {0}")
    @ValueSource(strings = {
            "john@example.com",
            "alice.b@mail.co.uk",
            "carol_99@domain.io"
    })
    @DisplayName("isValidEmail trả về true với email đúng định dạng")
    void isValidEmail_ValidEmails_ReturnsTrue(String email) {
        // Act
        boolean result = service.isValidEmail(email);
        // Assert
        assertTrue(result);
    }

    // ---------- isValidEmail: invalid ----------

    @ParameterizedTest(name = "Email không hợp lệ: \"{0}\"")
    @CsvSource(value = {
            "bobmail.com",       // thiếu @
            "missing@dot",       // thiếu .domain
            "'@nodomain.com'",   // thiếu local part
            "' '",               // chỉ khoảng trắng
            "NULL"               // sẽ map về null
    }, nullValues = "NULL")
    @DisplayName("isValidEmail trả về false với email sai định dạng / null")
    void isValidEmail_InvalidEmails_ReturnsFalse(String email) {
        // Act & Assert
        assertFalse(service.isValidEmail(email));
    }

    // ---------- registerAccount: CSV File Source ----------

    @ParameterizedTest(name = "Row {index}: ({0},{1},{2}) → {3}")
    @CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
    @DisplayName("registerAccount với dữ liệu từ test-data.csv")
    void registerAccount_FromCsv(String username, String password,
                                  String email, boolean expected) {
        // Act
        boolean actual = service.registerAccount(username, password, email);
        // Assert
        assertEquals(expected, actual,
                () -> String.format("(%s,%s,%s) phải trả về %s",
                        username, password, email, expected));
    }

    // ---------- registerAccount: edge cases ----------

    @Test
    @DisplayName("registerAccount: password = 6 ký tự (biên dưới) → false")
    void registerAccount_PasswordExactly6_ReturnsFalse() {
        // Arrange
        String username = "bob";
        String password = "abcdef"; // đúng 6 ký tự
        String email = "bob@mail.com";
        // Act
        boolean actual = service.registerAccount(username, password, email);
        // Assert
        assertFalse(actual, "password phải > 6 ký tự");
    }

    @Test
    @DisplayName("registerAccount: password = 7 ký tự (biên trên) → true")
    void registerAccount_PasswordExactly7_ReturnsTrue() {
        // Arrange
        String username = "bob";
        String password = "abcdefg"; // đúng 7 ký tự
        String email = "bob@mail.com";
        // Act
        boolean actual = service.registerAccount(username, password, email);
        // Assert
        assertTrue(actual, "password > 6 ký tự phải hợp lệ");
    }

    @Test
    @DisplayName("registerAccount: tất cả tham số null → false")
    void registerAccount_AllNull_ReturnsFalse() {
        // Act & Assert
        assertFalse(service.registerAccount(null, null, null));
    }

    @Test
    @DisplayName("registerAccount: username null → false")
    void registerAccount_NullUsername_ReturnsFalse() {
        // Arrange
        String password = "validpassword";
        String email = "test@mail.com";
        // Act
        boolean actual = service.registerAccount(null, password, email);
        // Assert
        assertFalse(actual, "username null phải trả về false");
    }

    @Test
    @DisplayName("registerAccount: username rỗng → false")
    void registerAccount_EmptyUsername_ReturnsFalse() {
        // Arrange
        String username = "";
        String password = "validpassword";
        String email = "test@mail.com";
        // Act
        boolean actual = service.registerAccount(username, password, email);
        // Assert
        assertFalse(actual, "username rỗng phải trả về false");
    }

    @Test
    @DisplayName("registerAccount: password null → false")
    void registerAccount_NullPassword_ReturnsFalse() {
        // Arrange
        String username = "john";
        String email = "john@mail.com";
        // Act
        boolean actual = service.registerAccount(username, null, email);
        // Assert
        assertFalse(actual, "password null phải trả về false");
    }

    @Test
    @DisplayName("registerAccount: email null → false")
    void registerAccount_NullEmail_ReturnsFalse() {
        // Arrange
        String username = "john";
        String password = "validpassword";
        // Act
        boolean actual = service.registerAccount(username, password, null);
        // Assert
        assertFalse(actual, "email null phải trả về false");
    }

    @Test
    @DisplayName("registerAccount: tất cả hợp lệ → true")
    void registerAccount_AllValid_ReturnsTrue() {
        // Arrange
        String username = "john123";
        String password = "securepass";
        String email = "john123@example.com";
        // Act
        boolean actual = service.registerAccount(username, password, email);
        // Assert
        assertTrue(actual, "tất cả hợp lệ phải trả về true");
    }
}
