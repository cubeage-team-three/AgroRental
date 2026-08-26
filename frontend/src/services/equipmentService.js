import { request } from './apiClient';
import { API_BASE_URL } from '../utils/constants';

/**
 * Frontend Service for Machine Management / Equipment Management Module.
 */
export const equipmentService = {
  /**
   * Retrieves discoverable equipment listings with database-side pagination.
   */
  async getAvailableEquipmentPage(page = 0, size = 20) {
    const res = await request(`/api/equipment/available/page?page=${page}&size=${size}`, { method: 'GET' });
    return res;
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
    return res;
  },

  /**
   * Retrieves single equipment listing by ID.
   */
  async getEquipmentById(id) {
    const res = await request(`/api/equipment/${id}`, { method: 'GET' });
    return res;
  },

  /**
   * Retrieves all equipment in the catalog (including disabled ones) for administrative management (FR-39).
   */
  async getAllEquipment(page = 0, size = 50) {
    const res = await request(`/api/equipment/page?page=${page}&size=${size}`, { method: 'GET' });
    return res;
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

  /**
   * Uploads an equipment image file to the backend server.
   */
  async uploadEquipmentImage(file) {
    const formData = new FormData();
    formData.append('file', file);

    let token = null;
    try {
      const storedUserStr = localStorage.getItem('agro_user');
      if (storedUserStr) {
        const parsedUser = JSON.parse(storedUserStr);
        token = parsedUser?.token || parsedUser?.accessToken || null;
      }
    } catch (e) {}
    if (!token) {
      token = localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
    }

    const response = await fetch(`${API_BASE_URL}/upload/image`, {
      method: 'POST',
      headers: {
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      },
      body: formData,
    });

    const data = await response.json();
    if (!response.ok) {
      throw new Error(data.message || 'Image upload failed. Please try again.');
    }
    return data.data; // { url: "/uploads/equipment/filename.ext" }
  },
};
