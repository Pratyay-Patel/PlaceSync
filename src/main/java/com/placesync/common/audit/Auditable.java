package com.placesync.common.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    AuditAction action();
    String entityType();
    /** Parameter index of the entity UUID when the method returns void. -1 = use getId() on return value. */
    int entityIdParamIndex() default -1;
}
