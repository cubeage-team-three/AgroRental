import { request } from './apiClient';

/**
 * Frontend Service for Machine Management / Equipment Management Module.
 * Connects UI components to verified Spring Boot REST endpoints.
 */
export const equipmentService = {
  /**
   * Retrieves discoverable equipment listings with database-side pagination.
   */
  async getAvailableEquipmentPage(page = 0, size = 20) {
    return request(`/equipment/available/page?page=${page}&size=${size}`, {
      method: 'GET',
    });
  },

  /**
   * Dynamically searches for equipment matching filter criteria with pagination.
   */
  async searchEquipmentPage(filters = {}, page = 0, size = 20) {
    const params = new URLSearchParams();
    if (filters.category) params.append('category', filters.category);
    if (filters.minPrice) params.append('minPrice', filters.minPrice);
    if (filters.maxPrice) params.append('maxPrice', filters.maxPrice);
    if (filters.availabilityStatus) params.append('availabilityStatus', filters.availabilityStatus);
    if (filters.locationAddress) params.append('locationAddress', filters.locationAddress);
    params.append('page', page);
    params.append('size', size);

    return request(`/equipment/search/page?${params.toString()}`, {
      method: 'GET',
    });
  },

  /**
   * Retrieves single equipment listing by ID.
   */
  async getEquipmentById(id) {
    return request(`/equipment/${id}`, {
      method: 'GET',
    });
  },

  /**
   * Retrieves all equipment owned by a specific partner.
   */
  async getPartnerEquipment(partnerId = 1) {
    return request(`/equipment/partner/${partnerId}`, {
      method: 'GET',
      partnerId,
    });
  },

  /**
   * Creates a new machinery listing.
   */
  async createEquipment(payload, partnerId = 1) {
    return request('/equipment', {
      method: 'POST',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Updates an existing machinery listing.
   */
  async updateEquipment(id, payload, partnerId = 1) {
    return request(`/equipment/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Re-enables an administratively disabled equipment.
   */
  async enableEquipment(id, partnerId = 1) {
    return request(`/equipment/${id}/enable`, {
      method: 'PUT',
      partnerId,
    });
  },

  /**
   * Administratively disables an equipment.
   */
  async disableEquipment(id, partnerId = 1) {
    return request(`/equipment/${id}/disable`, {
      method: 'PUT',
      partnerId,
    });
  },

  /**
   * Deactivates / soft-deletes equipment listing.
   */
  async deleteEquipment(id, partnerId = 1) {
    return request(`/equipment/${id}`, {
      method: 'DELETE',
      partnerId,
    });
  },

  /**
   * Deletes a specific equipment image.
   */
  async deleteEquipmentImage(equipmentId, imageId, partnerId = 1) {
    return request(`/equipment/${equipmentId}/images/${imageId}`, {
      method: 'DELETE',
      partnerId,
    });
  },
};
