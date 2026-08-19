import { apiClient } from './apiClient';

/**
 * Service managing client-side reviews and star ratings API communication.
 */
export const reviewService = {

  /**
   * Submits a review for a completed booking.
   */
  async createReview(payload) {
    const data = await apiClient.post('/reviews', payload);
    return data;
  },

  /**
   * Fetches review by booking ID.
   */
  async getReviewByBookingId(bookingId) {
    const data = await apiClient.get(`/reviews/booking/${bookingId}`);
    return data;
  },

  /**
   * Fetches list of reviews for equipment.
   */
  async getReviewsForEquipment(equipmentId) {
    const data = await apiClient.get(`/reviews/equipment/${equipmentId}`);
    return data || [];
  },

  /**
   * Fetches average rating summary for equipment.
   */
  async getEquipmentRatingSummary(equipmentId) {
    const data = await apiClient.get(`/reviews/equipment/${equipmentId}/summary`);
    return data;
  },

  /**
   * Fetches list of reviews for a partner.
   */
  async getReviewsForPartner(partnerId) {
    const data = await apiClient.get(`/reviews/partner/${partnerId}`);
    return data || [];
  },

  /**
   * Fetches average rating summary for partner.
   */
  async getPartnerRatingSummary(partnerId) {
    const data = await apiClient.get(`/reviews/partner/${partnerId}/summary`);
    return data;
  },
};

export default reviewService;
