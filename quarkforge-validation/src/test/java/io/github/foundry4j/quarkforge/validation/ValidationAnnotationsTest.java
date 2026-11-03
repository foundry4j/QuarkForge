package io.github.foundry4j.quarkforge.validation;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.foundry4j.quarkforge.validation.annotation.BankCard;
import io.github.foundry4j.quarkforge.validation.annotation.EnumValid;
import io.github.foundry4j.quarkforge.validation.annotation.IdCard;
import io.github.foundry4j.quarkforge.validation.annotation.PasswordStrength;
import io.github.foundry4j.quarkforge.validation.annotation.Phone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationAnnotationsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void phone() {
        assertValid(new PhoneHolder("13800138000"));
        assertValid(new PhoneHolder(null));            // null 合法，必填交给 @NotNull
        assertInvalid(new PhoneHolder("12345678901")); // 非 1[3-9] 号段
        assertInvalid(new PhoneHolder("1380013800"));  // 位数不足
        assertInvalid(new PhoneHolder(""));            // 空串非法
    }

    @Test
    void idCard() {
        // 加权因子校验通过的真实格式号码（校验位为 X）
        assertValid(new IdCardHolder("11010519491231002X"));
        assertValid(new IdCardHolder("11010519491231002x")); // 小写 x 兼容
        assertValid(new IdCardHolder(null));
        assertInvalid(new IdCardHolder("110105194912310026")); // 校验位错误
        assertInvalid(new IdCardHolder("11010519491332002X")); // 月份 13
        assertInvalid(new IdCardHolder("1101051949123100260")); // 19 位
    }

    @Test
    void bankCard() {
        assertValid(new BankCardHolder("4242424242424242")); // Luhn 合法
        assertValid(new BankCardHolder(null));
        assertInvalid(new BankCardHolder("4242424242424241")); // Luhn 不合法
        assertInvalid(new BankCardHolder("79927398713"));      // 不足 12 位
        assertInvalid(new BankCardHolder("4242-4242"));        // 含非数字
    }

    @Test
    void passwordStrength() {
        assertValid(new PasswordHolder("Abcd1234"));
        assertInvalid(new PasswordHolder("abcdabcd"));  // 缺数字
        assertInvalid(new PasswordHolder("12345678"));  // 缺字母
        assertInvalid(new PasswordHolder("Ab1"));       // 太短

        StrongPasswordHolder good = new StrongPasswordHolder("Passw0rd!");
        assertValid(good);
        StrongPasswordHolder weak = new StrongPasswordHolder("Passw0rd");
        assertInvalid(weak); // 缺特殊字符
    }

    @Test
    void enumValid() {
        assertValid(new StatusHolder("PAID"));
        assertValid(new StatusHolder(null));
        assertInvalid(new StatusHolder("UNKNOWN"));
        assertInvalid(new StatusHolder("paid")); // 默认大小写敏感

        assertValid(new IgnoreCaseStatusHolder("paid"));
    }

    private void assertValid(Object bean) {
        Set<? extends ConstraintViolation<?>> violations = validator.validate(bean);
        assertTrue(violations.isEmpty(), () -> "期望校验通过: " + violations);
    }

    private void assertInvalid(Object bean) {
        assertEquals(1, validator.validate(bean).size(), "期望恰好一个校验错误");
    }

    enum Status {
        PAID, UNPAID, CANCELLED
    }

    static class PhoneHolder {
        @Phone
        final String value;

        PhoneHolder(String value) {
            this.value = value;
        }
    }

    static class IdCardHolder {
        @IdCard
        final String value;

        IdCardHolder(String value) {
            this.value = value;
        }
    }

    static class BankCardHolder {
        @BankCard
        final String value;

        BankCardHolder(String value) {
            this.value = value;
        }
    }

    static class PasswordHolder {
        @PasswordStrength
        final String value;

        PasswordHolder(String value) {
            this.value = value;
        }
    }

    static class StrongPasswordHolder {
        @PasswordStrength(min = 8, max = 32, requireSpecial = true)
        final String value;

        StrongPasswordHolder(String value) {
            this.value = value;
        }
    }

    static class StatusHolder {
        @EnumValid(Status.class)
        final String value;

        StatusHolder(String value) {
            this.value = value;
        }
    }

    static class IgnoreCaseStatusHolder {
        @EnumValid(value = Status.class, ignoreCase = true)
        final String value;

        IgnoreCaseStatusHolder(String value) {
            this.value = value;
        }
    }
}
