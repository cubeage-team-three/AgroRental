import { request } from './apiClient';

/**
 * Service for interacting with Operator Job and Booking status APIs.
 * Connects to canonical backend Booking endpoints for Phase 0 architectural safety.
 */
export const getJobDetails = async (jobId) => {
  return await request(`/bookings/${jobId}`);
};

export const acceptJob = async (jobId) => {
  return await request(`/bookings/${jobId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'CONFIRMED' }),
  });
};

export const rejectJob = async (jobId, reason = '') => {
  return await request(`/bookings/${jobId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status: 'REJECTED', notes: reason }),
  });
};

export default {
  getJobDetails,
  acceptJob,
  rejectJob,
};
