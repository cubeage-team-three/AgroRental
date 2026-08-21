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
    return await request('/bookings', {
      method: 'POST',
      body: JSON.stringify(bookingData),
    });
  },

  /**
   * Retrieves booking details by ID.
   * @param {number} id - Booking ID
   * @returns {Promise<Object>} BookingResponse payload
   */
  getBookingById: async (id) => {
    return await request(`/bookings/${id}`);
  },

  /**
   * Retrieves all bookings for a specific farmer.
   * @param {number} farmerId - Farmer ID
   * @returns {Promise<Array>} Array of BookingResponse objects
   */
  getBookingsByFarmer: async (farmerId) => {
    return await request(`/bookings/farmer/${farmerId}`);
  },

  /**
   * Retrieves all booking requests for a partner's equipment.
   * @param {number} partnerId - Partner ID
   * @returns {Promise<Array>} Array of BookingResponse objects
   */
  getBookingsByPartner: async (partnerId) => {
    return await request(`/bookings/partner/${partnerId}`);
  },

  /**
   * Retrieves all booking requests assigned to a specific operator.
   * @param {number} operatorId - Operator ID
   * @returns {Promise<Array>} Array of BookingResponse objects
   */
  getBookingsByOperator: async (operatorId) => {
    return await request(`/bookings/operator/${operatorId}`);
  },

  /**
   * Cancels an active booking reservation.
   * @param {number} id - Booking ID
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  cancelBooking: async (id) => {
    return await request(`/bookings/${id}/cancel`, {
      method: 'PATCH',
    });
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
