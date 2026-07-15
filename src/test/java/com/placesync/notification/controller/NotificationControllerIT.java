package com.placesync.notification.controller;

import com.placesync.common.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerIT extends AbstractIntegrationTest {

    private static final String NOTIF_URL = "/api/v1/notifications";

    @Test
    void getNotifications_noToken_returns401() throws Exception {
        mockMvc.perform(get(NOTIF_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotifications_asStudent_returns200() throws Exception {
        mockMvc.perform(get(NOTIF_URL)
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void countUnread_asStudent_returns200() throws Exception {
        mockMvc.perform(get(NOTIF_URL + "/unread-count")
                        .header("Authorization", bearerHeader(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    void getNotifications_asRecruiter_returns200() throws Exception {
        mockMvc.perform(get(NOTIF_URL)
                        .header("Authorization", bearerHeader(recruiterUser)))
                .andExpect(status().isOk());
    }
}
