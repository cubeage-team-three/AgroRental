import { request } from './apiClient';
import { saveOperatorSession, clearAuth } from '../utils/auth';

/**
 * Service for interacting with backend Operator REST APIs.
 */
export const operatorService = {
  /**
   * Registers a new operator account with PENDING verification status.
   * @param {Object} operatorData - Operator registration payload
   * @returns {Promise<Object>} OperatorResponse payload
   */
  registerOperator: async (operatorData) => {
    return await request('/operators/register', {
      method: 'POST',
      body: JSON.stringify(operatorData),
    });
  },

  /**
   * Dispatches an OTP to the Operator's mobile number.
   * @param {string} mobileNumber - 10-digit mobile number
   * @param {string} purpose - Purpose of OTP (e.g. MOBILE_VERIFICATION)
   * @returns {Promise<Object>} OperatorOtpResponse payload
   */
  sendOtp: async (mobileNumber, purpose = 'MOBILE_VERIFICATION') => {
    return await request('/operators/otp/send', {
      method: 'POST',
      body: JSON.stringify({ mobileNumber, purpose }),
    });
  },

  /**
   * Verifies the submitted OTP for the Operator's mobile number.
   * @param {string} mobileNumber - 10-digit mobile number
   * @param {string} otp - 6-digit OTP string
   * @param {string} purpose - Purpose of OTP
   * @returns {Promise<Object>} OperatorOtpResponse payload
   */
  verifyOtp: async (mobileNumber, otp, purpose = 'MOBILE_VERIFICATION') => {
    return await request('/operators/otp/verify', {
      method: 'POST',
      body: JSON.stringify({ mobileNumber, otp, purpose }),
    });
  },

  /**
   * Authenticates an operator using mobile number and password (FR-39).
   * Automatically stores the session token upon successful login.
   * @param {Object} credentials - { mobileNumber, password }
   * @returns {Promise<Object>} OperatorLoginResponse payload
   */
  loginOperator: async (credentials) => {
    const data = await request('/operators/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    if (data && data.accessToken) {
      saveOperatorSession(data);
    }
    return data;
  },

  /**
   * Retrieves the current authenticated Operator's profile identity (GET /api/operators/me).
   * @returns {Promise<Object>} AuthenticatedOperatorResponse payload
   */
  getCurrentOperator: async () => {
    return await request('/operators/me');
  },

  /**
   * Retrieves the full profile of the authenticated Operator (GET /api/operators/profile).
   * @returns {Promise<Object>} OperatorProfileResponse payload
   */
  getProfile: async () => {
    return await request('/operators/profile');
  },

  /**
   * Updates permitted editable profile fields for the authenticated Operator (PUT /api/operators/profile).
   * @param {Object} profileData - { fullName, email, address, experience, skills, profilePhoto }
   * @returns {Promise<Object>} Updated OperatorProfileResponse payload
   */
  updateProfile: async (profileData) => {
    return await request('/operators/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  },

  /**
   * Changes the authenticated Operator's password (PATCH /api/operators/profile/password).
   * @param {Object} passwordData - { currentPassword, newPassword, confirmPassword }
   * @returns {Promise<Object>} Success message payload
   */
  changePassword: async (passwordData) => {
    return await request('/operators/profile/password', {
      method: 'PATCH',
      body: JSON.stringify(passwordData),
    });
  },

  /**
   * Logs out the current operator and clears cached tokens.
   */
  logout: () => {
    clearAuth();
  },

  /**
   * Submits a KYC or certification document metadata record for an Operator.
   * @param {number|string} operatorId - Operator ID
   * @param {Object} documentData - Document metadata
   * @returns {Promise<Object>} OperatorDocumentResponse payload
   */
  uploadDocument: async (operatorId, documentData) => {
    return await request(`/operators/${operatorId}/documents`, {
      method: 'POST',
      body: JSON.stringify(documentData),
    });
  },

  /**
   * Retrieves all KYC documents for an Operator.
   * @param {number|string} operatorId - Operator ID
   * @returns {Promise<Array>} Array of OperatorDocumentResponse payloads
   */
  getDocuments: async (operatorId) => {
    return await request(`/operators/${operatorId}/documents`);
  },

  /**
   * Retrieves paginated assigned jobs for the authenticated Operator (GET /api/operators/jobs/assigned).
   * @param {Object} params - { page, size, sort }
   * @returns {Promise<Object>} Page of OperatorAssignedJobResponse
   */
  getAssignedJobs: async (params = {}) => {
    const queryParams = new URLSearchParams(params).toString();
    const endpoint = `/operators/jobs/assigned${queryParams ? `?${queryParams}` : ''}`;
    return await request(endpoint);
  },

  /**
   * Retrieves specific assigned job details for the authenticated Operator (GET /api/operators/jobs/:assignmentId).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorAssignedJobResponse
   */
  getAssignedJob: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}`);
  },

  /**
   * Searches and retrieves eligible approved operators for assignment (GET /api/operators/eligible).
   * @param {Object} params - { search, page, size }
   * @returns {Promise<Object>} Page of EligibleOperatorResponse
   */
  getEligibleOperators: async (params = {}) => {
    const queryParams = new URLSearchParams(params).toString();
    const endpoint = `/operators/eligible${queryParams ? `?${queryParams}` : ''}`;
    return await request(endpoint);
  },

  /**
   * Assigns an eligible operator to a confirmed booking (POST /api/bookings/:bookingId/operator).
   * @param {number|string} bookingId - Booking ID
   * @param {Object} assignmentData - { operatorId, notes }
   * @returns {Promise<Object>} OperatorAssignmentResponse
   */
  assignOperator: async (bookingId, assignmentData) => {
    return await request(`/bookings/${bookingId}/operator`, {
      method: 'POST',
      body: JSON.stringify(assignmentData),
    });
  },

  /**
   * Retrieves the active operator assignment for a booking (GET /api/bookings/:bookingId/operator).
   * @param {number|string} bookingId - Booking ID
   * @returns {Promise<Object>} OperatorAssignmentResponse
   */
  getBookingAssignment: async (bookingId) => {
    return await request(`/bookings/${bookingId}/operator`);
  },

  // ==========================================
  // PHASE 5: WORK LIFECYCLE MUTATION METHODS
  // ==========================================

  /**
   * Accepts an assigned job (PATCH /api/operators/jobs/:assignmentId/accept).
   */
  acceptJob: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/accept`, {
      method: 'PATCH',
    });
  },

  /**
   * Rejects an assigned job with mandatory reason (PATCH /api/operators/jobs/:assignmentId/reject).
   * @param {number|string} assignmentId - Assignment ID
   * @param {Object} payload - { rejectionReason }
   */
  rejectJob: async (assignmentId, payload) => {
    return await request(`/operators/jobs/${assignmentId}/reject`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Starts traveling to the farm location (PATCH /api/operators/jobs/:assignmentId/start-travel).
   */
  startTravel: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/start-travel`, {
      method: 'PATCH',
    });
  },

  /**
   * Marks arrival at the farm location (PATCH /api/operators/jobs/:assignmentId/reached).
   */
  markReached: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/reached`, {
      method: 'PATCH',
    });
  },

  /**
   * Starts machinery operations / field work (PATCH /api/operators/jobs/:assignmentId/start-work).
   */
  startWork: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/start-work`, {
      method: 'PATCH',
    });
  },

  /**
   * Pauses field work with mandatory reason (PATCH /api/operators/jobs/:assignmentId/pause).
   * @param {number|string} assignmentId - Assignment ID
   * @param {Object} payload - { pauseReason }
   */
  pauseJob: async (assignmentId, payload) => {
    return await request(`/operators/jobs/${assignmentId}/pause`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  /**
   * Resumes field work (PATCH /api/operators/jobs/:assignmentId/resume).
   */
  resumeJob: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/resume`, {
      method: 'PATCH',
    });
  },

  /**
   * Marks field work completed (PATCH /api/operators/jobs/:assignmentId/complete).
   * @param {number|string} assignmentId - Assignment ID
   * @param {Object} [payload] - { completionNotes }
   */
  completeJob: async (assignmentId, payload = {}) => {
    return await request(`/operators/jobs/${assignmentId}/complete`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  // ==========================================
  // PHASE 6: DASHBOARD METRICS METHOD
  // ==========================================

  /**
   * Retrieves aggregated dashboard metrics, status breakdown, and active task for the authenticated Operator.
   * @returns {Promise<Object>} OperatorDashboardMetricsResponse
   */
  getDashboardMetrics: async () => {
    return await request('/operators/dashboard/metrics');
  },
};

export default operatorService;



