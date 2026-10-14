import { Outlet, Navigate, useLocation } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';
import { getCurrentUser } from '../services/authService';

function DashboardLayout() {
  const location = useLocation();
  const token = localStorage.getItem('agro_token');
  const user = getCurrentUser();

  if (location.pathname.startsWith('/operator')) {
    if (!token || !user || (user.role !== 'OPERATOR' && user.role !== 'ROLE_OPERATOR')) {
      return <Navigate to="/login?role=OPERATOR" replace state={{ from: location }} />;
    }
  }

  return (
    <div className="min-h-screen flex bg-[#F8FAF8]">
      <Sidebar />
      <main className="flex-1 p-4 sm:p-6 lg:p-8 min-w-0">
        <Outlet />
      </main>
    </div>
  );
}

export default DashboardLayout;
