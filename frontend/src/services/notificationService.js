import { apiClient } from './apiClient';

/**
 * Service managing client-side notification API integration.
 */
export const notificationService = {

  /**
   * Retrieves notifications for a given recipient role and ID.
   */
  async getUserNotifications(role, id) {
    const data = await apiClient.get('/notifications', {
      params: { role, id },
    });
    return data || [];
  },

  /**
   * Fetches unread notification count.
   */
  async getUnreadCount(role, id) {
    const data = await apiClient.get('/notifications/unread-count', {
      params: { role, id },
    });
    return data?.unreadCount || 0;
  },

  /**
   * Marks a single notification as read.
   */
  async markAsRead(notificationId, role, id) {
    const data = await apiClient.patch(`/notifications/${notificationId}/read`, null, {
      params: { role, id },
    });
    return data;
  },

  /**
   * Marks all notifications as read for a recipient.
   */
  async markAllAsRead(role, id) {
    const data = await apiClient.patch('/notifications/read-all', null, {
      params: { role, id },
    });
    return data;
  },
};

export default notificationService;
