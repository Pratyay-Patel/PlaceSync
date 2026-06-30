package com.placesync.common.admin.mapper;

import com.placesync.common.admin.dto.UserSummaryResponse;
import com.placesync.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminUserMapper {
    UserSummaryResponse toSummaryResponse(User user);
}
