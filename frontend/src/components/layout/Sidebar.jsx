import { useState, useEffect } from 'react';
import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { getCurrentUser, logoutUser, getFarmerId, getPartnerId, getOperatorId } from '../../services/authService';
import { notificationService } from '../../services/notificationService';
import { useLanguage } from '../../context/LanguageContext';

function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useLanguage();
  const currentUser = getCurrentUser();
  const userName = currentUser?.fullName || 'AgroRental User';
  const mobileNumber = currentUser?.mobileNumber || '9876543210';
  const [unreadCount, setUnreadCount] = useState(0);

  const handleLogout = () => {
    logoutUser();
    navigate('/login');
  };

  let portalTitle = 'Farmer Portal';
  let navItems = [
    { path: '/farmer/dashboard', labelKey: 'menu_dashboard', label: 'Dashboard', icon: '📊' },
    { path: '/farmer/profile', labelKey: 'menu_profile', label: 'Profile', icon: '👤' },
    { path: '/farmer/farms', labelKey: 'menu_my_farms', label: 'My Farms', icon: '🌾' },
    { path: '/farmer/equipment', labelKey: 'menu_search_equipment', label: 'Book Machinery', icon: '🚜' },
    { path: '/farmer/bookings', labelKey: 'menu_my_bookings', label: 'My Bookings', icon: '📋' },
    { path: '/farmer/payments', labelKey: 'menu_payments', label: 'Payments', icon: '💳' },
    { path: '/farmer/complaints', labelKey: 'menu_support', label: 'Support', icon: '🎧' },
    { path: '/farmer/notifications', labelKey: 'menu_notifications', label: 'Notifications', icon: '🔔' },
  ];

  let currentRole = 'FARMER';
  let currentId = getFarmerId();

  if (location.pathname.startsWith('/operator')) {
    currentRole = 'OPERATOR';
    currentId = getOperatorId();
    portalTitle = 'Operator Console';
    navItems = [
      { path: '/operator/dashboard', label: 'Dashboard', icon: '📊' },
      { path: '/operator/jobs', label: 'Assigned Jobs', icon: '📋' },
      { path: '/operator/notifications', label: 'Job Alerts', icon: '🔔' },
      { path: '/operator/earnings', label: 'Earnings', icon: '💰' },
      { path: '/operator/history', label: 'Job History', icon: '📜' },
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
    <aside className="w-64 bg-[#1B4D3E] text-white min-h-screen flex flex-col justify-between p-4 shadow-xl border-r border-emerald-900/30 font-sans shrink-0">
      <div className="space-y-6">
        
        {/* Brand Header */}
        <div className="flex items-center gap-3 px-2 py-3 border-b border-emerald-800/60">
          <div className="w-10 h-10 rounded-2xl bg-[#3E7B27] text-white flex items-center justify-center font-bold text-xl shadow-md border border-white/20">
            🌱
          </div>
          <div>
            <h1 className="text-xl font-extrabold tracking-tight text-white drop-shadow-sm">AgroRent</h1>
            <span className="text-[10px] font-bold text-emerald-200 uppercase tracking-widest block -mt-0.5">
              {portalTitle}
            </span>
          </div>
        </div>

        {/* User Mini Profile Card */}
        <div className="bg-[#265D4D] p-3 rounded-2xl border border-emerald-700/50 flex items-center gap-3 shadow-inner">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-700 text-white font-extrabold flex items-center justify-center text-lg shadow">
            {userName.charAt(0).toUpperCase()}
          </div>
          <div className="overflow-hidden">
            <h3 className="text-sm font-bold text-white truncate">{userName}</h3>
            <p className="text-[11px] text-emerald-200 font-medium truncate">+91 {mobileNumber}</p>
            <span className="inline-block mt-0.5 px-2 py-0.2 bg-emerald-500/30 text-emerald-100 rounded text-[9px] font-extrabold">
              ✓ Active Account
            </span>
          </div>
        </div>

        {/* Main Navigation Menu */}
        <nav className="space-y-1">
          <span className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-emerald-300/70 block mb-2">
            Main Navigation
          </span>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-150 ${
                  isActive
                    ? 'bg-[#3E7B27] text-white shadow-md border border-emerald-400/30 translate-x-1'
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
      <div className="pt-4 border-t border-emerald-800/60">
        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500/20 hover:bg-red-600 text-red-100 hover:text-white rounded-xl text-xs font-bold transition-all duration-150 border border-red-500/30 shadow-sm"
        >
          <span>🚪</span>
          <span>{t('logout')}</span>
        </button>
      </div>

    </aside>
  );
}

export default Sidebar;
