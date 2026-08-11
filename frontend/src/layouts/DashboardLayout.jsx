import { Outlet } from 'react-router-dom';
import Sidebar from '../components/layout/Sidebar';

function DashboardLayout() {
  return (
    <div className="min-h-screen flex">
      <Sidebar />
      <main className="flex-1 p-6">
        <Outlet />
      </main>
    </div>
  );
}

export default DashboardLayout;
