package com.placesync.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class CgpaValidator implements ConstraintValidator<ValidCgpa, BigDecimal> {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.TEN;

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.compareTo(MIN) >= 0 && value.compareTo(MAX) <= 0;
    }
}
