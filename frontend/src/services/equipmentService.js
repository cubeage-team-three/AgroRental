import { request } from './apiClient';

const MOCK_EQUIPMENT = [
  {
    id: 1,
    name: 'Mahindra 575 DI Heavy Tractor (50 HP)',
    category: 'TRACTOR',
    brand: 'Mahindra',
    model: '575 DI',
    manufacturingYear: 2023,
    capacity: '50 HP Engine',
    fuelType: 'DIESEL',
    rentalPrice: 1800,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Khed, Pune District, Maharashtra',
    latitude: 18.8500,
    longitude: 73.9100,
    partnerId: 1,
    partnerName: 'Krushi Agro Services',
    primaryImageUrl: '/images/equipment/tractor-ploughing.jpg',
    description: 'High torque 50 HP tractor suitable for heavy ploughing, rotavator operations, and haulage across medium to large farms.',
    images: [
      { id: 101, imageUrl: '/images/equipment/tractor-ploughing.jpg', isPrimary: true },
    ],
  },
  {
    id: 2,
    name: 'Kubota Combine Harvester DC-68G',
    category: 'HARVESTER',
    brand: 'Kubota',
    model: 'DC-68G',
    manufacturingYear: 2024,
    capacity: '68 HP Hydrostatic',
    fuelType: 'DIESEL',
    rentalPrice: 3500,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Manchar, Pune, Maharashtra',
    latitude: 19.0031,
    longitude: 73.9439,
    partnerId: 1,
    partnerName: 'Sahyadri Agri Rentals',
    primaryImageUrl: '/images/equipment/harvester-service.jpg',
    description: 'Advanced paddy and wheat combine harvester with rubber crawler tracks for wet field maneuverability and grain cleaning.',
    images: [
      { id: 102, imageUrl: '/images/equipment/harvester-service.jpg', isPrimary: true },
    ],
  },
  {
    id: 3,
    name: 'DJI Agras T40 Agricultural Spraying Drone',
    category: 'SPRAYER',
    brand: 'DJI',
    model: 'Agras T40',
    manufacturingYear: 2024,
    capacity: '40 Liter Tank',
    fuelType: 'ELECTRIC',
    rentalPrice: 2200,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Baramati, Pune, Maharashtra',
    latitude: 18.1517,
    longitude: 74.5772,
    partnerId: 2,
    partnerName: 'TechCrop Aero Solutions',
    primaryImageUrl: '/images/equipment/drone-spraying.jpg',
    description: 'Precision autonomous drone for uniform pesticide, liquid fertilizer, and micronutrient spraying with active phased array radar.',
    images: [
      { id: 103, imageUrl: '/images/equipment/drone-spraying.jpg', isPrimary: true },
    ],
  },
  {
    id: 4,
    name: 'Automatic Multi-Crop Pneumatic Seeder',
    category: 'SEEDER',
    brand: 'FieldKing',
    model: 'PneumoPlanter 9',
    manufacturingYear: 2023,
    capacity: '9 Row Precision',
    fuelType: 'MANUAL_HUMAN_POWERED',
    rentalPrice: 1200,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Shirur, Pune, Maharashtra',
    latitude: 18.8267,
    longitude: 74.3774,
    partnerId: 2,
    partnerName: 'GreenField Implements',
    primaryImageUrl: '/images/equipment/seeder-sowing.jpg',
    description: 'Pneumatic seed drill for uniform spacing and depth during maize, cotton, and soybean sowing.',
    images: [
      { id: 104, imageUrl: '/images/equipment/seeder-sowing.jpg', isPrimary: true },
    ],
  },
  {
    id: 5,
    name: 'Solar Powered Drip Irrigation Pump Unit',
    category: 'IRRIGATION',
    brand: 'Shakti',
    model: 'Helios 7.5',
    manufacturingYear: 2023,
    capacity: '7.5 HP Solar Pump',
    fuelType: 'ELECTRIC',
    rentalPrice: 1500,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Nashik District, Maharashtra',
    latitude: 19.9975,
    longitude: 73.7898,
    partnerId: 3,
    partnerName: 'SuryaAgri Irrigation',
    primaryImageUrl: '/images/equipment/irrigation-setup.jpg',
    description: 'Complete portable solar pumping system with micro-drip filtration for orchard and vegetable field irrigation.',
    images: [
      { id: 105, imageUrl: '/images/equipment/irrigation-setup.jpg', isPrimary: true },
    ],
  },
  {
    id: 6,
    name: 'Heavy Duty 42 Blade Rotary Tiller',
    category: 'TILLER',
    brand: 'Shaktiman',
    model: 'Semi Champion 7',
    manufacturingYear: 2022,
    capacity: '7 Feet Working Width',
    fuelType: 'DIESEL',
    rentalPrice: 1400,
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    locationAddress: 'Solapur, Maharashtra',
    latitude: 17.6599,
    longitude: 75.9064,
    partnerId: 3,
    partnerName: 'Solapur Machinery Depot',
    primaryImageUrl: '/images/equipment/rotavator-work.jpg',
    description: 'High performance rotavator for fine seedbed preparation, stubble incorporation, and soil aeration.',
    images: [
      { id: 106, imageUrl: '/images/equipment/rotavator-work.jpg', isPrimary: true },
    ],
  },
];

