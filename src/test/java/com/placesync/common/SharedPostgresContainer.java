package com.placesync.common;

import org.testcontainers.containers.PostgreSQLContainer;

public final class SharedPostgresContainer {

    public static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    private SharedPostgresContainer() {}
}
