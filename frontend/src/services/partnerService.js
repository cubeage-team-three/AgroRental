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
};

export default partnerService;
