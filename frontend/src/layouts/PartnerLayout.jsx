import { useState, useEffect } from 'react';
import { NavLink, Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  User,
  Tractor,
  PlusCircle,
  ToggleLeft,
  CalendarCheck,
  HardHat,
  Wallet,
  Star,
  Bell,
  LogOut,
  Menu,
  X,
  Search,
  ChevronRight,
  ShieldCheck,
  BadgeCheck,
  Clock,
  Sparkles,
} from 'lucide-react';
import { getCurrentUser, logoutUser, getPartnerId } from '../services/authService';
import { partnerService } from '../services/partnerService';
import { notificationService } from '../services/notificationService';
import agroRentLogo from '../assets/images/agrorent-logo.jpeg';

function PartnerLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [partner, setPartner] = useState(null);
  const [unreadCount, setUnreadCount] = useState(0);

  const currentUser = getCurrentUser();
  const partnerId = getPartnerId();

  useEffect(() => {
    // Fetch latest partner details
    partnerService
      .getProfile(partnerId)
      .then((data) => {
        if (data) setPartner(data);
      })
      .catch((err) => {
        console.warn('Partner profile fetch fallback in layout:', err.message);
      });

    const fetchUnreadCount = () => {
      notificationService
        .getUnreadCount('PARTNER', partnerId)
        .then((count) => setUnreadCount(count || 0))
        .catch(() => {});
    };

    fetchUnreadCount();

    window.addEventListener('notificationsUpdated', fetchUnreadCount);
    return () => window.removeEventListener('notificationsUpdated', fetchUnreadCount);
  }, [partnerId, location.pathname]);

  // Close mobile drawer on route change
  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  const handleLogout = () => {
    logoutUser();
    navigate('/login');
  };

  const partnerName = partner?.fullName || currentUser?.fullName || 'Partner Owner';
  const businessName = partner?.businessName || currentUser?.businessName || 'Agro Equipment Fleet';
  const verificationStatus = partner?.verificationStatus || currentUser?.accountStatus || 'PENDING';
  const profilePhoto = partner?.profilePhoto || null;

  const navItems = [
    { path: '/partner/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/partner/profile', label: 'Business Profile', icon: User },
    { path: '/partner/equipment', label: 'My Equipment', icon: Tractor },
    { path: '/partner/equipment/add', label: 'Add Equipment', icon: PlusCircle },
    { path: '/partner/equipment/availability', label: 'Fleet Availability', icon: ToggleLeft },
    { path: '/partner/bookings', label: 'Booking Requests', icon: CalendarCheck, badge: 'Demo' },
    { path: '/partner/earnings', label: 'Earnings & Payouts', icon: Wallet, badge: 'Demo' },
    { path: '/partner/reviews', label: 'Reviews & Ratings', icon: Star, badge: 'Demo' },
    { path: '/partner/notifications', label: 'Notifications', icon: Bell },
  ];

  return (
    <div className="min-h-screen bg-[#F7F6F0] flex font-sans antialiased text-gray-900">
      
      {/* Mobile Drawer Backdrop */}
      {mobileMenuOpen && (
        <div
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-xs transition-opacity"
        />
      )}

      {/* Sidebar Navigation */}
      <aside
        className={`fixed lg:sticky top-0 left-0 z-50 h-screen w-72 bg-[#142E1C] text-white flex flex-col justify-between p-4 shadow-2xl border-r border-emerald-900/40 transition-transform duration-300 ease-in-out shrink-0 overflow-y-auto ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="space-y-5">
          
          {/* Brand Header */}
          <div className="flex items-center justify-between px-2 py-2 border-b border-emerald-800/60">
            <Link to="/partner/dashboard" className="flex items-center gap-3 group">
              <div className="flex items-center overflow-hidden rounded-xl bg-white px-2 py-1 shadow-sm ring-1 ring-slate-900/5 h-11 shrink-0">
                <img
                  src={agroRentLogo}
                  alt="AgroRent Marketplace"
                  className="h-full w-auto object-contain"
                />
              </div>
              <div>
                <div className="flex items-center gap-1.5">
                  <h1 className="text-lg font-extrabold tracking-tight text-white">AgroRent</h1>
                  <span className="px-1.5 py-0.5 bg-lime-400 text-emerald-950 rounded text-[9px] font-black uppercase tracking-wider">
                    Partner
                  </span>
                </div>
                <span className="text-[10px] font-semibold text-emerald-300 block -mt-0.5">
                  Equipment Hub
                </span>
              </div>
            </Link>

            {/* Mobile close button */}
            <button
              onClick={() => setMobileMenuOpen(false)}
              className="lg:hidden p-1.5 text-emerald-300 hover:text-white rounded-lg hover:bg-emerald-800/50"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Partner Mini Profile Card */}
          <Link
            to="/partner/profile"
            className="block bg-[#1E3F29] hover:bg-[#254F34] p-3.5 rounded-2xl border border-emerald-700/40 transition-all shadow-inner group"
          >
            <div className="flex items-center gap-3">
              <div className="relative">
                <div className="w-12 h-12 rounded-xl overflow-hidden bg-gradient-to-br from-emerald-500 to-teal-700 text-white font-extrabold flex items-center justify-center text-lg shadow-md border border-white/20 shrink-0">
                  {profilePhoto ? (
                    <img
                      src={profilePhoto}
                      alt={partnerName}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.style.display = 'none';
                      }}
                    />
                  ) : (
                    <span>{partnerName.charAt(0).toUpperCase()}</span>
                  )}
                </div>
                {verificationStatus === 'APPROVED' ? (
                  <BadgeCheck className="w-4 h-4 text-lime-400 absolute -bottom-1 -right-1 bg-[#1E3F29] rounded-full" />
                ) : (
                  <Clock className="w-4 h-4 text-amber-400 absolute -bottom-1 -right-1 bg-[#1E3F29] rounded-full" />
                )}
              </div>

              <div className="overflow-hidden flex-1">
                <h3 className="text-sm font-bold text-white truncate group-hover:text-lime-300 transition-colors">
                  {partnerName}
                </h3>
                <p className="text-[11px] text-emerald-200 font-medium truncate">{businessName}</p>
                <div className="mt-1 flex items-center gap-1.5">
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-black uppercase tracking-wider ${
                      verificationStatus === 'APPROVED'
                        ? 'bg-emerald-500/20 text-emerald-200 border border-emerald-500/30'
                        : 'bg-amber-500/20 text-amber-200 border border-amber-500/30'
                    }`}
                  >
                    {verificationStatus === 'APPROVED' ? '✓ Verified' : '⏳ Pending'}
                  </span>
                </div>
              </div>
            </div>
          </Link>

          {/* Navigation Menu */}
          <nav className="space-y-1">
            <span className="px-3 text-[10px] font-black uppercase tracking-widest text-emerald-400/80 block mb-2">
              Partner Management
            </span>

            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 ${
                      isActive
                        ? 'bg-[#3E7B27] text-white shadow-md border border-lime-400/30 translate-x-1'
                        : 'text-emerald-100/80 hover:bg-emerald-800/40 hover:text-white'
                    }`
                  }
                >
                  <div className="flex items-center gap-3 truncate">
                    <Icon className="w-4 h-4 shrink-0" />
                    <span className="truncate">{item.label}</span>
                  </div>
                  {item.badge && (
                    <span className="px-1.5 py-0.5 bg-emerald-950/70 text-emerald-300 text-[9px] font-bold rounded">
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </nav>

        </div>

        {/* Sidebar Footer */}
        <div className="pt-4 mt-6 border-t border-emerald-800/60 space-y-2">
          
          <Link
            to="/partner/equipment/add"
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-lime-400 hover:bg-lime-300 text-emerald-950 rounded-xl text-xs font-extrabold shadow-md transition-all duration-150"
          >
            <PlusCircle className="w-4 h-4" />
            <span>+ List Machinery</span>
          </Link>

          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500/15 hover:bg-red-600 text-red-200 hover:text-white rounded-xl text-xs font-bold transition-all duration-150 border border-red-500/20 shadow-xs"
          >
            <LogOut className="w-4 h-4" />
            <span>Log Out</span>
          </button>

        </div>

      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        
        {/* Top Navbar */}
        <header className="sticky top-0 z-30 bg-white/90 backdrop-blur-md border-b border-gray-200/80 shadow-xs px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between gap-4">
          
          {/* Mobile hamburger & Breadcrumb info */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden p-2 rounded-xl border border-gray-200 text-gray-700 hover:bg-gray-100"
              aria-label="Open menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div className="hidden sm:flex items-center gap-2 text-xs font-semibold text-gray-500">
              <Link to="/partner/dashboard" className="hover:text-[#3E7B27]">Partner Portal</Link>
              <ChevronRight className="w-3.5 h-3.5 text-gray-400" />
              <span className="text-gray-900 font-bold capitalize">
                {location.pathname.split('/')[2] || 'Dashboard'}
              </span>
            </div>
          </div>

          {/* Right Action Icons & Profile Pill */}
          <div className="flex items-center gap-3">
            
            {/* Direct Add Machinery CTA */}
            <Link
              to="/partner/equipment/add"
              className="hidden sm:inline-flex items-center gap-1.5 px-3.5 py-2 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl shadow-xs transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Add Equipment</span>
            </Link>

            {/* Notifications Shortcut */}
            <Link
              to="/partner/notifications"
              className="relative p-2.5 rounded-xl border border-gray-200 hover:bg-gray-100 text-gray-600 transition-colors"
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-4 h-4 px-1 bg-red-500 text-white text-[9px] font-black rounded-full flex items-center justify-center ring-2 ring-white">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>

            {/* Partner Avatar Dropdown Pill */}
            <Link
              to="/partner/profile"
              className="flex items-center gap-2.5 pl-2 pr-3 py-1.5 rounded-xl border border-gray-200 hover:border-[#3E7B27] hover:bg-emerald-50/40 transition-all group"
            >
              <div className="w-8 h-8 rounded-lg overflow-hidden bg-[#1B4D3E] text-white font-bold text-xs flex items-center justify-center">
                {profilePhoto ? (
                  <img src={profilePhoto} alt={partnerName} className="w-full h-full object-cover" />
                ) : (
                  <span>{partnerName.charAt(0).toUpperCase()}</span>
                )}
              </div>
              <div className="text-left hidden md:block">
                <span className="text-xs font-bold text-gray-900 block leading-tight group-hover:text-[#2E6F22]">
                  {partnerName}
                </span>
                <span className="text-[10px] text-gray-500 font-medium">Owner Profile</span>
              </div>
            </Link>

          </div>

        </header>

        {/* Page Content Outlet */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>

      </div>

    </div>
  );
}

export default PartnerLayout;
