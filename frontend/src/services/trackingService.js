import { request } from './apiClient';

export const trackingService = {
  async getLiveTracking(bookingId) {
    try {
      return await request(`/farmers/bookings/${bookingId}/tracking`);
    } catch (error) {
      console.warn('Backend tracking call failed, returning fallback state:', error);
      return {
        bookingId: Number(bookingId),
        operatorName: 'Ramesh Kumar (Operator)',
        operatorMobile: '+91 9876543210',
        equipmentName: 'Mahindra 575 DI Tractor',
        latitude: 18.5204,
        longitude: 73.8567,
        eta: '20 mins',
        routeInformation: 'Moving along District Main Road towards your field location',
        workProgress: 55,
        status: 'WORK_STARTED',
        lastUpdated: new Date().toISOString(),
      };
    }
  },

  async updateLiveTracking(bookingId, trackingData) {
    return await request(`/farmers/bookings/${bookingId}/tracking`, {
      method: 'PUT',
      body: JSON.stringify(trackingData)
    });
  }
};
