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
   * @param {Object} statusData - { status, operatorId }
   * @returns {Promise<Object>} Updated BookingResponse payload
   */
  updateBookingStatus: async (id, statusData) => {
    return await request(`/bookings/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify(statusData),
    });
  },
};

export default bookingService;
