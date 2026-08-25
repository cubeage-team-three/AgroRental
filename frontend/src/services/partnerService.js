import { apiRequest } from './api';
import { request } from './apiClient';

/**
 * Partner Service abstraction for managing Partner Profile and Partner features.
 */
export const partnerService = {
  /**
   * Fetch partner profile details by ID.
   */
  getProfile: async (partnerId) => {
    try {
      const res = await apiRequest(`/partners/${partnerId}`, 'GET');
      return res.data || res;
    } catch (err) {
      // Fallback for apiClient if apiRequest fails or custom wrapper
      return await request(`/partners/${partnerId}`, {
        partnerId: String(partnerId),
      });
    }
  },

  /**
   * Update partner profile information.
   */
  updateProfile: async (partnerId, profileData) => {
    return await apiRequest(`/partners/${partnerId}`, 'PUT', profileData);
  },

  /**
   * Update partner password.
   */
  changePassword: async (partnerId, passwordData) => {
    return await apiRequest(`/partners/${partnerId}/password`, 'PUT', passwordData);
  },

  /**
   * Register a new Partner.
   */
  registerPartner: async (registrationData) => {
    return await apiRequest('/partners/register', 'POST', registrationData);
  },

  /**
   * Get partner dashboard metrics.
   */
  getDashboard: async (partnerId) => {
    return await apiRequest(`/partners/${partnerId}/dashboard`, 'GET');
  },

  /**
   * Fetch all registered partners (Admin / KYC oversight).
   */
  getAllPartners: async () => {
    const res = await apiRequest('/partners', 'GET');
    return res.data || res;
  },

  /**
   * Approve a partner's KYC verification.
   */
  approveKyc: async (partnerId) => {
    const res = await apiRequest(`/partners/${partnerId}/kyc/approve`, 'PUT');
    return res.data || res;
  },

  /**
   * Reject a partner's KYC verification.
   */
  rejectKyc: async (partnerId) => {
    const res = await apiRequest(`/partners/${partnerId}/kyc/reject`, 'PUT');
    return res.data || res;
  },

  /**
   * Send OTP for partner verification.
   */
  sendOtp: async (partnerId) => {
    const res = await apiRequest(`/partners/${partnerId}/otp/send`, 'POST');
    return res.data || res;
  },

  /**
   * Verify partner OTP code.
   */
  verifyOtp: async (partnerId, otp) => {
    const res = await apiRequest(`/partners/${partnerId}/otp/verify`, 'POST', { otp });
    return res.data || res;
  },
};

export default partnerService;
