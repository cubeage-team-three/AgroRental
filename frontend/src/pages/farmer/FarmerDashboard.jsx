import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';
import { getCurrentUser } from '../../services/authService';
import { getFarmerDashboard } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function FarmerDashboard() {
  let langCtx;
  try {
    langCtx = useLanguage();
  } catch (e) {
    langCtx = {};
  }
  const t = langCtx?.t || ((k, d) => d || k);

  const navigate = useNavigate();
  const currentUser = getCurrentUser();
  const farmerId = currentUser?.farmerId || 1;

  const [loading, setLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState({
    profileSummary: {
      fullName: currentUser?.fullName || 'Farmer User',
      mobileNumber: currentUser?.mobileNumber || '',
      accountStatus: currentUser?.accountStatus || 'ACTIVE',
      preferredLanguage: currentUser?.preferredLanguage || 'English',
    },
    totalFarmsCount: 0,
    activeBookingsCount: 0,
    completedBookingsCount: 0,
    totalSpentAmount: 0,
    activeBookingStatusMessage: 'No active bookings currently in progress.',
    recentBookings: [],
  });

  useEffect(() => {
    fetchDashboard();
  }, [farmerId]);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const res = await getFarmerDashboard(farmerId);
      if (res && res.data) {
        setDashboardData(res.data);
      }
    } catch (err) {
      console.warn('Dashboard fetch notice:', err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading farmer portal dashboard...</p>
        </div>
      </div>
    );
  }

  const farmerName = dashboardData.profileSummary?.fullName || 'Farmer';

  return (
    <div className="mx-auto max-w-6xl py-6 px-4 sm:px-8 font-sans space-y-6">
      
      {/* Header Banner Card with Official AgroRent Logo */}
      <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="relative h-44 sm:h-52 bg-gradient-to-r from-[#0F382C] via-[#1B4D3E] to-[#2E7D32] overflow-hidden flex items-center px-6 sm:px-10">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-25 mix-blend-overlay"
          />

          <div className="relative z-10 w-full flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-white">
            <div className="flex items-center gap-4">
              <div className="hidden sm:flex items-center overflow-hidden rounded-2xl bg-white px-3 py-1.5 shadow-md h-14 shrink-0">
                <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
              </div>
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-3 py-0.5 bg-white/20 backdrop-blur-md text-white rounded-full text-xs font-bold uppercase tracking-wider">
                    🌱 Farmer Portal
                  </span>
                  <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-full text-[11px] font-extrabold shadow-sm">
                    ✓ Active Account
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                  Welcome back, {farmerName}! 👋
                </h1>
                <p className="text-xs sm:text-sm text-emerald-100 mt-1">
                  India's Premier Agriculture Machinery & Equipment Marketplace
                </p>
              </div>
            </div>

            {/* Quick Header CTA Buttons */}
            <div className="flex items-center gap-2 shrink-0">
              <Link
                to="/farmer/equipment"
                className="px-4 py-2.5 bg-lime-400 hover:bg-lime-300 text-emerald-950 font-extrabold text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center gap-1.5"
              >
                <span>🚜 Rent Equipment</span>
              </Link>
              <Link
                to="/farmer/profile"
                className="px-4 py-2.5 bg-white/10 hover:bg-white/20 backdrop-blur-md text-white font-bold text-xs sm:text-sm rounded-xl border border-white/20 transition-all flex items-center gap-1.5"
              >
                <span>👤 My Profile</span>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        
        {/* Farms Card */}
        <Link
          to="/farmer/farms"
          className="bg-white p-5 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
              🌾
            </div>
            <span className="text-xs font-bold text-emerald-700 group-hover:translate-x-1 transition-transform">View →</span>
          </div>
          <span className="text-xs uppercase font-bold text-slate-400 block">Registered Farms</span>
          <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-0.5">{dashboardData.totalFarmsCount}</p>
        </Link>

        {/* Active Bookings Card */}
        <Link
          to="/farmer/bookings"
          className="bg-white p-5 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
              🚜
            </div>
            <span className="text-xs font-bold text-blue-600 group-hover:translate-x-1 transition-transform">View →</span>
          </div>
          <span className="text-xs uppercase font-bold text-slate-400 block">Active Bookings</span>
          <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-0.5">{dashboardData.activeBookingsCount}</p>
        </Link>

        {/* Completed Jobs Card */}
        <Link
          to="/farmer/bookings"
          className="bg-white p-5 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
              ⭐
            </div>
            <span className="text-xs font-bold text-purple-600 group-hover:translate-x-1 transition-transform">History →</span>
          </div>
          <span className="text-xs uppercase font-bold text-slate-400 block">Completed Jobs</span>
          <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-0.5">{dashboardData.completedBookingsCount}</p>
        </Link>

        {/* Total Spent Card */}
        <Link
          to="/farmer/payments"
          className="bg-white p-5 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
              💳
            </div>
            <span className="text-xs font-bold text-amber-600 group-hover:translate-x-1 transition-transform">Receipts →</span>
          </div>
          <span className="text-xs uppercase font-bold text-slate-400 block">Total Payments</span>
          <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-0.5">₹{dashboardData.totalSpentAmount?.toLocaleString('en-IN')}</p>
        </Link>

      </div>

      {/* Service Shortcuts Section */}
      <div className="space-y-4">
        <h2 className="text-xl font-extrabold text-slate-900 flex items-center gap-2">
          <span>⚡</span> Farmer Service Shortcuts
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            onClick={() => navigate('/farmer/equipment')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              🚜
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Search & Reserve Equipment</h3>
            <p className="text-xs text-slate-500 mt-1">Browse tractors, harvesters, tillers, and drones near your farm.</p>
            <span className="inline-flex items-center text-xs font-bold text-emerald-700 mt-3 group-hover:translate-x-1 transition-transform">Explore Machinery →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/farms')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              🌾
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Farm Management</h3>
            <p className="text-xs text-slate-500 mt-1">Add and manage your agricultural land plots & crop details.</p>
            <span className="inline-flex items-center text-xs font-bold text-amber-700 mt-3 group-hover:translate-x-1 transition-transform">Manage Farms →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/bookings')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              📋
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Booking Schedule & Status</h3>
            <p className="text-xs text-slate-500 mt-1">Track operator status, field arrival time, and booking history.</p>
            <span className="inline-flex items-center text-xs font-bold text-blue-700 mt-3 group-hover:translate-x-1 transition-transform">View Schedule →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/payments')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              💳
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Online Payments & Invoices</h3>
            <p className="text-xs text-slate-500 mt-1">Process UPI/Card checkout and view downloadable GST tax invoices.</p>
            <span className="inline-flex items-center text-xs font-bold text-purple-700 mt-3 group-hover:translate-x-1 transition-transform">View Invoices →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/live-tracking')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-teal-50 text-teal-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              📡
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Live Machine Tracking</h3>
            <p className="text-xs text-slate-500 mt-1">Real-time GPS tracking for active machines, field arrival & ETA.</p>
            <span className="inline-flex items-center text-xs font-bold text-teal-700 mt-3 group-hover:translate-x-1 transition-transform">Open Live GPS →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/complaints')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              ⚠️
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">Help & Complaints</h3>
            <p className="text-xs text-slate-500 mt-1">Report service disputes, breakdown issues, or billing queries.</p>
            <span className="inline-flex items-center text-xs font-bold text-rose-700 mt-3 group-hover:translate-x-1 transition-transform">Get Support →</span>
          </div>

          <div
            onClick={() => navigate('/farmer/profile')}
            className="bg-white p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all cursor-pointer group"
          >
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
              👤
            </div>
            <h3 className="text-base font-bold text-slate-900 group-hover:text-emerald-700 transition-colors">My Profile & Preferences</h3>
            <p className="text-xs text-slate-500 mt-1">Update contact details, preferred language & security settings.</p>
            <span className="inline-flex items-center text-xs font-bold text-emerald-700 mt-3 group-hover:translate-x-1 transition-transform">Edit Profile →</span>
          </div>
        </div>
      </div>

    </div>
  );
}

export default FarmerDashboard;
