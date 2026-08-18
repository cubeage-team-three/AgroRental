import { request } from './apiClient';

/**
 * Service methods for Operator Job Assignment (Module 6)
 */

/**
 * Fetch assigned jobs for the authenticated operator.
 * Optional status filter: 'PENDING_RESPONSE', 'ACCEPTED', 'COMPLETED', 'REJECTED', 'CANCELLED'
 */
export async function getAssignedJobs(status = null) {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return request(`/operators/jobs${query}`, {
    method: 'GET',
  });
}

/**
 * Fetch detailed information for a specific job belonging to the authenticated operator.
 */
export async function getJobDetails(jobId) {
  return request(`/operators/jobs/${jobId}`, {
    method: 'GET',
  });
}

/**
 * Fetch job counts summary (total, pending, accepted, completed, rejected, cancelled)
 * for the authenticated operator's dashboard.
 */
export async function getJobsSummary() {
  return request('/operators/jobs/summary', {
    method: 'GET',
  });
}

/**
 * Assign a job to an operator (Admin / Partner workflow).
 */
export async function assignJob(data) {
  return request('/operators/jobs/assign', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

/**
 * Accept an assigned job (Module 7).
 */
export async function acceptJob(jobId) {
  return request(`/operators/jobs/${jobId}/accept`, {
    method: 'PUT',
  });
}

/**
 * Reject an assigned job with optional reason (Module 7).
 */
export async function rejectJob(jobId, reason = '') {
  const query = reason ? `?reason=${encodeURIComponent(reason)}` : '';
  return request(`/operators/jobs/${jobId}/reject${query}`, {
    method: 'PUT',
  });
}

/**
 * Update job work status (Module 8: TRAVELING, REACHED_LOCATION, WORK_STARTED, WORK_PAUSED, WORK_RESUMED, WORK_COMPLETED).
 */
export async function updateJobStatus(jobId, status, notes = '') {
  return request(`/operators/jobs/${jobId}/status`, {
    method: 'PUT',
    body: JSON.stringify({
      status,
      notes,
    }),
  });
}

