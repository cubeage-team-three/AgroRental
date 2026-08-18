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
  }
};

export const getCurrentUser = () => {
  const user = localStorage.getItem('agro_user');
  return user ? JSON.parse(user) : null;
};

export const logoutUser = () => {
  localStorage.removeItem('agro_token');
  localStorage.removeItem('agro_user');
};
