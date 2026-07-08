import axiosClient from './axiosClient';

export const notificationApi = {
  getUnreadCount: () =>
    axiosClient
      .get<{ success: boolean; data: number }>('/notifications/unread-count')
      .then((r) => r.data.data),
};
