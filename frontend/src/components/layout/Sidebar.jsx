import { NavLink, useNavigate } from 'react-router-dom';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';
import { getCurrentUser, logoutUser } from '../../services/authService';
import { useLanguage } from '../../context/LanguageContext';

function Sidebar() {
  const navigate = useNavigate();
  let langCtx;
  try {
    langCtx = useLanguage();
  } catch (e) {
    langCtx = {};
  }
  const t = langCtx?.t || ((k, d) => d || k);
  const currentUser = getCurrentUser();
  const farmerName = currentUser?.fullName || 'Ramesh Yadav';
  const mobileNumber = currentUser?.mobileNumber || '9876543210';

  const handleLogout = () => {
    logoutUser();
    navigate('/login');
  };

  const navItems = [
    { path: '/farmer/dashboard', labelKey: 'menu_dashboard', fallback: 'Dashboard', icon: '📊' },
    { path: '/farmer/profile', labelKey: 'menu_profile', fallback: 'My Profile', icon: '👤' },
    { path: '/farmer/farms', labelKey: 'menu_my_farms', fallback: 'My Farms', icon: '🌾' },
    { path: '/farmer/equipment', labelKey: 'menu_search_equipment', fallback: 'Search Equipment', icon: '🚜' },
    { path: '/farmer/bookings', labelKey: 'menu_my_bookings', fallback: 'My Bookings', icon: '📋' },
    { path: '/farmer/payments', labelKey: 'menu_payments', fallback: 'Payments & Invoices', icon: '💳' },
    { path: '/farmer/complaints', labelKey: 'menu_support', fallback: 'Support & Issues', icon: '🎧' },
    { path: '/farmer/notifications', labelKey: 'menu_notifications', fallback: 'Notifications', icon: '🔔' },
  ];

  return (
    <aside className="w-64 bg-[#0F382C] text-white min-h-screen flex flex-col justify-between p-4 shadow-xl border-r border-emerald-950/40 font-sans shrink-0">
      <div className="space-y-6">
        
        {/* Official Brand Logo Header */}
        <div className="flex items-center gap-3 px-2 py-2 border-b border-emerald-800/60">
          <div className="flex items-center overflow-hidden rounded-xl bg-white px-2 py-1 shadow-sm ring-1 ring-slate-900/5 h-11 shrink-0">
            <img
              src={agroRentLogo}
              alt="AgroRent Marketplace"
              className="h-full w-auto object-contain"
            />
          </div>
          <div>
            <h1 className="text-lg font-extrabold tracking-tight text-white drop-shadow-sm">AgroRent</h1>
            <span className="text-[10px] font-extrabold text-lime-400 uppercase tracking-wider block -mt-0.5">
              Farmer Portal
            </span>
          </div>
        </div>

        {/* User Profile Summary Card */}
        <div className="bg-[#194D3E] p-3 rounded-2xl border border-emerald-700/50 flex items-center gap-3 shadow-inner">
          <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-lime-400 to-emerald-600 text-emerald-950 font-extrabold flex items-center justify-center text-lg shadow">
            {farmerName.charAt(0).toUpperCase()}
          </div>
          <div className="overflow-hidden">
            <h3 className="text-sm font-extrabold text-white truncate">{farmerName}</h3>
            <p className="text-[11px] text-emerald-200 font-medium truncate">+91 {mobileNumber}</p>
            <span className="inline-block mt-0.5 px-2 py-0.2 bg-lime-400/20 text-lime-300 rounded text-[9px] font-extrabold border border-lime-400/30">
              ✓ Active Farmer
            </span>
          </div>
        </div>

        {/* Navigation Menu */}
        <nav className="space-y-1">
          <span className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-emerald-300/70 block mb-2">
            Farmer Navigation
          </span>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-150 ${
                  isActive
                    ? 'bg-lime-400 text-emerald-950 shadow-md border border-lime-300/40 translate-x-1 font-extrabold'
                    : 'text-emerald-100/90 hover:bg-emerald-800/50 hover:text-white'
                }`
              }
            >
              <span className="text-base">{item.icon}</span>
              <span className="truncate">{t(item.labelKey, item.fallback)}</span>
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
          <span>Logout Account</span>
        </button>
      </div>

    </aside>
  );
}

export default Sidebar;
