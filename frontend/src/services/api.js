import { API_BASE_URL } from '../utils/constants';

export async function apiRequest(endpoint, method = 'GET', body = null) {
  const headers = {};
  if (!(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const token = localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const partnerId = localStorage.getItem('partnerId');
  if (partnerId) {
    headers['X-Partner-Id'] = partnerId;
  }

  const config = {
    method,
    headers,
  };

  if (body) {
    config.body = body instanceof FormData ? body : JSON.stringify(body);
  }

  const cleanEndpoint = endpoint.startsWith('/api')
    ? endpoint.substring(4)
    : endpoint;

  const baseUrl = API_BASE_URL || 'http://localhost:8080/api';

  const url = `${baseUrl}${cleanEndpoint.startsWith('/')
    ? cleanEndpoint
    : `/${cleanEndpoint}`}`;

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
      const errorMsg =
        (data && data.message) ||
        (data && data.data
          ? JSON.stringify(data.data)
          : `HTTP Error ${response.status}`);

      const error = new Error(errorMsg);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    if (error.status) {
      throw error;
    }

    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error(
        'Network error: Unable to connect to backend server. Please check if the backend service is running on http://localhost:8080.'
      );
    }

    throw error;
  }
}