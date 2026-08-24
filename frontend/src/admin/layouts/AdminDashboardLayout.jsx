import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  AlertTriangle,
  CalendarRange,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  Tractor,
  Users,
  X,
} from 'lucide-react';
import { adminAuthService } from '../services/adminAuthService';
import { getCurrentUser } from '../../services/authService';

const NAV_ITEMS = [
  { label: 'Overview', to: '/admin/overview', icon: LayoutDashboard, end: true },
  { label: 'User Management', to: '/admin/farmers', icon: Users },
  { label: 'Equipment Approvals', to: '/admin/equipment', icon: Tractor },
  { label: 'Bookings', to: '/admin/bookings', icon: CalendarRange },
  { label: 'Disputes', to: '/admin/complaints', icon: AlertTriangle },
];

const EASE = [0.22, 1, 0.36, 1];

function AdminDashboardLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [checked, setChecked] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const admin = getCurrentUser();

  useEffect(() => {
    if (!adminAuthService.isAuthenticated()) {
      navigate('/admin/login', { replace: true });
      return;
    }
    setChecked(true);
  }, [navigate]);

  const handleLogout = () => {
    adminAuthService.logout();
    navigate('/admin/login', { replace: true });
  };

  if (!checked) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#03050a] text-white/40">
        Verifying session…
      </div>
    );
  }

  const sidebarContent = (
    <>
      <div className="flex items-center justify-between px-1">
        <Link to="/admin/overview" className="flex items-center gap-2.5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl border border-emerald-400/20 bg-emerald-400/10 text-emerald-300">
            <ShieldCheck className="h-4.5 w-4.5" />
          </span>
          <span className="font-display text-base font-bold text-white">AgroRent Admin</span>
        </Link>
        <button
          type="button"
          onClick={() => setMobileOpen(false)}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-white/50 hover:text-white lg:hidden"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <nav className="mt-8 flex-1 space-y-1">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all duration-200 ease-out ${
                isActive
                  ? 'bg-emerald-400/10 text-emerald-300 shadow-[inset_0_0_0_1px_rgba(16,185,129,0.25)]'
                  : 'text-white/50 hover:bg-white/5 hover:text-white/80'
              }`
            }
          >
            <item.icon className="h-[18px] w-[18px]" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-6 space-y-3 border-t border-white/10 pt-5">
        <div className="rounded-xl border border-white/10 bg-white/5 px-3.5 py-3">
          <p className="truncate text-sm font-semibold text-white">{admin?.fullName || 'Administrator'}</p>
          <p className="truncate text-xs text-white/40">{admin?.email}</p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center justify-center gap-2 rounded-xl border border-white/10 bg-white/5 py-2.5 text-sm font-semibold text-white/70 transition-all duration-200 ease-out hover:border-red-400/30 hover:bg-red-500/10 hover:text-red-300"
        >
          <LogOut className="h-4 w-4" />
          Log Out
        </button>
      </div>
    </>
  );

  return (
    <div className="relative min-h-screen bg-[#03050a]">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_15%_0%,rgba(16,185,129,0.12),transparent_55%)]" />
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_90%_100%,rgba(132,204,22,0.08),transparent_50%)]" />

      <div className="relative flex min-h-screen">
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col border-r border-white/10 bg-white/[0.02] p-5 backdrop-blur-2xl lg:flex">
          {sidebarContent}
        </aside>

        <AnimatePresence>
          {mobileOpen && (
            <>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setMobileOpen(false)}
                className="fixed inset-0 z-40 bg-black/60 lg:hidden"
              />
              <motion.aside
                initial={{ x: -280 }}
                animate={{ x: 0 }}
                exit={{ x: -280 }}
                transition={{ duration: 0.3, ease: EASE }}
                className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r border-white/10 bg-[#03050a] p-5 backdrop-blur-2xl lg:hidden"
              >
                {sidebarContent}
              </motion.aside>
            </>
          )}
        </AnimatePresence>

        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-3 border-b border-white/10 bg-white/[0.02] px-4 py-3.5 backdrop-blur-xl lg:hidden">
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/10 text-white/70"
            >
              <Menu className="h-4.5 w-4.5" />
            </button>
            <span className="font-display text-sm font-bold text-white">AgroRent Admin</span>
          </div>

          <main className="p-5 sm:p-8">
            <AnimatePresence mode="wait">
              <motion.div
                key={location.pathname}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={{ duration: 0.3, ease: EASE }}
              >
                <Outlet />
              </motion.div>
            </AnimatePresence>
          </main>
        </div>
      </div>
    </div>
  );
}

export default AdminDashboardLayout;
