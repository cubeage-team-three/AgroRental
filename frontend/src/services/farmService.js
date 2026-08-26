import { apiClient } from './apiClient';

/**
 * Service for interacting with Farmer Farm Management APIs (Module 6).
 */
export const farmService = {
  /**
   * Fetch all registered farms for the authenticated farmer.
   */
  async getFarms(farmerId) {
    try {
      const endpoint = farmerId ? `/api/farmers/farms?farmerId=${farmerId}` : '/api/farmers/farms';
      const response = await apiClient.get(endpoint);
      if (Array.isArray(response)) {
        return response;
      }
      return response ? (Array.isArray(response.data) ? response.data : []) : [];
    } catch (error) {
      console.warn('API fetch failed for farms:', error);
      return [];
    }
  },

  /**
   * Alias for getFarms
   */
  async getFarmerFarms(farmerId) {
    return this.getFarms(farmerId);
  },

  /**
   * Fetch details for a specific farm by ID.
   */
  async getFarmById(id) {
    const response = await apiClient.get(`/api/farmers/farms/${id}`);
    return response;
  },

  /**
   * Create a new farm.
   */
  async createFarm(farmData) {
    const response = await apiClient.post('/api/farmers/farms', farmData);
    return response;
  },

  /**
   * Update an existing farm.
   */
  async updateFarm(id, farmData) {
    const response = await apiClient.put(`/api/farmers/farms/${id}`, farmData);
    return response;
  },

  /**
   * Delete a farm record.
   */
  async deleteFarm(id) {
    const response = await apiClient.delete(`/api/farmers/farms/${id}`);
    return response;
  },
};
