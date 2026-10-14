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

  /**
   * Retrieves paginated, filtered bookings for the Admin Booking Management table.
   * @param {Object} params - Query parameters (page, size, status, search, startDate, endDate, sort)
   * @returns {Promise<Object>} Page of BookingResponse
   */
  getBookings: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.status && params.status !== 'ALL') {
      query.append('status', params.status);
    }
    if (params.search && params.search.trim()) {
      query.append('search', params.search.trim());
    }
    if (params.startDate) {
      query.append('startDate', params.startDate);
    }
    if (params.endDate) {
      query.append('endDate', params.endDate);
    }
    if (params.page !== undefined) {
      query.append('page', params.page);
    }
    if (params.size !== undefined) {
      query.append('size', params.size);
    }
    if (params.sort) {
      query.append('sort', params.sort);
    }

    const queryString = query.toString();
    const endpoint = queryString ? `/admin/bookings?${queryString}` : '/admin/bookings';
    return await request(endpoint);
  },

  /**
   * Retrieves aggregate booking statistics for Admin Booking Management summary cards.
   * @returns {Promise<Object>} AdminBookingStatsResponse
   */
  getBookingStats: async () => {
    return await request('/admin/bookings/stats');
  },

  /**
   * Retrieves full details for a single booking by ID.
   * @param {number|string} id - Booking ID
   * @returns {Promise<Object>} BookingResponse
   */
  getBookingById: async (id) => {
    return await request(`/admin/bookings/${id}`);
  },

  /**
   * Updates booking status or assigns operator with admin authorization.
   * @param {number|string} id - Booking ID
   * @param {Object} statusData - { status, operatorId, rejectionReason }
   * @returns {Promise<Object>} Updated BookingResponse
   */
  updateBookingStatus: async (id, statusData) => {
    return await request(`/admin/bookings/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(statusData),
    });
  },

  /**
   * Cancels a booking, restoring equipment availability and cleaning up operator assignments.
   * @param {number|string} id - Booking ID
   * @returns {Promise<Object>} Updated BookingResponse
   */
  cancelBooking: async (id) => {
    return await request(`/admin/bookings/${id}/cancel`, {
      method: 'PATCH',
    });
  },
};

export default adminService;
