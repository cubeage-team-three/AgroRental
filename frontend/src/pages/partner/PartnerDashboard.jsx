import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Tractor,
  CalendarCheck,
  Wallet,
  Clock,
  PlusCircle,
  ToggleLeft,
  User,
  ShieldCheck,
  ArrowRight,
  TrendingUp,
  MapPin,
  CheckCircle2,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import { getCurrentUser, getPartnerId } from '../../services/authService';
import { partnerService } from '../../services/partnerService';
import { equipmentService } from '../../services/equipmentService';
import { bookingService } from '../../services/bookingService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
  getStatusBadgeInfo,
} from '../../utils/constants';

function PartnerDashboard() {
  const navigate = useNavigate();
  const currentUser = getCurrentUser();
  const partnerId = getPartnerId();

  const [dashboardData, setDashboardData] = useState(null);
  const [equipmentList, setEquipmentList] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadDashboard = async () => {
    if (!partnerId) {
      setError('No partner session found. Please log in again.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setError('');

    try {
      const [dashRes, eqRes, bkRes] = await Promise.all([
        partnerService.getDashboard(partnerId),
        equipmentService.getPartnerEquipment(partnerId),
        bookingService.getBookingsByPartner(partnerId),
      ]);

      if (dashRes) {
        setDashboardData(dashRes);
      }
      if (Array.isArray(eqRes)) {
        setEquipmentList(eqRes);
      }
      if (Array.isArray(bkRes)) {
        setBookings(bkRes);
      }
    } catch (err) {
      console.error('Failed to load partner dashboard data:', err);
      setError(err.response?.data?.message || err.message || 'Failed to load partner dashboard metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, [partnerId]);

  const partnerName = dashboardData?.fullName || currentUser?.fullName || 'Partner';
  const businessName = dashboardData?.businessName || currentUser?.businessName || 'Agro Machinery Fleet';
  const verificationStatus = dashboardData?.verificationStatus || currentUser?.accountStatus || 'APPROVED';

  const totalEquipmentCount = dashboardData?.totalMachines ?? equipmentList.length;
  const activeMachinesCount = dashboardData?.activeMachines ?? equipmentList.filter((e) => e.availabilityStatus === 'AVAILABLE' && !e.isDisabled).length;
  const pendingRequestsCount = dashboardData?.pendingBookings ?? bookings.filter((b) => b.status === 'PENDING').length;
  const completedBookingsCount = dashboardData?.completedBookings ?? bookings.filter((b) => b.status === 'COMPLETED').length;
  const monthlyRevenue = dashboardData?.monthlyRevenue ?? 0;
  const customerRating = dashboardData?.customerRatings ?? 0.0;
  const activeOperators = dashboardData?.activeOperators ?? 0;

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={loadDashboard}
            className="text-xs font-bold text-red-800 underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* Hero Welcome Banner */}
      <div className="bg-white rounded-3xl shadow-lg border border-gray-100 overflow-hidden">
        <div className="relative h-44 sm:h-52 bg-gradient-to-r from-[#142E1C] via-[#1B4D3E] to-[#2E7D32] overflow-hidden flex items-center px-6 sm:px-10">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-25 mix-blend-overlay"
          />
          <div className="relative z-10 w-full flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-white">
            <div>
              <div className="flex items-center gap-2 mb-1.5">
                <span className="px-3 py-0.5 bg-white/20 backdrop-blur-md text-lime-300 rounded-full text-xs font-bold uppercase tracking-wider border border-white/20 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>Partner Portal</span>
                </span>
                <span className="px-2.5 py-0.5 bg-emerald-500 text-white rounded-full text-[11px] font-black uppercase">
                  ✓ {verificationStatus}
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
                Welcome back, {partnerName}! 👋
              </h1>
              <p className="text-xs sm:text-sm text-emerald-100 mt-1 font-medium">
                {businessName} • Monitor rental fleet availability, incoming bookings, and revenue.
              </p>
            </div>

            {/* Quick CTAs */}
            <div className="flex items-center gap-2.5 shrink-0">
              <Link
                to="/partner/equipment/add"
                className="px-4 py-2.5 bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center gap-1.5"
              >
                <PlusCircle className="w-4 h-4" />
                <span>Add Equipment</span>
              </Link>
              <Link
                to="/partner/profile"
                className="px-4 py-2.5 bg-white/15 hover:bg-white/25 backdrop-blur-md text-white font-bold text-xs sm:text-sm rounded-xl border border-white/20 transition-all flex items-center gap-1.5"
              >
                <User className="w-4 h-4" />
                <span>My Profile</span>
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* KPI Metric Cards Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        
        {/* Total Equipment */}
        <Link
          to="/partner/equipment"
          className="bg-white p-5 sm:p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center group-hover:scale-110 transition-transform">
              <Tractor className="w-6 h-6" />
            </div>
            <span className="text-xs font-bold text-[#3E7B27] flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Fleet →
            </span>
          </div>
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Total Machinery</span>
          <p className="text-2xl sm:text-3xl font-black text-gray-900 mt-1">{totalEquipmentCount}</p>
          <span className="text-[11px] text-emerald-600 font-semibold mt-1 block">
            {activeMachinesCount} Active & Ready for Rent
          </span>
        </Link>

        {/* Monthly Revenue */}
        <Link
          to="/partner/earnings"
          className="bg-white p-5 sm:p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Wallet className="w-6 h-6" />
            </div>
            <span className="text-xs font-bold text-amber-600 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Payouts →
            </span>
          </div>
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Monthly Revenue</span>
          <p className="text-2xl sm:text-3xl font-black text-gray-900 mt-1">
            ₹{Number(monthlyRevenue).toLocaleString('en-IN')}
          </p>
          <span className="text-[11px] text-amber-600 font-semibold mt-1 block flex items-center gap-1">
            <TrendingUp className="w-3.5 h-3.5" /> Live Revenue
          </span>
        </Link>

        {/* Pending Requests */}
        <Link
          to="/partner/bookings"
          className="bg-white p-5 sm:p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Clock className="w-6 h-6" />
            </div>
            <span className="text-xs font-bold text-purple-600 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Review →
            </span>
          </div>
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Pending Requests</span>
          <p className="text-2xl sm:text-3xl font-black text-gray-900 mt-1">{pendingRequestsCount}</p>
          <span className="text-[11px] text-purple-600 font-semibold mt-1 block">
            {completedBookingsCount} Completed Orders
          </span>
        </Link>

        {/* Customer Rating */}
        <Link
          to="/partner/reviews"
          className="bg-white p-5 sm:p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <CalendarCheck className="w-6 h-6" />
            </div>
            <span className="text-xs font-bold text-blue-600 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
              Reviews →
            </span>
          </div>
          <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Customer Rating</span>
          <p className="text-2xl sm:text-3xl font-black text-gray-900 mt-1">
            {customerRating > 0 ? `${customerRating.toFixed(1)} ★` : '0.0 ★'}
          </p>
          <span className="text-[11px] text-blue-600 font-semibold mt-1 block">
            {activeOperators} Certified Operators Available
          </span>
        </Link>

      </div>

      {/* Quick Action Hub */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 space-y-4">
        <h2 className="text-lg font-black text-[#142E1C] flex items-center gap-2">
          <span>⚡</span> Quick Management Actions
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          
          <Link
            to="/partner/equipment/add"
            className="p-5 rounded-2xl border border-gray-200 hover:border-[#3E7B27] hover:bg-[#F8FAF8] transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <PlusCircle className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">
              List New Machinery
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              Add tractors, harvesters, seeders or sprayers.
            </p>
          </Link>

          <Link
            to="/partner/equipment/availability"
            className="p-5 rounded-2xl border border-gray-200 hover:border-[#3E7B27] hover:bg-[#F8FAF8] transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-amber-50 text-amber-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <ToggleLeft className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">
              Fleet Availability
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              Toggle operational states & maintenance schedules.
            </p>
          </Link>

          <Link
            to="/partner/bookings"
            className="p-5 rounded-2xl border border-gray-200 hover:border-[#3E7B27] hover:bg-[#F8FAF8] transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-blue-50 text-blue-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <CalendarCheck className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">
              Booking Requests
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              Review and approve incoming farmer rental inquiries.
            </p>
          </Link>

          <Link
            to="/partner/profile"
            className="p-5 rounded-2xl border border-gray-200 hover:border-[#3E7B27] hover:bg-[#F8FAF8] transition-all group"
          >
            <div className="w-10 h-10 rounded-xl bg-purple-50 text-purple-700 flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
              <User className="w-5 h-5" />
            </div>
            <h3 className="text-sm font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">
              Partner Profile & KYC
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              Update enterprise address, GST, and security.
            </p>
          </Link>

        </div>
      </div>

      {/* Fleet Machinery Overview Section */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-black text-[#142E1C] flex items-center gap-2">
              <Tractor className="w-5 h-5 text-[#3E7B27]" />
              <span>Fleet Machinery Overview</span>
            </h2>
            <p className="text-xs text-gray-500 mt-0.5">
              Live snapshot of your registered equipment listings.
            </p>
          </div>
          <Link
            to="/partner/equipment"
            className="text-xs font-bold text-[#3E7B27] hover:underline flex items-center gap-1"
          >
            <span>View All ({equipmentList.length})</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {equipmentList.length === 0 ? (
          <div className="p-8 border border-dashed border-gray-200 rounded-2xl text-center space-y-3">
            <Tractor className="w-10 h-10 text-gray-300 mx-auto" />
            <p className="text-sm font-bold text-gray-700">No machinery listings added yet</p>
            <Link
              to="/partner/equipment/add"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#3E7B27] text-white text-xs font-bold rounded-xl"
            >
              <PlusCircle className="w-4 h-4" />
              <span>Add Your First Machine</span>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {equipmentList.slice(0, 3).map((item) => {
              const badge = getStatusBadgeInfo(item.availabilityStatus);
              const imageSrc = item.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

              return (
                <div
                  key={item.id}
                  className="bg-[#F8FAF8] rounded-2xl border border-emerald-900/5 overflow-hidden flex flex-col justify-between hover:shadow-md transition-shadow"
                >
                  <div>
                    <div className="relative h-36 w-full bg-gray-100 overflow-hidden">
                      <img
                        src={imageSrc}
                        alt={item.name}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                        }}
                      />
                      <span
                        className={`absolute top-2.5 right-2.5 text-[10px] font-black uppercase px-2 py-0.5 rounded-full border shadow-xs ${badge.badgeClass}`}
                      >
                        {badge.label}
                      </span>
                    </div>

                    <div className="p-4 space-y-2">
                      <span className="text-[10px] font-black uppercase text-[#3E7B27]">
                        {formatCategoryLabel(item.category)}
                      </span>
                      <h4 className="text-sm font-bold text-gray-900 line-clamp-1">{item.name}</h4>
                      <p className="text-xs text-gray-500 flex items-center gap-1 truncate">
                        <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                        <span>{item.locationAddress}</span>
                      </p>
                      <div className="pt-2 border-t border-gray-200/60 flex items-center justify-between">
                        <span className="text-base font-black text-[#142E1C]">
                          ₹{Number(item.rentalPrice).toLocaleString('en-IN')} <span className="text-[10px] text-gray-500 font-normal">/ day</span>
                        </span>
                        <span className="text-xs font-semibold text-gray-500">{item.brand} {item.model}</span>
                      </div>
                    </div>
                  </div>

                  <div className="p-3 bg-white border-t border-gray-100 flex items-center justify-between">
                    <button
                      onClick={() => navigate(`/partner/equipment/add?edit=${item.id}`)}
                      className="text-xs font-bold text-gray-700 hover:text-[#3E7B27]"
                    >
                      Edit Machine
                    </button>
                    <Link
                      to="/partner/equipment"
                      className="text-xs font-bold text-[#3E7B27] hover:underline"
                    >
                      Manage Listing →
                    </Link>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

    </div>
  );
}

export default PartnerDashboard;