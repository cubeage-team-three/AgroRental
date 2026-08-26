import { request } from './apiClient';

/**
 * Partner Service abstraction for managing Partner Profile and Partner features.
 * Uses apiClient's request(), which automatically attaches the caller's
 * Authorization: Bearer token — required for every one of these endpoints
 * except registration and the pre-login OTP steps.
 */
export const partnerService = {
  /**
   * Fetch partner profile details by ID.
   */
  getProfile: async (partnerId) => {
    return await request(`/partners/${partnerId}`, { method: 'GET' });
  },

  /**
   * Update partner profile information.
   */
  updateProfile: async (partnerId, profileData) => {
    return await request(`/partners/${partnerId}`, {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  },

  /**
   * Update partner password.
   */
  changePassword: async (partnerId, passwordData) => {
    return await request(`/partners/${partnerId}/password`, {
      method: 'PUT',
      body: JSON.stringify(passwordData),
    });
  },

  /**
   * Register a new Partner.
   */
  registerPartner: async (registrationData) => {
    return await request('/partners/register', {
      method: 'POST',
      body: JSON.stringify(registrationData),
    });
  },

  /**
   * Get partner dashboard metrics.
   */
  getDashboard: async (partnerId) => {
    return await request(`/partners/${partnerId}/dashboard`, { method: 'GET' });
  },

  /**
   * Fetch all registered partners (Admin / KYC oversight).
   */
  getAllPartners: async () => {
    return await request('/partners', { method: 'GET' });
  },

  /**
   * Approve a partner's KYC verification.
   */
  approveKyc: async (partnerId) => {
    return await request(`/partners/${partnerId}/kyc/approve`, { method: 'PUT' });
  },

  /**
   * Reject a partner's KYC verification.
   */
  rejectKyc: async (partnerId) => {
    return await request(`/partners/${partnerId}/kyc/reject`, { method: 'PUT' });
  },

  /**
   * Send OTP for partner verification.
   */
  sendOtp: async (partnerId) => {
    return await request(`/partners/${partnerId}/otp/send`, { method: 'POST' });
  },

  /**
   * Verify partner OTP code.
   */
  verifyOtp: async (partnerId, otp) => {
    return await request(`/partners/${partnerId}/otp/verify`, {
      method: 'POST',
      body: JSON.stringify({ otp }),
    });
  },
};

export default partnerService;