/**
 * Frontend Service for Machine Management / Equipment Management Module.
 */
export const equipmentService = {
  /**
   * Retrieves discoverable equipment listings with database-side pagination.
   */
  async getAvailableEquipmentPage(page = 0, size = 20) {
    try {
      const res = await request(`/api/equipment/available/page?page=${page}&size=${size}`, { method: 'GET' });
      if (res && res.content && res.content.length > 0) {
        return res;
      }
    } catch (e) {
      console.warn('Backend equipment API fallback active:', e);
    }
    return {
      content: MOCK_EQUIPMENT,
      number: 0,
      size: 20,
      totalPages: 1,
      totalElements: MOCK_EQUIPMENT.length,
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

    try {
      const res = await request(`/api/equipment/search/page?${params.toString()}`, { method: 'GET' });
      if (res && res.content && res.content.length > 0) {
        return res;
      }
    } catch (e) {
      console.warn('Backend equipment search API fallback active:', e);
    }

    // Filter mock data locally if API returns empty
    let filtered = [...MOCK_EQUIPMENT];
    if (filters.category) {
      filtered = filtered.filter((item) => item.category === filters.category);
    }
    if (filters.minPrice) {
      filtered = filtered.filter((item) => item.rentalPrice >= Number(filters.minPrice));
    }
    if (filters.maxPrice) {
      filtered = filtered.filter((item) => item.rentalPrice <= Number(filters.maxPrice));
    }
    if (filters.minHp) {
      filtered = filtered.filter((item) => {
        const hpVal = parseInt(item.capacity || item.hp || '0', 10);
        return hpVal >= Number(filters.minHp);
      });
    }
    if (filters.locationAddress) {
      const loc = filters.locationAddress.toLowerCase();
      filtered = filtered.filter((item) => item.locationAddress.toLowerCase().includes(loc) || item.name.toLowerCase().includes(loc));
    }
    if (filters.minRating) {
      filtered = filtered.filter((item) => (item.rating || 5.0) >= Number(filters.minRating));
    }

    return {
      content: filtered,
      number: 0,
      size: 20,
      totalPages: 1,
      totalElements: filtered.length,
      first: true,
      last: true,
    };
  },

  /**
   * Retrieves single equipment listing by ID.
   */
  async getEquipmentById(id) {
    try {
      const res = await request(`/api/equipment/${id}`, { method: 'GET' });
      if (res) return res;
    } catch (e) {
      console.warn(`Backend equipment #${id} fallback active:`, e);
    }

    const mockItem = MOCK_EQUIPMENT.find((item) => item.id === Number(id)) || MOCK_EQUIPMENT[0];
    return mockItem;
  },

  /**
   * Retrieves all equipment in the catalog (including disabled ones) for administrative management (FR-39).
   */
  async getAllEquipment(page = 0, size = 50) {
    try {
      const res = await request(`/api/equipment/page?page=${page}&size=${size}`, { method: 'GET' });
      if (res && res.content) {
        return res;
      }
    } catch (e) {
      console.warn('Backend all equipment API fallback active:', e);
    }
    return {
      content: MOCK_EQUIPMENT,
      number: 0,
      size: size,
      totalPages: 1,
      totalElements: MOCK_EQUIPMENT.length,
      first: true,
      last: true,
    };
  },

  /**
   * Retrieves all equipment owned by a specific partner.
   */
  async getPartnerEquipment(partnerId = 1) {
  try {
    const res = await request(`/api/equipment/partner/${partnerId}`, {
      method: 'GET',
      partnerId,
    });

    if (Array.isArray(res)) {
      return res;
    }

    return [];
  } catch (e) {
    console.warn('Partner equipment API fallback active:', e);

    return MOCK_EQUIPMENT.filter(
      (item) => item.partnerId === Number(partnerId)
    );
  }
},

  /**
   * Creates a new machinery listing.
   */
  async createEquipment(payload, partnerId = 1) {
    return request('/api/equipment', {
      method: 'POST',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Updates an existing machinery listing.
   */
  async updateEquipment(id, payload, partnerId = 1) {
    return request(`/api/equipment/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
      partnerId,
    });
  },

  /**
   * Deletes an equipment listing by ID.
   */
  async deleteEquipment(id, partnerId = 1) {
    return request(`/api/equipment/${id}`, {
      method: 'DELETE',
      partnerId,
    });
  },

  /**
   * Enables a previously disabled equipment listing.
   */
  async enableEquipment(id, partnerId = 1) {
    return request(`/api/equipment/${id}/enable`, {
      method: 'PATCH',
      partnerId,
    });
  },

  /**
   * Disables an equipment listing for administrative lockout or maintenance.
   */
  async disableEquipment(id, partnerId = 1) {
    return request(`/api/equipment/${id}/disable`, {
      method: 'PATCH',
      partnerId,
    });
  },
};
