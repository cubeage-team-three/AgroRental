import { apiRequest } from './api';

export const loginUser = async (credentials) => {
  return await apiRequest('/auth/login', 'POST', credentials);
};

export const loginWithOtp = async (credentials) => {
  return await apiRequest('/auth/login-otp', 'POST', credentials);
};

export const saveUserSession = (userData) => {
  if (!userData) return;
  const payload = (userData.data && typeof userData.data === 'object' && (userData.data.token || userData.data.fullName || userData.data.id))
    ? userData.data
    : userData;

  if (payload) {
    if (payload.token) {
      localStorage.setItem('agro_token', payload.token);
    }
    localStorage.setItem('agro_user', JSON.stringify(payload));
    if (payload.partnerId) {
      localStorage.setItem('partnerId', String(payload.partnerId));
    }
    if (payload.farmerId) {
      localStorage.setItem('farmerId', String(payload.farmerId));
    }
    if (payload.operatorId) {
      localStorage.setItem('operatorId', String(payload.operatorId));
    }
  }
};

export const getCurrentUser = () => {
  const user = localStorage.getItem('agro_user');
  return user ? JSON.parse(user) : null;
};

export const isPartner = () => {
  const user = getCurrentUser();
  return user && user.role === 'PARTNER';
};

export const getPartnerId = () => {
  const user = getCurrentUser();
  if (user) {
    if (user.partnerId) return user.partnerId;
    if (user.role === 'PARTNER' && user.id) return user.id;
  }
  const storedId = localStorage.getItem('partnerId');
  return storedId ? Number(storedId) : null;
};

export const getFarmerId = () => {
  const user = getCurrentUser();
  if (user) {
    if (user.farmerId) return user.farmerId;
    if (user.role === 'FARMER' && user.id) return user.id;
  }
  const storedId = localStorage.getItem('farmerId');
  return storedId ? Number(storedId) : 1;
};

export const getOperatorId = () => {
  const user = getCurrentUser();
  if (user && user.operatorId) {
    return user.operatorId;
  }
  const storedId = localStorage.getItem('operatorId');
  return storedId ? Number(storedId) : 1;
};

export const logoutUser = () => {
  localStorage.removeItem('agro_token');
  localStorage.removeItem('accessToken');
  localStorage.removeItem('agro_user');
  localStorage.removeItem('partnerId');
  localStorage.removeItem('farmerId');
  localStorage.removeItem('operatorId');
};
