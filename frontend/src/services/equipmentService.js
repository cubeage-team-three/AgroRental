import { request } from './apiClient';

/**
 * Frontend Service for Machine Management / Equipment Management Module.
 */
export const equipmentService = {
  /**
   * Retrieves discoverable equipment listings with database-side pagination.
   */
  async getAvailableEquipmentPage(page = 0, size = 20) {
    const res = await request(`/api/equipment/available/page?page=${page}&size=${size}`, { method: 'GET' });
    return res || {
      content: [],
      number: page,
      size: size,
      totalPages: 0,
      totalElements: 0,
      first: true,
      last: true,
    };
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
    if (filters.minHp) params.append('minHp', filters.minHp);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.minRating) params.append('minRating', filters.minRating);
    if (filters.userLat) params.append('userLat', filters.userLat);
    if (filters.userLng) params.append('userLng', filters.userLng);
    if (filters.maxDistanceKm) params.append('maxDistanceKm', filters.maxDistanceKm);
    params.append('page', page);
    params.append('size', size);

    const res = await request(`/api/equipment/search/page?${params.toString()}`, { method: 'GET' });
    return res || {
      content: [],
      number: page,
      size: size,
      totalPages: 0,
      totalElements: 0,
      first: true,
      last: true,
    };
  },

  /**
   * Retrieves single equipment listing by ID.
   */
  async getEquipmentById(id) {
    return request(`/api/equipment/${id}`, { method: 'GET' });
  },

  /**
   * Retrieves all equipment in the catalog (including disabled ones) for administrative management (FR-39).
   */
  async getAllEquipment(page = 0, size = 50) {
    const res = await request(`/api/equipment/page?page=${page}&size=${size}`, { method: 'GET' });
    return res || {
      content: [],
      number: page,
      size: size,
      totalPages: 0,
      totalElements: 0,
      first: true,
      last: true,
    };
  },

  /**
   * Retrieves all equipment owned by a specific partner.
   */
  async getPartnerEquipment(partnerId) {
    if (!partnerId) return [];
    const res = await request(`/api/equipment/partner/${partnerId}`, {
      method: 'GET',
      partnerId,
    });

    if (Array.isArray(res)) {
      return res;
    }
    return [];
  },

  /**
   * Creates a new machinery listing.
   */
  async createEquipment(payload, partnerId) {
    return request('/api/equipment', {
      method: 'POST',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Updates an existing machinery listing.
   */
  async updateEquipment(id, payload, partnerId) {
    return request(`/api/equipment/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Deletes an equipment listing by ID.
   */
  async deleteEquipment(id, partnerId) {
    return request(`/api/equipment/${id}`, {
      method: 'DELETE',
      partnerId,
    });
  },

  /**
   * Enables a previously disabled equipment listing.
   */
  async enableEquipment(id, partnerId) {
    return request(`/api/equipment/${id}/enable`, {
      method: 'PATCH',
      partnerId,
    });
  },

  /**
   * Disables an equipment listing for administrative lockout or maintenance.
   */
  async disableEquipment(id, partnerId) {
    return request(`/api/equipment/${id}/disable`, {
      method: 'PATCH',
      partnerId,
    });
  },
};
