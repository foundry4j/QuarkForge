package io.github.foundry4j.quarkforge.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import io.github.foundry4j.quarkforge.validation.annotation.IdCard;

/// @see IdCard
public class IdCardValidator implements ConstraintValidator<IdCard, CharSequence> {

    /// 加权因子（GB 11643-1999）。
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /// 校验码映射表：sum % 11 → 校验位字符。
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        String id = value.toString().toUpperCase();
        if (!id.matches("^\\d{17}[0-9X]$")) {
            return false;
        }
        return isValidBirthday(id.substring(6, 14)) && checkCode(id);
    }

    /// 校验位算法：前 17 位按权重求和取模 11，对照映射表。
    private boolean checkCode(String id) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (id.charAt(i) - '0') * WEIGHTS[i];
        }
        return CHECK_CODES[sum % 11] == id.charAt(17);
    }

    /// 粗略校验出生日期：年月范围 + 当月天数。
    private boolean isValidBirthday(String yyyymmdd) {
        int year = Integer.parseInt(yyyymmdd.substring(0, 4));
        int month = Integer.parseInt(yyyymmdd.substring(4, 6));
        int day = Integer.parseInt(yyyymmdd.substring(6, 8));
        if (year < 1900 || year > 2100 || month < 1 || month > 12) {
            return false;
        }
        int[] daysOfMonth = {31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        return day >= 1 && day <= daysOfMonth[month - 1];
    }

    private boolean isLeapYear(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
