import { request } from './apiClient';
import { saveOperatorSession, clearAuth } from '../utils/auth';

/**
 * Service for interacting with backend Operator REST APIs.
 */
export const operatorService = {
  /**
   * Retrieves all registered operators.
   * @returns {Promise<Array>} Array of Operator objects
   */
  getAllOperators: async () => {
    return await request('/operators');
  },

  /**
   * Retrieves available operators filtering by optional date range and partner.
   * @param {Object} params - { partnerId, startDate, endDate }
   * @returns {Promise<Array>} Array of available Operator objects
   */
  getAvailableOperators: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.partnerId) query.append('partnerId', params.partnerId);
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/available${queryString}`);
  },

  /**
   * Retrieves operators assigned to a specific partner.
   * @param {number} partnerId - Partner ID
   * @returns {Promise<Array>} Array of Operator objects
   */
  getOperatorsByPartner: async (partnerId) => {
    return await request(`/operators/partner/${partnerId}`);
  },

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

  // ==========================================
  // PHASE 7: GPS / OPERATOR LOCATION TRACKING
  // ==========================================

  /**
   * Starts GPS location tracking for an active assignment (PATCH /api/operators/jobs/:assignmentId/location/start).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorLocationResponse
   */
  startLocationTracking: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/location/start`, {
      method: 'PATCH',
    });
  },

  /**
   * Updates operator GPS coordinates (PATCH /api/operators/jobs/:assignmentId/location).
   * @param {number|string} assignmentId - Assignment ID
   * @param {Object} locationData - { latitude, longitude, accuracy, speed, heading }
   * @returns {Promise<Object>} OperatorLocationResponse
   */
  updateLocation: async (assignmentId, locationData) => {
    return await request(`/operators/jobs/${assignmentId}/location`, {
      method: 'PATCH',
      body: JSON.stringify(locationData),
    });
  },

  /**
   * Retrieves latest recorded GPS location for the assignment (GET /api/operators/jobs/:assignmentId/location).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorLocationResponse
   */
  getLatestLocation: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/location`);
  },

  /**
   * Stops GPS location tracking for an active assignment (PATCH /api/operators/jobs/:assignmentId/location/stop).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorLocationResponse
   */
  stopLocationTracking: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/location/stop`, {
      method: 'PATCH',
    });
  },

  // ==========================================
  // PHASE 8: OPERATOR EARNINGS & WORK HOURS
  // ==========================================

  /**
   * Retrieves work duration and earnings calculation for a specific job assignment (GET /api/operators/jobs/:assignmentId/earnings).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorJobEarningsResponse
   */
  getJobEarnings: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/earnings`);
  },

  /**
   * Retrieves aggregate earnings summary and logged work hours (GET /api/operators/earnings/summary).
   * @returns {Promise<Object>} OperatorEarningsSummaryResponse
   */
  getEarningsSummary: async () => {
    return await request('/operators/earnings/summary');
  },

  /**
   * Retrieves paginated completed jobs earnings history (GET /api/operators/earnings/history).
   * @param {Object} params - { page, size }
   * @returns {Promise<Object>} Page<OperatorEarningsHistoryResponse>
   */
  getEarningsHistory: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page);
    if (params.size !== undefined) query.append('size', params.size);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/earnings/history${queryString}`);
  },

  // ==========================================
  // PHASE 9: OPERATOR RATING & REVIEWS
  // ==========================================

  /**
   * Retrieves overall rating summary and star distribution for an operator (GET /api/operators/:operatorId/ratings/summary).
   * @param {number|string} operatorId - Operator ID
   * @returns {Promise<Object>} OperatorRatingSummaryResponse
   */
  getOperatorRatingSummary: async (operatorId) => {
    return await request(`/operators/${operatorId}/ratings/summary`);
  },

  /**
   * Retrieves rating summary for the authenticated operator (GET /api/operators/me/ratings/summary).
   * @returns {Promise<Object>} OperatorRatingSummaryResponse
   */
  getMyRatingSummary: async () => {
    return await request('/operators/me/ratings/summary');
  },

  /**
   * Retrieves paginated reviews for an operator (GET /api/operators/:operatorId/reviews).
   * @param {number|string} operatorId - Operator ID
   * @param {Object} params - { page, size }
   * @returns {Promise<Object>} Page<OperatorReviewResponse>
   */
  getOperatorReviews: async (operatorId, params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page);
    if (params.size !== undefined) query.append('size', params.size);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/${operatorId}/reviews${queryString}`);
  },

  /**
   * Retrieves paginated feedback reviews for the authenticated operator (GET /api/operators/me/reviews).
   * @param {Object} params - { page, size }
   * @returns {Promise<Object>} Page<OperatorReviewResponse>
   */
  getMyReviews: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page);
    if (params.size !== undefined) query.append('size', params.size);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/me/reviews${queryString}`);
  },

  /**
   * Submits a verified star rating and review for a completed assignment (POST /api/operators/jobs/:assignmentId/reviews).
   * @param {number|string} assignmentId - Assignment ID
   * @param {Object} reviewData - { rating, comment }
   * @returns {Promise<Object>} OperatorReviewResponse
   */
  submitOperatorReview: async (assignmentId, reviewData) => {
    return await request(`/operators/jobs/${assignmentId}/reviews`, {
      method: 'POST',
      body: JSON.stringify(reviewData),
    });
  },

  /**
   * Retrieves review associated with a specific job assignment (GET /api/operators/jobs/:assignmentId/review).
   * @param {number|string} assignmentId - Assignment ID
   * @returns {Promise<Object>} OperatorReviewResponse
   */
  getAssignmentReview: async (assignmentId) => {
    return await request(`/operators/jobs/${assignmentId}/review`);
  },

  // ==========================================
  // PHASE 10: JOB HISTORY & ANALYTICS METHODS
  // ==========================================

  /**
   * Retrieves a paginated, filterable archive of historical jobs (GET /api/operators/jobs/history).
   * @param {Object} params - { page, size, startDate, endDate, status, equipmentCategory, search }
   * @returns {Promise<Object>} Page<OperatorJobHistoryResponse>
   */
  getJobHistory: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page);
    if (params.size !== undefined) query.append('size', params.size);
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);
    if (params.status && params.status !== 'ALL') query.append('status', params.status);
    if (params.equipmentCategory && params.equipmentCategory !== 'ALL') query.append('equipmentCategory', params.equipmentCategory);
    if (params.search && params.search.trim()) query.append('search', params.search.trim());

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/jobs/history${queryString}`);
  },

  /**
   * Retrieves aggregate historical field performance analytics (GET /api/operators/jobs/history/summary).
   * @param {Object} params - { startDate, endDate, equipmentCategory }
   * @returns {Promise<Object>} OperatorJobHistorySummaryResponse
   */
  getJobHistorySummary: async (params = {}) => {
    const query = new URLSearchParams();
    if (params.startDate) query.append('startDate', params.startDate);
    if (params.endDate) query.append('endDate', params.endDate);
    if (params.equipmentCategory && params.equipmentCategory !== 'ALL') query.append('equipmentCategory', params.equipmentCategory);

    const queryString = query.toString() ? `?${query.toString()}` : '';
    return await request(`/operators/jobs/history/summary${queryString}`);
  },
};

export default operatorService;




