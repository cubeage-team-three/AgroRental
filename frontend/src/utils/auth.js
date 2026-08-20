import { getCurrentUser } from '../services/authService';

/**
 * Authentication and authorization utility functions.
 */
export const isAuthenticated = () => {
  return !!localStorage.getItem('agro_token') || !!getCurrentUser();
};

export const getAuthUser = () => {
  return getCurrentUser();
};

export const getAuthToken = () => {
  return localStorage.getItem('agro_token');
};
