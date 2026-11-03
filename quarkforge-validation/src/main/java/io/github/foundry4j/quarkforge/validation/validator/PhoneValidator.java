package io.github.foundry4j.quarkforge.validation.validator;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.github.foundry4j.quarkforge.validation.annotation.Phone;

/// @see Phone
public class PhoneValidator implements ConstraintValidator<Phone, CharSequence> {

    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || MOBILE.matcher(value).matches();
    }
}
