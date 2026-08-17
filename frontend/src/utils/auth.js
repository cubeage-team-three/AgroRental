// Authentication & Token Storage Utilities for AgroRental

const TOKEN_KEY = "agro_auth_token";
const USER_KEY = "agro_auth_user";

export const saveAuth = (token, user) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
};

export const getAuthToken = () => {
  return localStorage.getItem(TOKEN_KEY);
};

export const getAuthUser = () => {
  const user = localStorage.getItem(USER_KEY);
  if (!user) return null;
  try {
    return JSON.parse(user);
  } catch {
    return null;
  }
};

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

export const isAuthenticated = () => {
  return Boolean(getAuthToken());
};
