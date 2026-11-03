package io.github.foundry4j.quarkforge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.github.foundry4j.quarkforge.validation.annotation.BankCard;

/// @see BankCard
public class BankCardValidator implements ConstraintValidator<BankCard, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String number = value.toString();
        if (!number.matches("^\\d{12,19}$")) {
            return false;
        }
        return luhnCheck(number);
    }

    /// Luhn 算法：从右往左，偶数位乘 2 后各位求和，总和是 10 的倍数即合法。
    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
