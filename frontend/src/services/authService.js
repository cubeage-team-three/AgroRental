import { apiRequest } from './api';

export const loginUser = async (credentials) => {
  return await apiRequest('/auth/login', 'POST', credentials);
};

export const loginWithOtp = async (credentials) => {
  return await apiRequest('/auth/login-otp', 'POST', credentials);
};

export const saveUserSession = (userData) => {
  if (userData && userData.token) {
    localStorage.setItem('agro_token', userData.token);
    localStorage.setItem('agro_user', JSON.stringify(userData));
    if (userData.partnerId) {
      localStorage.setItem('partnerId', String(userData.partnerId));
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
  if (user && user.partnerId) {
    return user.partnerId;
  }
  const storedId = localStorage.getItem('partnerId');
  return storedId ? Number(storedId) : 1;
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
  localStorage.removeItem('agro_user');
  localStorage.removeItem('partnerId');
};
