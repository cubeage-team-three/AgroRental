import { apiClient } from './apiClient';

/**
 * Service managing client-side payment and revenue transaction API communication.
 */
export const paymentService = {

  /**
   * Initiates a simulated payment transaction for a booking.
   */
  async createPayment(payload) {
    const data = await apiClient.post('/payments', payload);
    return data;
  },

  /**
   * Fetches payment details by primary key.
   */
  async getPaymentById(id) {
    const data = await apiClient.get(`/payments/${id}`);
    return data;
  },

  /**
   * Fetches payment details for a specific booking ID.
   */
  async getPaymentByBookingId(bookingId) {
    const data = await apiClient.get(`/payments/booking/${bookingId}`);
    return data;
  },

  /**
   * Retrieves transaction ledger for a farmer.
   */
  async getFarmerPayments(farmerId) {
    const data = await apiClient.get(`/payments/farmer/${farmerId}`);
    return data || [];
  },

  /**
   * Retrieves revenue transactions for a partner.
   */
  async getPartnerPayments(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}`);
    return data || [];
  },

  /**
   * Retrieves realized financial summary for a partner.
   */
  async getPartnerEarnings(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}/earnings`);
    return data;
  },
};

export default paymentService;
