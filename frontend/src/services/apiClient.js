import { API_BASE_URL } from '../utils/constants';

/**
 * Core HTTP client for executing REST requests against the Spring Boot backend.
 * Automatically attaches Authorization Bearer JWT headers and handles errors cleanly.
 */
export async function request(endpoint, options = {}) {
  const cleanEndpoint = endpoint.startsWith('/api') ? endpoint.substring(4) : endpoint;
  const url = `${API_BASE_URL}${cleanEndpoint}`;

  const defaultHeaders = {};
  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  // Automatically attach Bearer token if present
  const token = localStorage.getItem('accessToken') || localStorage.getItem('agro_token');
  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`;
  }

  // Include X-Partner-Id header if partner context is present
  const partnerId = options.partnerId || localStorage.getItem('partnerId');
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
      // If 401 Unauthorized, clear stored token on protected endpoint failures
      if (response.status === 401 && !endpoint.includes('/login') && !endpoint.includes('/register')) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('agro_token');
        localStorage.removeItem('agro_user');
      }

      const errorMessage = (data && data.message) || `HTTP Error ${response.status}`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    // Return inner data payload from standard ApiResponse if present
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
  get: (endpoint, options = {}) => {
    let url = endpoint;
    if (options.params) {
      const query = new URLSearchParams(options.params).toString();
      url = `${endpoint}?${query}`;
    }
    return request(url, { method: 'GET', ...options });
  },
  post: (endpoint, body, options = {}) => request(endpoint, { method: 'POST', body: body ? JSON.stringify(body) : null, ...options }),
  put: (endpoint, body, options = {}) => request(endpoint, { method: 'PUT', body: body ? JSON.stringify(body) : null, ...options }),
  patch: (endpoint, body, options = {}) => {
    let url = endpoint;
    if (options.params) {
      const query = new URLSearchParams(options.params).toString();
      url = `${endpoint}?${query}`;
    }
    return request(url, { method: 'PATCH', body: body ? JSON.stringify(body) : null, ...options });
  },
  delete: (endpoint, options = {}) => request(endpoint, { method: 'DELETE', ...options }),
};
