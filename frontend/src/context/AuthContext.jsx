import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { apiClient, SESSION_EXPIRED_EVENT } from '../services/apiClient';
import {
  loginUser as loginApi,
  saveUserSession,
  logoutUser,
  getCurrentUser,
} from '../services/authService';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  // True after the backend rejected the stored session (401), until the next sign-in or logout;
  // lets route guards tell "session expired" apart from "never signed in" / "logged out".
  const [sessionExpired, setSessionExpired] = useState(false);

  // apiClient/api.js clear storage on a 401 and fire this event; mirror it here so protected
  // routes redirect to the right login page instead of rendering a signed-out console.
  useEffect(() => {
    const handleSessionExpired = (event) => {
      setToken(null);
      setUser(null);
      // hadToken=false: the request went out without a session (already signed out elsewhere)
      if (event.detail?.hadToken) setSessionExpired(true);
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
  }, []);

  // Re-derives auth state from localStorage, and for farmers also re-fetches
  // the authoritative profile from the backend — farmer accounts verified via
  // OTP land on the dashboard with only whatever the OTP-verify response
  // carried, so this keeps name/mobile/email fresh without a full reload.
  const refreshUser = useCallback(async () => {
    const storedToken = localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
    const storedUser = getCurrentUser();

    if (!storedToken || !storedUser) {
      setToken(null);
      setUser(null);
      setIsLoading(false);
      return;
    }

    // Show cached session data immediately so the UI never has to wait on
    // the network before it can render a real name/mobile/email.
    setToken(storedToken);
    setUser(storedUser);
    setSessionExpired(false);

    if (storedUser.role === 'FARMER') {
      try {
        const fresh = await apiClient.get('/farmers/profile');
        if (fresh) {
          setUser((prev) => ({ ...prev, ...fresh }));
        }
      } catch (err) {
        const status = err.status || err.response?.status;
        if (status === 401 || status === 403) {
          // The stored token is genuinely invalid/expired (not a transient
          // network hiccup) — clear it instead of leaving a phantom "logged
          // in" session that fails on every subsequent authenticated request.
          logoutUser();
          setToken(null);
          setUser(null);
        } else {
          // Network/server issue — keep showing the cached session data;
          // a failed refresh shouldn't blank out an otherwise validly
          // logged-in user just because the backend hiccupped.
          console.warn('AuthContext: failed to refresh farmer profile:', err.message);
        }
      }
    }

    setIsLoading(false);
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (credentials) => {
    setIsLoading(true);
    try {
      // Password login only: OTP login is no longer supported by the backend
      const response = await loginApi(credentials);

      const userData = response.data || response;
      saveUserSession(userData);

      const newToken = localStorage.getItem('agro_token') || userData.token;
      const currentUser = getCurrentUser() || userData;

      setToken(newToken);
      setUser(currentUser);
      setSessionExpired(false);

      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    logoutUser();
    setToken(null);
    setUser(null);
    setSessionExpired(false);
  };

  const value = {
    user,
    setUser,
    token,
    role: user?.role || null,
    isAuthenticated: Boolean(token && user),
    sessionExpired,
    isLoading,
    loading: isLoading, // alias for callers written against the older name
    login,
    logout,
    refreshUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
