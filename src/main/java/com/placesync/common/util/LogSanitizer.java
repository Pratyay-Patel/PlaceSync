package com.placesync.common.util;

public final class LogSanitizer {

    private LogSanitizer() {}

    public static String sanitize(Object value) {
        return value == null ? "null" : value.toString().replaceAll("[\r\n\t]", "_");
    }
}
