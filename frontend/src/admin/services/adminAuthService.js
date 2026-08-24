import { request } from '../../services/apiClient';
import { setAuthToken, clearAuth, getAuthToken, getUserRole } from '../../utils/auth';

/**
 * Admin authentication — reuses the shared /api/auth/login endpoint and the
 * app's existing generic session storage (utils/auth.js), rather than a
 * separate token scheme apiClient's Bearer-token attachment wouldn't read.
 */
export const adminAuthService = {
  async login(email, password) {
    const data = await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        mobileOrEmail: email,
        password,
        loginType: 'PASSWORD',
      }),
    });

    if (!data || data.role !== 'ADMIN') {
      throw new Error('This account does not have administrator access.');
    }

    setAuthToken(data.token);
    localStorage.setItem('agro_user', JSON.stringify(data));

    return data;
  },

  isAuthenticated() {
    return Boolean(getAuthToken()) && getUserRole() === 'ADMIN';
  },

  logout() {
    clearAuth();
  },
};

export default adminAuthService;
