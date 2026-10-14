import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  AlertTriangle,
  CalendarRange,
  Handshake,
  LayoutDashboard,
  LogOut,
  Menu,
  ShieldCheck,
  Tractor,
  UserCheck,
  Users,
  X,
} from 'lucide-react';
import { adminAuthService } from '../services/adminAuthService';
import { getCurrentUser } from '../../services/authService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

const NAV_ITEMS = [
  { label: 'Overview', to: '/admin/overview', icon: LayoutDashboard, end: true },
  { label: 'Farmers', to: '/admin/farmers', icon: Users },
  { label: 'Partners', to: '/admin/partners', icon: Handshake },
  { label: 'Operators', to: '/admin/operators', icon: UserCheck },
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
      <div className="flex min-h-screen items-center justify-center bg-[#F8FAF8] text-slate-500 font-medium">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Verifying admin session…</p>
        </div>
      </div>
    );
  }

  const sidebarContent = (
    <>
      <div className="space-y-5">
        {/* Brand Header */}
        <div className="flex items-center justify-between px-2 py-2 border-b border-emerald-800/60">
          <Link to="/admin/overview" className="flex items-center gap-3 group">
            <div className="flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1.5 shadow-md ring-1 ring-slate-900/10 h-12 shrink-0">
              <img
                src={agroRentLogo}
                alt="AgroRent Admin"
                className="h-full w-auto object-contain"
              />
            </div>
            <div>
              <h1 className="text-base font-extrabold tracking-tight text-white drop-shadow-sm">AgroRent</h1>
              <span className="text-[10px] font-extrabold text-lime-400 uppercase tracking-wider block -mt-0.5">
                Admin Console
              </span>
            </div>
          </Link>
          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-emerald-300 hover:text-white lg:hidden hover:bg-emerald-800/50"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Admin Profile Mini Card */}
        <div className="bg-[#194D3E] p-3 rounded-2xl border border-emerald-700/50 flex items-center gap-3 shadow-inner">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-lime-400 to-emerald-600 text-emerald-950 font-extrabold flex items-center justify-center text-lg shadow-md shrink-0">
            {(admin?.fullName || 'Admin').charAt(0).toUpperCase()}
          </div>
          <div className="overflow-hidden flex-1">
            <h3 className="text-sm font-extrabold text-white truncate">{admin?.fullName || 'Administrator'}</h3>
            <p className="text-[11px] text-emerald-200 font-medium truncate">{admin?.email || 'admin@agrorent.com'}</p>
            <span className="inline-block mt-0.5 px-2 py-0.5 bg-lime-400/20 text-lime-300 rounded text-[9px] font-extrabold border border-lime-400/30">
              ✓ Super Admin
            </span>
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="space-y-1">
          <span className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-emerald-300/70 block mb-2">
            Management Console
          </span>
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setMobileOpen(false)}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-bold transition-all duration-150 ${
                  isActive
                    ? 'bg-lime-400 text-emerald-950 shadow-md border border-lime-300/40 translate-x-1 font-extrabold'
                    : 'text-emerald-100/90 hover:bg-emerald-800/50 hover:text-white'
                }`
              }
            >
              <item.icon className="h-4 w-4 shrink-0" />
              <span className="truncate">{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Logout Footer Button */}
      <div className="pt-4 border-t border-emerald-800/60 mt-auto">
        <button
          type="button"
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500/20 hover:bg-red-600 text-red-100 hover:text-white rounded-xl text-xs font-bold transition-all duration-150 border border-red-500/30 shadow-sm"
        >
          <LogOut className="h-4 w-4" />
          <span>Log Out Console</span>
        </button>
      </div>
    </>
  );

  return (
    <div className="min-h-screen bg-[#F8FAF8] text-slate-800 font-sans">
      <div className="flex min-h-screen">
        <aside className="sticky top-0 hidden h-screen w-64 shrink-0 flex-col justify-between border-r border-emerald-950/40 bg-[#0F382C] p-4 text-white shadow-xl lg:flex">
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
                className="fixed inset-0 z-40 bg-black/60 lg:hidden backdrop-blur-xs"
              />
              <motion.aside
                initial={{ x: -280 }}
                animate={{ x: 0 }}
                exit={{ x: -280 }}
                transition={{ duration: 0.3, ease: EASE }}
                className="fixed inset-y-0 left-0 z-50 flex w-64 flex-col justify-between border-r border-emerald-950/40 bg-[#0F382C] p-4 text-white shadow-2xl lg:hidden"
              >
                {sidebarContent}
              </motion.aside>
            </>
          )}
        </AnimatePresence>

        <div className="min-w-0 flex-1 flex flex-col">
          <div className="flex items-center justify-between border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur-md lg:hidden shadow-2xs">
            <button
              type="button"
              onClick={() => setMobileOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100"
            >
              <Menu className="h-4.5 w-4.5" />
            </button>
            <div className="flex items-center gap-2">
              <div className="flex items-center overflow-hidden rounded-xl bg-white px-2 py-1 shadow-sm ring-1 ring-slate-900/10 h-9 shrink-0">
                <img
                  src={agroRentLogo}
                  alt="AgroRent Admin"
                  className="h-full w-auto object-contain"
                />
              </div>
              <span className="text-sm font-extrabold text-slate-900">AgroRent Admin</span>
            </div>
            <div className="w-9" />
          </div>

          <main className="flex-1 p-4 sm:p-6 lg:p-8">
            <AnimatePresence mode="wait">
              <motion.div
                key={location.pathname}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.2, ease: EASE }}
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
