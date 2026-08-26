import { createContext, useContext, useState, useEffect } from 'react';
import { loginUser as loginApi, loginWithOtp as loginOtpApi, saveUserSession, logoutUser, getCurrentUser } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Initialize auth state on application startup from localStorage
  useEffect(() => {
    try {
      const storedToken = localStorage.getItem('agro_token') || localStorage.getItem('accessToken');
      const storedUser = getCurrentUser();

      if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(storedUser);
      }
    } catch (error) {
      console.error('Failed to initialize auth state:', error);
      logoutUser();
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = async (credentials) => {
    setIsLoading(true);
    try {
      let response;
      if (credentials.loginType === 'OTP') {
        response = await loginOtpApi(credentials);
      } else {
        response = await loginApi(credentials);
      }

      const userData = response.data || response;
      saveUserSession(userData);

      const newToken = localStorage.getItem('agro_token') || userData.token;
      const currentUser = getCurrentUser() || userData;

      setToken(newToken);
      setUser(currentUser);

      return response;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    logoutUser();
    setToken(null);
    setUser(null);
  };

  const value = {
    user,
    token,
    role: user?.role || null,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
