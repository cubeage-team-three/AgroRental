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
    { path: '/partner/bookings', label: 'Booking Requests', icon: CalendarCheck },
    { path: '/partner/earnings', label: 'Earnings & Payouts', icon: Wallet },
    { path: '/partner/reviews', label: 'Reviews & Ratings', icon: Star },
    { path: '/partner/notifications', label: 'Notifications', icon: Bell },
  ];

  return (
    <div className="min-h-screen bg-[#F8FAF8] flex font-sans antialiased text-slate-800">
      
      {/* Mobile Drawer Backdrop */}
      {mobileMenuOpen && (
        <div
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 bg-black/50 z-40 lg:hidden backdrop-blur-xs transition-opacity"
        />
      )}

      {/* Sidebar Navigation */}
      <aside
        className={`fixed lg:sticky top-0 left-0 z-50 h-screen w-72 bg-[#0F382C] text-white flex flex-col justify-between p-4 shadow-xl border-r border-emerald-950/40 transition-transform duration-300 ease-in-out shrink-0 overflow-y-auto ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="space-y-5">
          
          {/* Brand Header */}
          <div className="flex items-center justify-between px-2 py-2 border-b border-emerald-800/60">
            <Link to="/partner/dashboard" className="flex items-center gap-3 group">
              <div className="flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1.5 shadow-md ring-1 ring-slate-900/10 h-12 shrink-0">
                <img
                  src={agroRentLogo}
                  alt="AgroRent Marketplace"
                  className="h-full w-auto object-contain"
                />
              </div>
              <div>
                <h1 className="text-base font-extrabold tracking-tight text-white drop-shadow-sm">AgroRent</h1>
                <span className="text-[10px] font-extrabold text-lime-400 uppercase tracking-wider block -mt-0.5">
                  Partner Portal
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
            className="block bg-[#194D3E] hover:bg-[#1f5c4b] p-3 rounded-2xl border border-emerald-700/50 transition-all shadow-inner group"
          >
            <div className="flex items-center gap-3">
              <div className="relative">
                <div className="w-11 h-11 rounded-xl overflow-hidden bg-gradient-to-br from-lime-400 to-emerald-600 text-emerald-950 font-extrabold flex items-center justify-center text-lg shadow-md shrink-0">
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
                  <BadgeCheck className="w-4 h-4 text-lime-400 absolute -bottom-1 -right-1 bg-[#194D3E] rounded-full" />
                ) : (
                  <Clock className="w-4 h-4 text-amber-400 absolute -bottom-1 -right-1 bg-[#194D3E] rounded-full" />
                )}
              </div>

              <div className="overflow-hidden flex-1">
                <h3 className="text-sm font-extrabold text-white truncate group-hover:text-lime-300 transition-colors">
                  {partnerName}
                </h3>
                <p className="text-[11px] text-emerald-200 font-medium truncate">{businessName}</p>
                <div className="mt-1 flex items-center gap-1.5">
                  <span
                    className={`inline-block px-2 py-0.5 rounded text-[9px] font-extrabold border ${
                      verificationStatus === 'APPROVED'
                        ? 'bg-lime-400/20 text-lime-300 border-lime-400/30'
                        : 'bg-amber-400/20 text-amber-300 border-amber-400/30'
                    }`}
                  >
                    {verificationStatus === 'APPROVED' ? '✓ Verified Partner' : '⏳ Verification Pending'}
                  </span>
                </div>
              </div>
            </div>
          </Link>

          {/* Navigation Menu */}
          <nav className="space-y-1">
            <span className="px-3 text-[10px] font-extrabold uppercase tracking-widest text-emerald-300/70 block mb-2">
              Partner Management
            </span>

            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-all duration-150 ${
                      isActive
                        ? 'bg-lime-400 text-emerald-950 shadow-md border border-lime-300/40 translate-x-1 font-extrabold'
                        : 'text-emerald-100/90 hover:bg-emerald-800/50 hover:text-white'
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
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-red-500/20 hover:bg-red-600 text-red-100 hover:text-white rounded-xl text-xs font-bold transition-all duration-150 border border-red-500/30 shadow-sm"
          >
            <LogOut className="w-4 h-4" />
            <span>Log Out</span>
          </button>

        </div>

      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        
        {/* Top Navbar */}
        <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-2xs px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between gap-4">
          
          {/* Mobile hamburger & Breadcrumb info */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden p-2 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-100"
              aria-label="Open menu"
            >
              <Menu className="w-5 h-5" />
            </button>

            <div className="hidden sm:flex items-center gap-2 text-xs font-semibold text-slate-500">
              <Link to="/partner/dashboard" className="hover:text-emerald-700 font-bold">Partner Portal</Link>
              <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
              <span className="text-slate-900 font-bold capitalize">
                {location.pathname.split('/')[2]?.replace('-', ' ') || 'Dashboard'}
              </span>
            </div>
          </div>

          {/* Right Action Icons & Profile Pill */}
          <div className="flex items-center gap-3">
            
            {/* Direct Add Machinery CTA */}
            <Link
              to="/partner/equipment/add"
              className="hidden sm:inline-flex items-center gap-1.5 px-3.5 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition-all"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Add Equipment</span>
            </Link>

            {/* Notifications Shortcut */}
            <Link
              to="/partner/notifications"
              className="relative p-2.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 transition-colors"
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
              className="flex items-center gap-2.5 pl-2 pr-3 py-1.5 rounded-xl border border-slate-200 hover:border-emerald-600 hover:bg-emerald-50/40 transition-all group"
            >
              <div className="w-8 h-8 rounded-lg overflow-hidden bg-gradient-to-br from-lime-400 to-emerald-600 text-emerald-950 font-extrabold text-xs flex items-center justify-center">
                {profilePhoto ? (
                  <img src={profilePhoto} alt={partnerName} className="w-full h-full object-cover" />
                ) : (
                  <span>{partnerName.charAt(0).toUpperCase()}</span>
                )}
              </div>
              <div className="text-left hidden md:block">
                <span className="text-xs font-bold text-slate-900 block leading-tight group-hover:text-emerald-700">
                  {partnerName}
                </span>
                <span className="text-[10px] text-slate-500 font-medium">Owner Profile</span>
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
