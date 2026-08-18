import { NavLink, useLocation, useNavigate, Link } from 'react-router-dom';
import { getAuthUser, clearAuth, isAuthenticated } from '../../utils/auth';
import agroRentLogo from '../../assets/images/agrorent-logo.png';

function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const user = getAuthUser();
  const authenticated = isAuthenticated();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const isOperatorRoute = location.pathname.startsWith('/operator');
  const isFarmerRoute = location.pathname.startsWith('/farmer');
  const isPartnerRoute = location.pathname.startsWith('/partner');
  const isAdminRoute = location.pathname.startsWith('/admin');

  const operatorSections = [
    {
      title: "Core",
      items: [
        { to: '/operator/dashboard', label: 'Dashboard', icon: '📊' },
      ]
    },
    {
      title: "Jobs & Deployments",
      items: [
        { to: '/operator/jobs', label: 'Assigned Jobs', icon: '🚜' },
        { to: '/operator/history', label: 'Job History', icon: '📜' },
      ]
    },
    {
      title: "Field Operations",
      items: [
        { to: '/operator/work', label: 'Active Job Progress', icon: '⚡' },
        { to: '/operator/gps', label: 'GPS Check-In', icon: '📍' },
      ]
    },
    {
      title: "Financials & Reviews",
      items: [
        { to: '/operator/earnings', label: 'My Earnings', icon: '💰' },
        { to: '/operator/ratings', label: 'Ratings & Reviews', icon: '⭐' },
      ]
    },
    {
      title: "Account & Profile",
      items: [
        { to: '/operator/profile', label: 'My Profile', icon: '👤' },
        { to: '/operator/documents', label: 'Documents & KYC', icon: '🪪' },
        { to: '/operator/notifications', label: 'Notifications', icon: '🔔' },
      ]
    }
  ];

  const farmerNavItems = [
    { to: '/farmer/dashboard', label: 'Dashboard', icon: '🌾' },
    { to: '/farmer/profile', label: 'Profile', icon: '👤' },
    { to: '/farmer/equipment', label: 'Search Equipment', icon: '🔍' },
    { to: '/farmer/bookings', label: 'My Bookings', icon: '📋' },
    { to: '/farmer/farms', label: 'My Farms', icon: '🚜' },
  ];

  const partnerNavItems = [
    { to: '/partner/dashboard', label: 'Dashboard', icon: '📈' },
    { to: '/partner/profile', label: 'Profile', icon: '👤' },
    { to: '/partner/equipment', label: 'My Equipment', icon: '🚜' },
    { to: '/partner/equipment/add', label: 'Add Equipment', icon: '➕' },
    { to: '/partner/bookings', label: 'Booking Requests', icon: '📋' },
  ];

  const adminNavItems = [
    { to: '/admin/dashboard', label: 'Dashboard', icon: '🛡️' },
    { to: '/admin/operators', label: 'Manage Operators', icon: '🚜' },
    { to: '/admin/farmers', label: 'Manage Farmers', icon: '🌾' },
    { to: '/admin/partners', label: 'Manage Partners', icon: '🤝' },
    { to: '/admin/equipment', label: 'Manage Equipment', icon: '🚜' },
  ];

  let portalTitle = 'Operator Portal';
  if (isFarmerRoute) portalTitle = 'Farmer Portal';
  else if (isPartnerRoute) portalTitle = 'Partner Portal';
  else if (isAdminRoute) portalTitle = 'Admin Console';

  return (
    <aside className="w-64 bg-[#0C1F13] text-white min-h-screen flex flex-col justify-between border-r border-emerald-900/40 shadow-xl shrink-0">
      <div>
        {/* Brand Header */}
        <div className="p-5 border-b border-emerald-900/50 flex items-center gap-3">
          <Link to="/" className="flex items-center gap-2">
            <img src={agroRentLogo} alt="AgroRent" className="h-10 w-auto" />
            <div>
              <span className="block font-black text-sm text-lime-400 tracking-tight">
                AGRO RENTAL
              </span>
              <span className="block text-[10px] text-emerald-300 font-semibold uppercase tracking-wider">
                {portalTitle}
              </span>
            </div>
          </Link>
        </div>

        {/* Navigation Links */}
        <div className="p-4 space-y-4 max-h-[calc(100vh-180px)] overflow-y-auto custom-scrollbar">
          {isOperatorRoute || (!isFarmerRoute && !isPartnerRoute && !isAdminRoute) ? (
            operatorSections.map((section, idx) => (
              <div key={idx} className="space-y-1">
                <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-400/60 px-3">
                  {section.title}
                </p>
                <nav className="flex flex-col gap-1">
                  {section.items.map((item) => (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      className={({ isActive }) =>
                        `flex items-center gap-3 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all duration-150 ${
                          isActive
                            ? 'bg-lime-400 text-emerald-950 shadow-md font-bold'
                            : 'text-emerald-100/80 hover:bg-emerald-900/50 hover:text-white'
                        }`
                      }
                    >
                      <span className="text-base">{item.icon}</span>
                      <span>{item.label}</span>
                    </NavLink>
                  ))}
                </nav>
              </div>
            ))
          ) : (
            <nav className="flex flex-col gap-1">
              {(isFarmerRoute ? farmerNavItems : isPartnerRoute ? partnerNavItems : adminNavItems).map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-semibold transition-all duration-150 ${
                      isActive
                        ? 'bg-lime-400 text-emerald-950 shadow-md font-bold'
                        : 'text-emerald-100/80 hover:bg-emerald-900/50 hover:text-white'
                    }`
                  }
                >
                  <span className="text-base">{item.icon}</span>
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </nav>
          )}
        </div>
      </div>

      {/* Footer / User Profile Summary */}
      <div className="p-4 border-t border-emerald-900/50 bg-[#08170D]">
        {authenticated && user ? (
          <div className="space-y-3">
            <Link
              to="/operator/profile"
              className="flex items-center gap-3 p-2 rounded-xl hover:bg-emerald-900/40 transition group"
            >
              <div className="w-9 h-9 rounded-xl bg-lime-400/20 text-lime-300 border border-lime-400/30 flex items-center justify-center font-bold text-sm">
                {user.fullName?.charAt(0) || '👤'}
              </div>
              <div className="overflow-hidden flex-1">
                <p className="text-xs font-bold text-white truncate group-hover:text-lime-300 transition">
                  {user.fullName || 'Operator'}
                </p>
                <p className="text-[10px] text-emerald-300/80 truncate">
                  +91 {user.mobileNumber}
                </p>
              </div>
            </Link>

            <button
              type="button"
              onClick={handleLogout}
              className="w-full bg-emerald-900/60 hover:bg-red-900/40 text-emerald-200 hover:text-red-200 text-xs font-semibold py-2 px-3 rounded-xl border border-emerald-800/60 transition flex items-center justify-center gap-2"
            >
              <span>🚪</span>
              <span>Sign Out</span>
            </button>
          </div>
        ) : (
          <Link
            to="/login"
            className="block text-center bg-lime-400 text-emerald-950 text-xs font-bold py-2.5 px-4 rounded-xl shadow transition hover:bg-lime-300"
          >
            Log In to Portal
          </Link>
        )}
      </div>
    </aside>
  );
}

export default Sidebar;
