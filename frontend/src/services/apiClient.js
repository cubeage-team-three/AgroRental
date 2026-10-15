import { API_BASE_URL } from '../utils/constants';

/**
 * Window event fired after a 401 ended the stored session. `detail.role` is the role that was signed in;
 * `detail.hadToken` is false when the request carried no token (the session was already gone).
 */
export const SESSION_EXPIRED_EVENT = 'agro:session-expired';

const SESSION_KEYS = ['accessToken', 'agro_token', 'agro_user', 'partnerId', 'farmerId', 'operatorId'];

// Credential checks answer 401 for a wrong password; that must not end an existing session
const isCredentialEndpoint = (endpoint) => endpoint.includes('/login') || endpoint.includes('/register');

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('agro_user') || 'null');
  } catch {
    return null;
  }
}

/**
 * Ends the stored session after the backend answered 401 to a request that carried `rejectedToken`
 * (null when it carried none). Skipped when the stored session uses a different token than the rejected
 * one, e.g. the user signed in again while the request was in flight. Listeners (AuthContext) then send
 * the user to the login page of the area they were in.
 */
export function expireSession(endpoint, rejectedToken) {
  if (isCredentialEndpoint(endpoint)) return;
  const storedUser = readStoredUser();
  const storedTokens = [storedUser?.token, storedUser?.accessToken,
    localStorage.getItem('agro_token'), localStorage.getItem('accessToken')].filter(Boolean);
  if (rejectedToken ? !storedTokens.includes(rejectedToken) : storedTokens.length > 0) return;

  SESSION_KEYS.forEach((key) => localStorage.removeItem(key));
  window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT, {
    detail: { role: storedUser?.role || null, hadToken: Boolean(rejectedToken) },
  }));
}

/**
 * Core HTTP client for executing REST requests against the Spring Boot backend.
 * Automatically attaches Authorization Bearer JWT headers and handles errors cleanly.
 */
export async function request(endpoint, options = {}) {
  const cleanEndpoint = endpoint.startsWith('/api') ? endpoint.substring(4) : endpoint;
  const formattedEndpoint = cleanEndpoint.startsWith('/') ? cleanEndpoint : `/${cleanEndpoint}`;
  const url = `${API_BASE_URL}${formattedEndpoint}`;

  const defaultHeaders = {};
  if (!(options.body instanceof FormData)) {
    defaultHeaders['Content-Type'] = 'application/json';
  }

  // Automatically attach Bearer token belonging to the current active session
  let token = null;
  try {
    const storedUserStr = localStorage.getItem('agro_user');
    if (storedUserStr) {
      const parsedUser = JSON.parse(storedUserStr);
      token = parsedUser?.token || parsedUser?.accessToken || null;
    }
  } catch (e) {
    // Ignore JSON parse error
  }
  if (!token) {
    token = localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
  }


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
      // 401 on a protected endpoint: the session is no longer valid (expired, revoked or tampered).
      // Judge the token actually sent: a caller may override Authorization (e.g. an onboarding token).
      if (response.status === 401) {
        const sentAuth = config.headers.Authorization || config.headers.authorization || '';
        expireSession(endpoint, sentAuth.startsWith('Bearer ') ? sentAuth.slice(7) : null);
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
