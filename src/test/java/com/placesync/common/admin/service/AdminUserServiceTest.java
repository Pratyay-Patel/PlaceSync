package com.placesync.common.admin.service;

import com.placesync.common.admin.dto.UpdateUserStatusRequest;
import com.placesync.common.admin.dto.UserSummaryResponse;
import com.placesync.common.admin.mapper.AdminUserMapper;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.util.PagedResponse;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AdminUserMapper adminUserMapper;

    @InjectMocks AdminUserService adminUserService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void searchUsers_returnsPagedResults() {
        User user = User.builder().build();
        UserSummaryResponse response = UserSummaryResponse.builder().build();
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(adminUserMapper.toSummaryResponse(user)).thenReturn(response);

        PagedResponse<UserSummaryResponse> result = adminUserService.searchUsers(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getUserById_existingUser_returnsMappedResponse() {
        User user = User.builder().build();
        UserSummaryResponse response = UserSummaryResponse.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(adminUserMapper.toSummaryResponse(user)).thenReturn(response);

        UserSummaryResponse result = adminUserService.getUserById(userId);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUserStatus_deactivatesUser() {
        User user = User.builder().isActive(true).build();
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(adminUserMapper.toSummaryResponse(user)).thenReturn(UserSummaryResponse.builder().build());

        adminUserService.updateUserStatus(userId, req);

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void updateUserStatus_reactivatesUser() {
        User user = User.builder().isActive(false).build();
        UpdateUserStatusRequest req = new UpdateUserStatusRequest();
        req.setIsActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(adminUserMapper.toSummaryResponse(user)).thenReturn(UserSummaryResponse.builder().build());

        adminUserService.updateUserStatus(userId, req);

        assertThat(user.getIsActive()).isTrue();
    }

    @Test
    void updateUserStatus_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateUserStatus(userId, new UpdateUserStatusRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
