import { getCurrentUser } from '../services/authService';

/**
 * Authentication and authorization utility functions.
 */
export const isAuthenticated = () => {
  return !!getAuthToken() || !!getCurrentUser();
};

export const getAuthUser = () => {
  return getCurrentUser();
};

export const getStoredUser = () => {
  return getCurrentUser();
};

export const getAuthToken = () => {
  try {
    const storedUserStr = localStorage.getItem('agro_user');
    if (storedUserStr) {
      const parsedUser = JSON.parse(storedUserStr);
      if (parsedUser?.token || parsedUser?.accessToken) {
        return parsedUser.token || parsedUser.accessToken;
      }
    }
  } catch (e) {
    // Ignore JSON parse errors
  }
  return localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
};

export const setAuthToken = (token) => {
  if (token) {
    localStorage.setItem('agro_token', token);
    localStorage.setItem('accessToken', token);
  }
};

export const clearAuth = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('agro_token');
  localStorage.removeItem('agro_user');
  localStorage.removeItem('operatorId');
  localStorage.removeItem('partnerId');
  localStorage.removeItem('farmerId');
};

export const getUserRole = () => {
  const user = getCurrentUser();
  return user?.role || null;
};

export const isOperator = () => {
  return getUserRole() === 'OPERATOR';
};

export const getOperatorId = () => {
  const user = getCurrentUser();
  if (user && user.id && user.role === 'OPERATOR') {
    return user.id;
  }
  const stored = localStorage.getItem('operatorId');
  return stored ? Number(stored) : null;
};

export const saveOperatorSession = (loginResponseData) => {
  if (!loginResponseData) return;
  const token = loginResponseData.accessToken || loginResponseData.token;
  if (token) {
    setAuthToken(token);
  }
  const operator = loginResponseData.operator || loginResponseData;
  if (operator) {
    localStorage.setItem('agro_user', JSON.stringify({
      ...operator,
      role: 'OPERATOR',
    }));
    if (operator.id) {
      localStorage.setItem('operatorId', String(operator.id));
    }
  }
};

export const logoutOperator = () => {
  clearAuth();
};
