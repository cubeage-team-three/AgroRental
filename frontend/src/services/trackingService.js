import { request } from './apiClient';

export const trackingService = {
  // Errors propagate to the page: never substitute invented operator, position or progress data
  async getLiveTracking(bookingId) {
    return await request(`/farmers/bookings/${bookingId}/tracking`);
  },

  async updateLiveTracking(bookingId, trackingData) {
    return await request(`/farmers/bookings/${bookingId}/tracking`, {
      method: 'PUT',
      body: JSON.stringify(trackingData)
    });
  }
};
