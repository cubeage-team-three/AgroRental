package com.agrorental.auth;

import com.agrorental.auth.service.PasswordPolicy;
import com.agrorental.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Shared password policy: 8-64 characters, a letter and a digit, not only whitespace")
class PasswordPolicyTest {

    @ParameterizedTest(name = "accepts \"{0}\"")
    @ValueSource(strings = {"abcdefg1", "Passw0rd", "Correct-Horse-1", "  spaced 1 pass  ", "पासवर्ड1234", "1234567a"})
    void acceptsCompliantPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password)).isTrue();
        assertThat(PasswordPolicy.violation(password)).isNull();
        assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"abcdef1", "short1", "abcdefgh", "ABCDEFGHIJ", "12345678", "1234567890123", "        ",
            "\t\t\t\t\t\t\t\t", "        1"})
    void rejectsNonCompliantPasswords(String password) {
        assertThat(PasswordPolicy.isValid(password)).isFalse();
        assertThat(PasswordPolicy.violation(password)).isEqualTo(PasswordPolicy.REQUIREMENTS_MESSAGE);
        assertThatThrownBy(() -> PasswordPolicy.validate(password))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(PasswordPolicy.REQUIREMENTS_MESSAGE);
    }

    @Test
    @DisplayName("Boundaries: exactly 8 and exactly 64 characters are accepted, 7 and 65 are not")
    void lengthBoundaries() {
        assertThat(PasswordPolicy.isValid("a" + "1".repeat(7))).isTrue();
        assertThat(PasswordPolicy.isValid("a" + "1".repeat(63))).isTrue();
        assertThat(PasswordPolicy.isValid("a" + "1".repeat(6))).isFalse();
        assertThat(PasswordPolicy.isValid("a" + "1".repeat(64))).isFalse();
    }

    @Test
    @DisplayName("Passwords BCrypt cannot hash (more than 72 UTF-8 bytes) are refused instead of failing at hashing time")
    void refusesPasswordsLongerThanBcryptInput() {
        String multiByte = "अ".repeat(30) + "1"; // 31 characters, 91 UTF-8 bytes
        assertThat(multiByte.codePointCount(0, multiByte.length())).isLessThanOrEqualTo(PasswordPolicy.MAX_LENGTH);
        assertThat(PasswordPolicy.violation(multiByte)).isEqualTo(PasswordPolicy.TOO_LONG_MESSAGE);
    }
}
