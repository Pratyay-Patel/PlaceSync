import axiosClient from './axiosClient';
import type { PagedResponse } from '../types/common';
import type { Notification } from '../types/notification';

export const notificationApi = {
  getUnreadCount: () =>
    axiosClient
      .get<{ success: boolean; data: number }>('/notifications/unread-count')
      .then((r) => r.data.data),

  getNotifications: (page = 0, size = 20, unreadOnly = false) =>
    axiosClient
      .get<{ success: boolean; data: PagedResponse<Notification> }>('/notifications', {
        params: { page, size, unreadOnly },
      })
      .then((r) => r.data.data),

  markAsRead: (id: string) =>
    axiosClient.patch(`/notifications/${id}/read`),

  markAllAsRead: () =>
    axiosClient.patch('/notifications/read-all'),
};
