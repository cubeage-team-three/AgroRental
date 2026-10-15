import { Outlet, Navigate, useLocation } from 'react-router-dom';
import { Menu } from 'lucide-react';
import Sidebar from '../components/layout/Sidebar';
import { useMobileDrawer, DRAWER_BACKDROP_CLASSES } from '../components/layout/useMobileDrawer';
import { getCurrentUser } from '../services/authService';
import { getLoginPath } from '../utils/auth';
import agroRentLogo from '../assets/images/agrorent-logo.jpeg';

const SIDEBAR_ID = 'dashboard-sidebar';

function DashboardLayout() {
  const location = useLocation();
  const drawer = useMobileDrawer();
  const token = localStorage.getItem('agro_token');
  const user = getCurrentUser();
  const isOperatorPortal = location.pathname.startsWith('/operator');

  if (isOperatorPortal) {
    if (!token || !user || (user.role !== 'OPERATOR' && user.role !== 'ROLE_OPERATOR')) {
      return <Navigate to={getLoginPath('OPERATOR')} replace state={{ from: location }} />;
    }
  }

  return (
    <div className="min-h-screen bg-[#F8FAF8] lg:flex">
      {drawer.open && (
        <div aria-hidden="true" onClick={drawer.closeDrawer} className={DRAWER_BACKDROP_CLASSES} />
      )}

      <Sidebar
        id={SIDEBAR_ID}
        open={drawer.open}
        onClose={drawer.closeDrawer}
        closeButtonRef={drawer.closeButtonRef}
      />

      {/* Page behind an open drawer is inert: no Tab stops or screen-reader access until it closes */}
      <div className="flex min-w-0 flex-1 flex-col" inert={drawer.open || undefined}>
        {/* Small-screen top bar with the menu toggle; at lg+ the sidebar is always visible */}
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-slate-200 bg-white/95 px-4 py-3 shadow-2xs backdrop-blur-md lg:hidden">
          <button
            ref={drawer.menuButtonRef}
            type="button"
            onClick={drawer.openDrawer}
            aria-label="Open navigation menu"
            aria-expanded={drawer.open}
            aria-controls={SIDEBAR_ID}
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100 focus-visible:outline-2 focus-visible:outline-emerald-600"
          >
            <Menu className="h-5 w-5" aria-hidden="true" />
          </button>
          <div className="flex min-w-0 items-center gap-2">
            <span className="flex h-9 shrink-0 items-center overflow-hidden rounded-xl bg-white px-2 py-1 shadow-sm ring-1 ring-slate-900/10">
              <img src={agroRentLogo} alt="" className="h-full w-auto object-contain" />
            </span>
            <span className="truncate text-sm font-extrabold text-slate-900">
              AgroRent {isOperatorPortal ? 'Operator Console' : 'Farmer Portal'}
            </span>
          </div>
        </header>

        <main className="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default DashboardLayout;
