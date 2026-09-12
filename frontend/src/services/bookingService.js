import { request } from './apiClient';

/**
 * Service for interacting with backend Booking & Machine Reservation REST APIs.
 */
export const bookingService = {
  /**
   * Creates a new machinery reservation request for a farmer.
   * @param {Object} bookingData - { equipmentId, farmerId, startDate, endDate, deliveryAddress, notes }
   * @returns {Promise<Object>} Created BookingResponse payload
   */
  createBooking: async (bookingData) => {
    try {
      return await request('/bookings', {
        method: 'POST',
        body: JSON.stringify(bookingData),
      });
    } catch {
      return await request('/farmers/bookings', {
        method: 'POST',
        body: JSON.stringify(bookingData),
      });
    }
  },

  getBookingById: async (id) => {
    try {
      return await request(`/bookings/${id}`);
    } catch {
      return await request(`/farmers/bookings/${id}`);
    }
  },

  getBookingsByFarmer: async (farmerId) => {
    if (!farmerId) return [];
    try {
      return await request(`/bookings/farmer/${farmerId}`);
    } catch {
      return await request(`/farmers/bookings?farmerId=${farmerId}`);
    }
  },

  getBookingsByPartner: async (partnerId) => {
    if (!partnerId) return [];
    return await request(`/bookings/partner/${partnerId}`);
  },

  getBookingsByOperator: async (operatorId) => {
    if (!operatorId) return [];
    return await request(`/bookings/operator/${operatorId}`);
  },

  cancelBooking: async (id) => {
    try {
      return await request(`/bookings/${id}/cancel`, {
        method: 'PATCH',
      });
    } catch {
      return await request(`/farmers/bookings/${id}/cancel`, {
        method: 'PUT',
      });
    }
  },

  /**
   * Updates booking status or assigns an operator.
   * @param {number} id - Booking ID
   * @param {Object} statusData - { status, operatorId, rejectionReason }
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  updateBookingStatus: async (id, statusData) => {
    return await request(`/bookings/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(statusData),
    });
  },

  /**
   * Partner accepts a booking request.
   * @param {number} id - Booking ID
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  acceptBooking: async (id) => {
    return await request(`/bookings/${id}/accept`, {
      method: 'PATCH',
    });
  },

  /**
   * Partner rejects a booking request with a reason.
   * @param {number} id - Booking ID
   * @param {string} rejectionReason - Reason for rejection
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  rejectBooking: async (id, rejectionReason) => {
    return await request(`/bookings/${id}/reject`, {
      method: 'PATCH',
      body: JSON.stringify({ rejectionReason }),
    });
  },

  /**
   * Partner assigns an operator to a booking.
   * @param {number} id - Booking ID
   * @param {number} operatorId - Operator ID
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  assignOperator: async (id, operatorId) => {
    return await request(`/bookings/${id}/assign-operator`, {
      method: 'PATCH',
      body: JSON.stringify({ operatorId }),
    });
  },
};

export default bookingService;
