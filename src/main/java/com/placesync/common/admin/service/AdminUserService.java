package com.placesync.common.admin.service;

import com.placesync.common.admin.dto.UpdateUserStatusRequest;
import com.placesync.common.admin.dto.UserSummaryResponse;
import com.placesync.common.admin.mapper.AdminUserMapper;
import com.placesync.common.exception.ResourceNotFoundException;
import com.placesync.common.spec.UserSearchSpecification;
import com.placesync.common.util.PagedResponse;
import com.placesync.user.entity.User;
import com.placesync.user.entity.UserRole;
import com.placesync.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;
    private final AdminUserMapper adminUserMapper;

    @Transactional(readOnly = true)
    public PagedResponse<UserSummaryResponse> searchUsers(String email, UserRole role, Boolean isActive, Pageable pageable) {
        return PagedResponse.of(
                userRepository.findAll(UserSearchSpecification.withFilters(email, role, isActive), pageable)
                        .map(adminUserMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(adminUserMapper::toSummaryResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Transactional
    public UserSummaryResponse updateUserStatus(UUID userId, UpdateUserStatusRequest req) {
        log.info("Admin updating status of userId={} to isActive={}", userId, req.getIsActive());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setIsActive(req.getIsActive());
        return adminUserMapper.toSummaryResponse(userRepository.save(user));
    }
}
