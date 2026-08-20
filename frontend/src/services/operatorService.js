import { request } from './apiClient';

/**
 * Service for interacting with backend Operator REST APIs.
 */
export const operatorService = {
  /**
   * Registers a new operator account with PENDING verification status.
   * @param {Object} operatorData - Operator registration payload
   * @returns {Promise<Object>} OperatorResponse payload
   */
  registerOperator: async (operatorData) => {
    return await request('/operators/register', {
      method: 'POST',
      body: JSON.stringify(operatorData),
    });
  },
};

export default operatorService;
