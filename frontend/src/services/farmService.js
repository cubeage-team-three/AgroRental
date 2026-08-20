import { apiClient } from './apiClient';

let MOCK_FARMS = [
  {
    id: 1,
    farmerId: 1,
    farmName: 'Sunrise Agro Fields',
    village: 'Khed',
    taluka: 'Khed',
    district: 'Pune',
    state: 'Maharashtra',
    latitude: 18.8500,
    longitude: 73.9100,
    farmArea: 15.5,
    cropType: 'Wheat & Sugarcane',
    createdAt: new Date().toISOString(),
  },
  {
    id: 2,
    farmerId: 1,
    farmName: 'Green Valley Organic Plot',
    village: 'Manchar',
    taluka: 'Ambegaon',
    district: 'Pune',
    state: 'Maharashtra',
    latitude: 19.0031,
    longitude: 73.9439,
    farmArea: 8.2,
    cropType: 'Soybean & Vegetables',
    createdAt: new Date().toISOString(),
  },
];

/**
 * Service for interacting with Farmer Farm Management APIs (Module 6).
 */
export const farmService = {
  /**
   * Fetch all registered farms for a farmer.
   */
  async getFarms(farmerId = 1) {
    try {
      const response = await apiClient.get(`/api/farmers/farms?farmerId=${farmerId}`);
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
   * Fetch details for a specific farm by ID.
   */
  async getFarmById(id) {
    try {
      const response = await apiClient.get(`/api/farmers/farms/${id}`);
      return response;
    } catch (error) {
      console.warn(`API fetch failed for farm #${id}, using fallback:`, error);
      return MOCK_FARMS.find((f) => f.id === Number(id)) || MOCK_FARMS[0];
    }
  },

  /**
   * Create a new farm.
   */
  async createFarm(farmData) {
    try {
      const response = await apiClient.post('/api/farmers/farms', farmData);
      if (response && response.id) {
        MOCK_FARMS.push(response);
        return response;
      }
      const newFarm = { id: Date.now(), ...farmData, createdAt: new Date().toISOString() };
      MOCK_FARMS.push(newFarm);
      return newFarm;
    } catch (error) {
      console.warn('API create farm failed, saving to local state fallback:', error);
      const newFarm = { id: Date.now(), ...farmData, createdAt: new Date().toISOString() };
      MOCK_FARMS.push(newFarm);
      return newFarm;
    }
  },

  /**
   * Update an existing farm.
   */
  async updateFarm(id, farmData) {
    try {
      const response = await apiClient.put(`/api/farmers/farms/${id}`, farmData);
      if (response && response.id) {
        MOCK_FARMS = MOCK_FARMS.map((f) => (f.id === Number(id) ? response : f));
        return response;
      }
      const updated = { id: Number(id), ...farmData, updatedAt: new Date().toISOString() };
      MOCK_FARMS = MOCK_FARMS.map((f) => (f.id === Number(id) ? updated : f));
      return updated;
    } catch (error) {
      console.warn(`API update farm #${id} failed, updating local state fallback:`, error);
      const updated = { id: Number(id), ...farmData, updatedAt: new Date().toISOString() };
      MOCK_FARMS = MOCK_FARMS.map((f) => (f.id === Number(id) ? updated : f));
      return updated;
    }
  },

  /**
   * Delete a farm record.
   */
  async deleteFarm(id) {
    try {
      const response = await apiClient.delete(`/api/farmers/farms/${id}`);
      MOCK_FARMS = MOCK_FARMS.filter((f) => f.id !== Number(id));
      return response;
    } catch (error) {
      console.warn(`API delete farm #${id} failed, removing from local state fallback:`, error);
      MOCK_FARMS = MOCK_FARMS.filter((f) => f.id !== Number(id));
      return { success: true };
    }
  },
};

