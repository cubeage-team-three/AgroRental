import { request } from './apiClient';

/**
 * Service for interacting with backend Operator REST APIs.
 */
export const operatorService = {
  /**
   * Retrieves all registered operators.
   * @returns {Promise<Array>} Array of Operator objects
   */
  getAllOperators: async () => {
    return await request('/operators');
  },

  /**
   * Retrieves available operators filtering by optional date range and partner.
   * @param {Object} params - { partnerId, startDate, endDate }
   * @returns {Promise<Array>} Array of available Operator objects
   */
  getAvailableOperators: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.partnerId) query.append('partnerId', params.partnerId);
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/available${queryString}`);
  },

  /**
   * Retrieves operators assigned to a specific partner.
   * @param {number} partnerId - Partner ID
   * @returns {Promise<Array>} Array of Operator objects
   */
  getOperatorsByPartner: async (partnerId) => {
    return await request(`/operators/partner/${partnerId}`);
  },
};

export default operatorService;
