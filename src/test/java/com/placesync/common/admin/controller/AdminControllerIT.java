package com.placesync.common.admin.controller;

import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_USERS_URL = "/api/v1/admin/users";

    @Test
    void searchUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL)
                        .header("Authorization", bearerHeader(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void searchUsers_asStudent_returns403() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL)
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchUsers_asRecruiter_returns403() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL)
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserById_asAdmin_returns200() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL + "/" + studentUser.getId())
                        .header("Authorization", bearerHeader(adminUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@itest.com"));
    }

    @Test
    void getUserById_asStudent_returns403() throws Exception {
        mockMvc.perform(get(ADMIN_USERS_URL + "/" + studentUser.getId())
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isForbidden());
    }
}
