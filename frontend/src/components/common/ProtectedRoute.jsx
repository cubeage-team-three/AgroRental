import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getLoginPath } from '../../utils/auth';
import { Loader2 } from 'lucide-react';

/**
 * ProtectedRoute Component
 * Guards routes against unauthenticated users and unauthorized roles.
 * Signed-out users (including a session that just expired with a 401) go to the login page of the
 * area they tried to open, e.g. /admin/login for admin routes.
 */
export default function ProtectedRoute({ allowedRoles = [] }) {
  const { isAuthenticated, isLoading, role, sessionExpired } = useAuth();
  const location = useLocation();
  const loginPath = getLoginPath(allowedRoles.length === 1 ? allowedRoles[0] : null);

  // 1. Wait for initial authentication state initialization
  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
        <Loader2 className="w-10 h-10 animate-spin text-emerald-600 mb-3" />
        <p className="text-sm font-medium text-slate-600">Verifying session...</p>
      </div>
    );
  }

  // 2. Redirect unauthenticated users to their Login page, preserving requested location state
  if (!isAuthenticated) {
    const state = sessionExpired ? { from: location, sessionExpired: true } : { from: location };
    return <Navigate to={loginPath} state={state} replace />;
  }

  // 3. Perform Role-Based Access Control (RBAC) checks
  if (allowedRoles.length > 0 && (!role || !allowedRoles.includes(role))) {
    console.warn(`Access denied for role '${role}' to path '${location.pathname}'`);
    if (role === 'FARMER') {
      return <Navigate to="/farmer/dashboard" replace />;
    } else if (role === 'ADMIN') {
      return <Navigate to="/admin/overview" replace />;
    } else if (role === 'PARTNER') {
      return <Navigate to="/partner/dashboard" replace />;
    } else if (role === 'OPERATOR') {
      return <Navigate to="/operator/dashboard" replace />;
    }
    return <Navigate to={loginPath} replace />;
  }

  return <Outlet />;
}
