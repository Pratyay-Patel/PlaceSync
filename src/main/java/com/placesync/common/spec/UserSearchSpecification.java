package com.placesync.common.spec;

import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import org.springframework.data.jpa.domain.Specification;

public class UserSearchSpecification {

    private UserSearchSpecification() {}

    public static Specification<User> withFilters(String email, UserRole role, Boolean isActive) {
        return Specification.where(notDeleted())
                .and(emailContains(email))
                .and(roleMatches(role))
                .and(activeMatches(isActive));
    }

    private static Specification<User> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<User> emailContains(String email) {
        if (email == null || email.isBlank()) return null;
        String pattern = "%" + email.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), pattern);
    }

    private static Specification<User> roleMatches(UserRole role) {
        if (role == null) return null;
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    private static Specification<User> activeMatches(Boolean isActive) {
        if (isActive == null) return null;
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }
}
