package com.placesync.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CgpaValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCgpa {
    String message() default "CGPA must be between 0.0 and 10.0";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
