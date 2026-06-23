package com.placesync.common.audit;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    SOFT_DELETE,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    PASSWORD_CHANGE,
    PASSWORD_RESET,
    EMAIL_VERIFIED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED
}
