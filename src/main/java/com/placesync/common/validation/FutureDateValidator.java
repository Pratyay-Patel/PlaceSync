package com.placesync.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.OffsetDateTime;

public class FutureDateValidator implements ConstraintValidator<FutureDate, OffsetDateTime> {

    @Override
    public boolean isValid(OffsetDateTime value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return value.isAfter(OffsetDateTime.now());
    }
}
