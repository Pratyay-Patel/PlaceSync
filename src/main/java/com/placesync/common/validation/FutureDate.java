package com.placesync.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FutureDateValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureDate {
    String message() default "Date must be strictly in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
