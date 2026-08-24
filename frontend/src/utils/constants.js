export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const EQUIPMENT_CATEGORIES = [
  { value: 'TRACTOR', label: 'Tractor' },
  { value: 'HARVESTER', label: 'Harvester' },
  { value: 'TILLER', label: 'Tiller / Cultivator' },
  { value: 'IRRIGATION', label: 'Irrigation Equipment' },
  { value: 'SEEDER', label: 'Seeder / Planter' },
  { value: 'SPRAYER', label: 'Sprayer' },
  { value: 'OTHER', label: 'Other Machinery' },
];

export const FUEL_TYPES = [
  { value: 'DIESEL', label: 'Diesel' },
  { value: 'PETROL', label: 'Petrol' },
  { value: 'ELECTRIC', label: 'Electric' },
  { value: 'HYBRID', label: 'Hybrid' },
  { value: 'MANUAL_HUMAN_POWERED', label: 'Manual / Human Powered' },
  { value: 'OTHER', label: 'Other' },
];

export const AVAILABILITY_STATUSES = [
  { value: 'AVAILABLE', label: 'Available', badgeClass: 'bg-green-100 text-green-800 border-green-300' },
  { value: 'BOOKED', label: 'Booked', badgeClass: 'bg-blue-100 text-blue-800 border-blue-300' },
  { value: 'UNDER_MAINTENANCE', label: 'Under Maintenance', badgeClass: 'bg-amber-100 text-amber-800 border-amber-300' },
  { value: 'INACTIVE', label: 'Inactive', badgeClass: 'bg-gray-100 text-gray-800 border-gray-300' },
];

export const DEFAULT_EQUIPMENT_IMAGE = '/images/equipment/tractor-ploughing.jpg';

export const CATEGORY_IMAGES = {
  TRACTOR: '/images/equipment/tractor-ploughing.jpg',
  HARVESTER: '/images/equipment/harvester-service.jpg',
  SPRAYER: '/images/equipment/drone-spraying.jpg',
  SEEDER: '/images/equipment/seeder-sowing.jpg',
  IRRIGATION: '/images/equipment/irrigation-setup.jpg',
  TILLER: '/images/equipment/rotavator-work.jpg',
  OTHER: '/images/equipment/custom-field-jobs.jpg',
};

export function getCategoryEquipmentImage(category) {
  if (!category) return DEFAULT_EQUIPMENT_IMAGE;
  const upper = String(category).toUpperCase();
  return CATEGORY_IMAGES[upper] || DEFAULT_EQUIPMENT_IMAGE;
}

export function formatCategoryLabel(category) {
  const item = EQUIPMENT_CATEGORIES.find((c) => c.value === category);
  return item ? item.label : category;
}

export function formatFuelTypeLabel(fuelType) {
  const item = FUEL_TYPES.find((f) => f.value === fuelType);
  return item ? item.label : fuelType;
}

export function getStatusBadgeInfo(status) {
  const item = AVAILABILITY_STATUSES.find((s) => s.value === status);
  return item || { label: status, badgeClass: 'bg-gray-100 text-gray-800 border-gray-300' };
}
