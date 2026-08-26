import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Loader2 } from 'lucide-react';

/**
 * ProtectedRoute Component
 * Guards routes against unauthenticated users and unauthorized roles.
 */
export default function ProtectedRoute({ allowedRoles = [] }) {
  const { isAuthenticated, isLoading, role } = useAuth();
  const location = useLocation();

  // 1. Wait for initial authentication state initialization
  if (isLoading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50">
        <Loader2 className="w-10 h-10 animate-spin text-emerald-600 mb-3" />
        <p className="text-sm font-medium text-slate-600">Verifying session...</p>
      </div>
    );
  }

  // 2. Redirect unauthenticated users to Login, preserving requested location state
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 3. Perform Role-Based Access Control (RBAC) checks
  if (allowedRoles.length > 0 && (!role || !allowedRoles.includes(role))) {
    console.warn(`Access denied for role '${role}' to path '${location.pathname}'`);
    if (role === 'FARMER') {
      return <Navigate to="/farmer/dashboard" replace />;
    } else if (role === 'ADMIN') {
      return <Navigate to="/admin/dashboard" replace />;
    } else if (role === 'PARTNER') {
      return <Navigate to="/partner/dashboard" replace />;
    } else if (role === 'OPERATOR') {
      return <Navigate to="/operator/dashboard" replace />;
    }
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
