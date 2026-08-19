import { request } from './apiClient';

/**
 * Service for interacting with backend Online Payment & Invoice REST APIs.
 */
export const paymentService = {
  /**
   * Processes a payment transaction for a booking.
   * @param {Object} paymentData - { bookingId, farmerId, amount, paymentMethod }
   * @returns {Promise<Object>} PaymentResponse payload
   */
  processPayment: async (paymentData) => {
    return await request('/farmers/payments', {
      method: 'POST',
      body: JSON.stringify(paymentData),
    });
  },

  /**
   * Retrieves payment by ID.
   * @param {number} id - Payment ID
   * @returns {Promise<Object>} PaymentResponse payload
   */
  getPaymentById: async (id) => {
    return await request(`/farmers/payments/${id}`);
  },

  /**
   * Retrieves all payments for a specific farmer.
   * @param {number} farmerId - Farmer ID
   * @returns {Promise<Array>} Array of PaymentResponse objects
   */
  getFarmerPayments: async (farmerId) => {
    return await request(`/farmers/payments/farmer/${farmerId}`);
  },

  /**
   * Generates and retrieves itemized tax invoice for a booking.
   * @param {number} bookingId - Booking ID
   * @returns {Promise<Object>} InvoiceResponse payload
   */
  getInvoice: async (bookingId) => {
    return await request(`/farmers/bookings/${bookingId}/invoice`);
  },
};

export default paymentService;
