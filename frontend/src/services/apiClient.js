import { API_BASE_URL } from '../utils/constants';

/**
 * Core HTTP client for executing REST requests against the Spring Boot backend.
 * Extracts payloads from standard ApiResponse wrappers and handles HTTP errors cleanly.
 */
export async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;

  const defaultHeaders = {
    'Content-Type': 'application/json',
  };

  // Include X-Partner-Id header if partner context is present in options or localStorage
  const partnerId = options.partnerId || localStorage.getItem('partnerId') || '1';
  if (partnerId) {
    defaultHeaders['X-Partner-Id'] = partnerId;
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);

    let data;
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      data = await response.json();
    } else {
      data = await response.text();
    }

    if (!response.ok) {
      const errorMessage = (data && data.message) || `HTTP Error ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    // Return inner data payload from ApiResponse if present
    if (data && typeof data === 'object' && 'data' in data) {
      return data.data;
    }

    return data;
  } catch (err) {
    if (err.status) {
      throw err;
    }
    const networkErr = new Error('Network error or server unreachable. Please check backend status.');
    networkErr.status = 0;
    throw networkErr;
  }
}

export const apiClient = {
  get: (endpoint, options) => request(endpoint, { method: 'GET', ...options }),
  post: (endpoint, body, options) => request(endpoint, { method: 'POST', body: JSON.stringify(body), ...options }),
  put: (endpoint, body, options) => request(endpoint, { method: 'PUT', body: JSON.stringify(body), ...options }),
  delete: (endpoint, options) => request(endpoint, { method: 'DELETE', ...options }),
};
