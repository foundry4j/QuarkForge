package io.github.foundry4j.quarkforge.validation.validator;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.github.foundry4j.quarkforge.validation.annotation.EnumValid;

/// @see EnumValid
public class EnumValidValidator implements ConstraintValidator<EnumValid, CharSequence> {

    private Set<String> allowedValues;
    private boolean ignoreCase;

    @Override
    public void initialize(EnumValid constraint) {
        this.ignoreCase = constraint.ignoreCase();
        this.allowedValues = Arrays.stream(constraint.value().getEnumConstants())
                .map(Enum::name)
                .map(name -> ignoreCase ? name.toLowerCase(Locale.ROOT) : name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String candidate = value.toString();
        return allowedValues.contains(ignoreCase ? candidate.toLowerCase(Locale.ROOT) : candidate);
    }
}
