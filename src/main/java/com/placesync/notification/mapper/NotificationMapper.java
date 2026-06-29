package com.placesync.notification.mapper;

import com.placesync.notification.dto.NotificationResponse;
import com.placesync.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
