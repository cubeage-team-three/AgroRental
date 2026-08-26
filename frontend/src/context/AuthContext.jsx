import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';
import { getCurrentUser, logoutUser } from '../services/authService';
import { getAuthToken } from '../utils/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    const token = getAuthToken();
    const cachedUser = getCurrentUser();

    if (!token || !cachedUser) {
      setUser(null);
      setLoading(false);
      return;
    }

    // Show cached session data immediately so the UI never has to wait on
    // the network before it can render a real name/mobile/email.
    setUser(cachedUser);

    // Farmer accounts verified via OTP land on the dashboard with only
    // whatever the OTP-verify response carried; re-fetch the authoritative
    // record from the backend so any fields entered after that (or edited
    // elsewhere) are reflected here too.
    if (cachedUser.role === 'FARMER') {
      try {
        const res = await axios.get(`${API_BASE_URL}/farmers/profile`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        const fresh = res.data?.data;
        if (fresh) {
          setUser((prev) => ({ ...prev, ...fresh }));
        }
      } catch (err) {
        const status = err.response?.status;
        if (status === 401 || status === 403) {
          // The stored token is genuinely invalid/expired (not a transient
          // network hiccup) — clear it instead of leaving a phantom "logged
          // in" session that fails on every subsequent authenticated request.
          logoutUser();
          setUser(null);
        } else {
          // Network/server issue — keep showing the cached session data;
          // a failed refresh shouldn't blank out a user who is otherwise
          // validly logged in just because the backend hiccupped.
          console.warn('AuthContext: failed to refresh farmer profile:', err.message);
        }
      }
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  return (
    <AuthContext.Provider value={{ user, setUser, loading, refreshUser }}>
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
