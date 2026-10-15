import { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { X } from 'lucide-react';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';
import { getFarmerId, getOperatorId } from '../../services/authService';
import { notificationService } from '../../services/notificationService';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { getLoginPath } from '../../utils/auth';
import { drawerPanelClasses } from './useMobileDrawer';

/**
 * Farmer / operator navigation. At `lg`+ it is a persistent column; below that it is an off-canvas
 * drawer controlled by DashboardLayout (`open`, `onClose`, `closeButtonRef` from useMobileDrawer),
 * which also renders the backdrop.
 */
function Sidebar({ id, open = false, onClose, closeButtonRef }) {
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();

  let langCtx;
  try {
    langCtx = useLanguage();
  } catch (e) {
    langCtx = {};
  }
  const t = langCtx?.t || ((k, d) => d || k);

  const userName = auth.user?.fullName || 'Loading...';
  const mobileNumber = auth.user?.mobileNumber || '';
  const [unreadCount, setUnreadCount] = useState(0);

  const isOperatorPortal = location.pathname.startsWith('/operator');

  const handleLogout = () => {
    auth.logout();
    navigate(getLoginPath(isOperatorPortal ? 'OPERATOR' : 'FARMER'));
  };

  let portalTitle = 'Farmer Portal';
  let navItems = [
    { path: '/farmer/dashboard', labelKey: 'menu_dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/farmer/profile', labelKey: 'menu_profile', label: 'My Profile', icon: '👤' },
    { path: '/farmer/farms', labelKey: 'menu_my_farms', label: 'My Farms', icon: '🌾' },
    { path: '/farmer/equipment', labelKey: 'menu_search_equipment', label: 'Search Equipment', icon: '🚜' },
    { path: '/farmer/bookings', labelKey: 'menu_my_bookings', label: 'My Bookings', icon: '📋' },
    { path: '/farmer/payments', labelKey: 'menu_payments', label: 'Payments & Invoices', icon: '💳' },
    { path: '/farmer/complaints', labelKey: 'menu_support', label: 'Support & Issues', icon: '🎧' },
    { path: '/farmer/notifications', labelKey: 'menu_notifications', label: 'Notifications', icon: '🔔' },
  ];

  let currentRole = 'FARMER';
  let currentId = getFarmerId();

  if (isOperatorPortal) {
    currentRole = 'OPERATOR';
    currentId = getOperatorId();
    portalTitle = 'Operator Console';
    navItems = [
      { path: '/operator/dashboard', label: 'Dashboard', icon: '📊' },
      { path: '/operator/jobs', label: 'Assigned Jobs', icon: '📋' },
      { path: '/operator/notifications', label: 'Job Alerts', icon: '🔔' },
      { path: '/operator/earnings', label: 'Earnings', icon: '💰' },
      { path: '/operator/history', label: 'Job History', icon: '📜' },
      { path: '/operator/ratings', label: 'Ratings & Reviews', icon: '⭐' },
      { path: '/operator/profile', label: 'Profile', icon: '👤' },
    ];
  } else if (location.pathname.startsWith('/admin')) {
    portalTitle = 'Admin Portal';
    navItems = [
      { path: '/admin/dashboard', label: 'Dashboard', icon: '📊' },
      { path: '/admin/farmers', label: 'Manage Farmers', icon: '🧑‍🌾' },
      { path: '/admin/partners', label: 'Manage Partners', icon: '🤝' },
      { path: '/admin/operators', label: 'Manage Operators', icon: '🚜' },
      { path: '/admin/equipment', label: 'Manage Equipment', icon: '🛠️' },
      { path: '/admin/bookings', label: 'Manage Bookings', icon: '📋' },
      { path: '/admin/payments', label: 'Manage Payments', icon: '💳' },
    ];
  }

  useEffect(() => {
    notificationService
      .getUnreadCount(currentRole, currentId)
      .then((count) => setUnreadCount(count || 0))
      .catch(() => {});
  }, [currentRole, currentId, location.pathname]);

  return (
    <aside
      id={id}
      aria-label={`${portalTitle} navigation`}
      className={`${drawerPanelClasses(open)} w-64 max-w-[85vw] bg-[#0F382C] text-white flex flex-col justify-between p-4 shadow-xl border-r border-emerald-950/40 font-sans`}
    >
      <div className="space-y-6">

        {/* Official Brand Logo Header */}
        <div className="flex items-center gap-3 px-2 py-2 border-b border-emerald-800/60">
          <div className="flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1.5 shadow-md ring-1 ring-slate-900/10 h-12 shrink-0">
            <img
              src={agroRentLogo}
              alt="AgroRent Marketplace"
              className="h-full w-auto object-contain"
            />
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="text-base font-extrabold tracking-tight text-white drop-shadow-sm">AgroRent</h1>
            <span className="text-[10px] font-extrabold text-lime-400 uppercase tracking-wider block -mt-0.5">
              {portalTitle}
            </span>
          </div>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            aria-label="Close navigation menu"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-emerald-300 hover:bg-emerald-800/50 hover:text-white focus-visible:outline-2 focus-visible:outline-lime-400 lg:hidden"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        {/* User Mini Profile Card */}
        <div className="bg-[#194D3E] p-3 rounded-2xl border border-emerald-700/50 flex items-center gap-3 shadow-inner">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-lime-400 to-emerald-600 text-emerald-950 font-extrabold flex items-center justify-center text-lg shadow">
            {userName.charAt(0).toUpperCase()}
          </div>
          <div className="overflow-hidden">
            <h3 className="text-sm font-extrabold text-white truncate">{userName}</h3>
            <p className="text-[11px] text-emerald-200 font-medium truncate">
              {mobileNumber ? `+91 ${mobileNumber}` : 'No mobile on file'}
            </p>
            <span className="inline-block mt-0.5 px-2 py-0.2 bg-lime-400/20 text-lime-300 rounded text-[9px] font-extrabold border border-lime-400/30">
              ✓ Active Account
            </span>
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="space-y-1" aria-label="Main">
          <span className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-emerald-300/70 block mb-2">
            Navigation Menu
          </span>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={open ? onClose : undefined}
              className={({ isActive }) =>
                `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-150 ${
                  isActive
                    ? 'bg-lime-400 text-emerald-950 shadow-md border border-lime-300/40 translate-x-1 font-extrabold'
                    : 'text-emerald-100/90 hover:bg-emerald-800/50 hover:text-white'
                }`
              }
            >
              <div className="flex items-center gap-3 truncate">
                <span className="text-base">{item.icon}</span>
                <span className="truncate">{item.labelKey ? t(item.labelKey, item.label) : item.label}</span>
              </div>
              {item.path.includes('notifications') && unreadCount > 0 && (
                <span className="px-1.5 py-0.5 bg-red-500 text-white text-[10px] font-black rounded-full shadow-xs">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

      </div>

      {/* Logout Footer Button */}
      <div className="pt-4 mt-6 border-t border-emerald-800/60">
        <button
          type="button"
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500/20 hover:bg-red-600 text-red-100 hover:text-white rounded-xl text-xs font-bold transition-all duration-150 border border-red-500/30 shadow-sm"
        >
          <span aria-hidden="true">🚪</span>
          <span>Logout Account</span>
        </button>
      </div>

    </aside>
  );
}

export default Sidebar;
