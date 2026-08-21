import { apiClient } from './apiClient';

/**
 * Service managing client-side payment, invoice, and revenue transaction API communication.
 */
export const paymentService = {
  /**
   * Processes a payment transaction for a booking.
   */
  async processPayment(paymentData) {
    try {
      return await apiClient.post('/farmers/payments', paymentData);
    } catch {
      return await apiClient.post('/payments', paymentData);
    }
  },

  async createPayment(payload) {
    return await apiClient.post('/payments', payload);
  },

  /**
   * Fetches payment details by primary key.
   */
  async getPaymentById(id) {
    try {
      return await apiClient.get(`/farmers/payments/${id}`);
    } catch {
      return await apiClient.get(`/payments/${id}`);
    }
  },

  /**
   * Fetches payment details for a specific booking ID.
   */
  async getPaymentByBookingId(bookingId) {
    return await apiClient.get(`/payments/booking/${bookingId}`);
  },

  /**
   * Retrieves transaction ledger for a farmer.
   */
  async getFarmerPayments(farmerId) {
    try {
      const res = await apiClient.get(`/farmers/payments/farmer/${farmerId}`);
      return res?.data || res || [];
    } catch {
      const data = await apiClient.get(`/payments/farmer/${farmerId}`);
      return data || [];
    }
  },

  /**
   * Generates and retrieves itemized tax invoice for a booking.
   */
  async getInvoice(bookingId) {
    return await apiClient.get(`/farmers/bookings/${bookingId}/invoice`);
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

  /**
   * FR-19: Retrieves detailed daily/weekly/monthly/yearly earnings breakdown.
   */
  async getPartnerEarningsDetail(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}/earnings-detail`);
    return data;
  },

  /**
   * FR-19 Report 1: Booking Revenue Report
   */
  async getBookingRevenueReport(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}/reports/booking-revenue`);
    return data || [];
  },

  /**
   * FR-19 Report 2: Equipment-wise Revenue Report
   */
  async getEquipmentRevenueReport(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}/reports/equipment-revenue`);
    return data || [];
  },

  /**
   * FR-19 Report 3: Customer-wise Revenue Report
   */
  async getCustomerRevenueReport(partnerId) {
    const data = await apiClient.get(`/payments/partner/${partnerId}/reports/customer-revenue`);
    return data || [];
  },
};

export default paymentService;
