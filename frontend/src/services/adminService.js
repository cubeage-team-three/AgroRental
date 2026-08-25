import { request } from './apiClient';

/**
 * Service for administrative actions and management.
 */
export const adminService = {
  /**
   * Retrieves a paginated list of operators with optional status and mobile verification filters.
   * @param {Object} params - Query parameters (status, mobileVerified, page, size)
   * @returns {Promise<Object>} Page of OperatorSummaryResponse
   */
  getOperators: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.status && params.status !== 'ALL') {
      query.append('status', params.status);
    }
    if (params.mobileVerified !== undefined && params.mobileVerified !== null) {
      query.append('mobileVerified', params.mobileVerified);
    }
    if (params.page !== undefined) {
      query.append('page', params.page);
    }
    if (params.size !== undefined) {
      query.append('size', params.size);
    }

    const queryString = query.toString();
    const endpoint = queryString ? `/admin/operators?${queryString}` : '/admin/operators';
    return await request(endpoint);
  },

  /**
   * Retrieves full operator detail for review.
   * @param {number|string} id - Operator ID
   * @returns {Promise<Object>} OperatorDetailResponse
   */
  getOperatorById: async (id) => {
    return await request(`/admin/operators/${id}`);
  },

  /**
   * Approves or rejects an operator account.
   * @param {number|string} id - Operator ID
   * @param {Object} verificationData - { status: 'APPROVED' | 'REJECTED', rejectionReason: string }
   * @returns {Promise<Object>} OperatorDetailResponse
   */
  verifyOperator: async (id, verificationData) => {
    return await request(`/admin/operators/${id}/verify`, {
      method: 'PATCH',
      body: JSON.stringify(verificationData),
    });
  },

  /**
   * Retrieves aggregate platform-wide metrics for the Admin Overview bento cards.
   * @returns {Promise<Object>} AdminDashboardStatsResponse
   */
  getDashboardStats: async () => {
    return await request('/admin/dashboard/stats');
  },

  /**
   * Retrieves the most recent bookings platform-wide for the recent activity table.
   * @returns {Promise<Array>} List of BookingResponse
   */
  getRecentBookings: async () => {
    return await request('/admin/dashboard/recent-bookings');
  },
};

export default adminService;
