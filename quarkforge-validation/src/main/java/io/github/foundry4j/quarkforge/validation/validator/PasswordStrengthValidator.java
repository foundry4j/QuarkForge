package io.github.foundry4j.quarkforge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.github.foundry4j.quarkforge.validation.annotation.PasswordStrength;

/// @see PasswordStrength
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, CharSequence> {

    private static final String SPECIAL_CHARS = "~!@#$%^&*()-_=+[]{}|;:,.<>?";

    private int min;
    private int max;
    private boolean requireDigit;
    private boolean requireLetter;
    private boolean requireSpecial;

    @Override
    public void initialize(PasswordStrength constraint) {
        this.min = constraint.min();
        this.max = constraint.max();
        this.requireDigit = constraint.requireDigit();
        this.requireLetter = constraint.requireLetter();
        this.requireSpecial = constraint.requireSpecial();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String password = value.toString();
        int length = password.length();
        if (length < min || length > max) {
            return false;
        }
        boolean hasDigit = false;
        boolean hasLetter = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (SPECIAL_CHARS.indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }
        return (!requireDigit || hasDigit)
                && (!requireLetter || hasLetter)
                && (!requireSpecial || hasSpecial);
    }
}
