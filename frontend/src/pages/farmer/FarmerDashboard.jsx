import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../../services/authService';
import { getFarmerDashboard } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function FarmerDashboard() {
  const { t } = useLanguage();
  const navigate = useNavigate();
  const currentUser = getCurrentUser();
  const farmerId = currentUser?.farmerId || 1;

  const [loading, setLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState({
    profileSummary: {
      fullName: currentUser?.fullName || 'Ramesh Yadav',
      mobileNumber: currentUser?.mobileNumber || '9876543210',
      accountStatus: currentUser?.accountStatus || 'ACTIVE',
      preferredLanguage: currentUser?.preferredLanguage || 'English',
    },
    totalFarmsCount: 2,
    activeBookingsCount: 1,
    completedBookingsCount: 4,
    totalSpentAmount: 12500,
    activeBookingStatusMessage: 'Mahindra 575 DI Tractor #BK-2026-0891 is confirmed for tomorrow 9:00 AM.',
    recentBookings: [
      {
        bookingId: 'BK-2026-0891',
        equipmentName: 'Mahindra 575 DI Tractor',
        bookingDate: '18 Aug 2026',
        status: 'ACCEPTED',
        totalCost: '₹3,200',
      },
      {
        bookingId: 'BK-2026-0744',
        equipmentName: 'John Deere Rotavator 6ft',
        bookingDate: '12 Aug 2026',
        status: 'COMPLETED',
        totalCost: '₹1,800',
      },
    ],
  });

  useEffect(() => {
    fetchDashboard();
  }, [farmerId]);

  const fetchDashboard = async () => {
    setLoading(true);
    try {
      const res = await getFarmerDashboard(farmerId);
      if (res.data) {
        setDashboardData(res.data);
      }
    } catch (err) {
      console.warn('Dashboard fetch warning (using default dashboard view):', err.message);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F7F6F0] p-6 flex items-center justify-center font-sans">
        <div className="flex items-center gap-3 text-[#3E7B27] font-semibold">
          <svg className="animate-spin h-6 w-6 text-[#3E7B27]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>Loading Farmer Dashboard...</span>
        </div>
      </div>
    );
  }

  const farmerName = dashboardData.profileSummary?.fullName || 'Farmer';

  return (
    <div className="min-h-screen bg-[#F7F6F0] py-6 px-4 sm:px-8 font-sans">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header Banner Card */}
        <div className="bg-white rounded-3xl shadow-lg border border-gray-100 overflow-hidden">
          <div className="relative h-40 sm:h-48 bg-gradient-to-r from-emerald-800 to-green-900 overflow-hidden flex items-center px-6 sm:px-10">
            <img
              src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=80"
              alt="Agro Field Banner"
              className="absolute inset-0 w-full h-full object-cover opacity-35"
            />
            <div className="relative z-10 w-full flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-white">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-3 py-0.5 bg-white/20 backdrop-blur-md text-emerald-100 rounded-full text-xs font-bold uppercase tracking-wider">
                    🌱 {t('farmer_portal')}
                  </span>
                  <span className="px-2.5 py-0.5 bg-emerald-500/80 text-white rounded-full text-[11px] font-extrabold">
                    ✓ {t('active_farmer')}
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                  {t('welcome_back')} {farmerName}! 👋
                </h1>
                <p className="text-xs sm:text-sm text-emerald-100 mt-1">
                  {t('dashboard_subtitle')}
                </p>
              </div>

              {/* Quick Header CTA Buttons */}
              <div className="flex items-center gap-2">
                <Link
                  to="/farmer/equipment"
                  className="px-4 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white font-bold text-xs sm:text-sm rounded-xl shadow-md transition-all flex items-center gap-1.5"
                >
                  <span>🚜 {t('book_equipment')}</span>
                </Link>
                <Link
                  to="/farmer/profile"
                  className="px-4 py-2.5 bg-white/10 hover:bg-white/20 backdrop-blur-md text-white font-bold text-xs sm:text-sm rounded-xl border border-white/20 transition-all flex items-center gap-1.5"
                >
                  <span>👤 {t('my_profile')}</span>
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
            className="bg-white p-5 rounded-3xl shadow-md border border-gray-100 hover:border-[#3E7B27] hover:shadow-lg transition-all duration-200 group"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
                🌾
              </div>
              <span className="text-xs font-bold text-[#3E7B27] group-hover:translate-x-1 transition-transform">View →</span>
            </div>
            <span className="text-xs uppercase font-bold text-gray-400 block">{t('registered_farms')}</span>
            <p className="text-2xl sm:text-3xl font-extrabold text-gray-900 mt-0.5">{dashboardData.totalFarmsCount}</p>
          </Link>

          {/* Active Bookings Card */}
          <Link
            to="/farmer/bookings"
            className="bg-white p-5 rounded-3xl shadow-md border border-gray-100 hover:border-[#3E7B27] hover:shadow-lg transition-all duration-200 group"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
                🚜
              </div>
              <span className="text-xs font-bold text-blue-600 group-hover:translate-x-1 transition-transform">View →</span>
            </div>
            <span className="text-xs uppercase font-bold text-gray-400 block">{t('active_bookings')}</span>
            <p className="text-2xl sm:text-3xl font-extrabold text-gray-900 mt-0.5">{dashboardData.activeBookingsCount}</p>
          </Link>

          {/* Completed Orders Card */}
          <Link
            to="/farmer/bookings"
            className="bg-white p-5 rounded-3xl shadow-md border border-gray-100 hover:border-[#3E7B27] hover:shadow-lg transition-all duration-200 group"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
                ⭐
              </div>
              <span className="text-xs font-bold text-purple-600 group-hover:translate-x-1 transition-transform">History →</span>
            </div>
            <span className="text-xs uppercase font-bold text-gray-400 block">{t('completed_jobs')}</span>
            <p className="text-2xl sm:text-3xl font-extrabold text-gray-900 mt-0.5">{dashboardData.completedBookingsCount}</p>
          </Link>

          {/* Total Spent Card */}
          <Link
            to="/farmer/payments"
            className="bg-white p-5 rounded-3xl shadow-md border border-gray-100 hover:border-[#3E7B27] hover:shadow-lg transition-all duration-200 group"
          >
            <div className="flex items-center justify-between mb-3">
              <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center text-2xl group-hover:scale-110 transition-transform">
                💳
              </div>
              <span className="text-xs font-bold text-amber-600 group-hover:translate-x-1 transition-transform">Receipts →</span>
            </div>
            <span className="text-xs uppercase font-bold text-gray-400 block">{t('total_spent')}</span>
            <p className="text-2xl sm:text-3xl font-extrabold text-gray-900 mt-0.5">₹{dashboardData.totalSpentAmount?.toLocaleString('en-IN')}</p>
          </Link>

        </div>

        {/* Active Booking Spotlight Card */}
        {dashboardData.activeBookingStatusMessage && (
          <div className="bg-gradient-to-r from-[#F1F8EE] to-[#E6F4E2] border-2 border-[#3E7B27]/30 rounded-3xl p-6 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 rounded-2xl bg-[#3E7B27] text-white flex items-center justify-center text-2xl shadow-sm shrink-0">
                📍
              </div>
              <div>
                <span className="px-2.5 py-0.5 bg-emerald-600 text-white rounded-md font-extrabold text-[10px] uppercase tracking-wider">
                  {t('upcoming_booking')}
                </span>
                <p className="text-sm font-bold text-gray-900 mt-1">
                  {dashboardData.activeBookingStatusMessage}
                </p>
              </div>
            </div>

            <Link
              to="/farmer/bookings"
              className="px-5 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-bold rounded-xl shadow transition-all shrink-0"
            >
              {t('live_tracking')}
            </Link>
          </div>
        )}

        {/* Service Access Shortcuts Section */}
        <div className="space-y-4">
          <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
            <span>⚡</span> {t('service_shortcuts')}
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            
            {/* 1. Equipment Search & Booking */}
            <div
              onClick={() => navigate('/farmer/equipment')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                🚜
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('equipment_search_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('equipment_search_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-[#3E7B27] mt-3 group-hover:translate-x-1 transition-transform">{t('explore_machinery')}</span>
            </div>

            {/* 2. Farm Management */}
            <div
              onClick={() => navigate('/farmer/farms')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                🌾
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('farm_mgmt_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('farm_mgmt_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-amber-700 mt-3 group-hover:translate-x-1 transition-transform">{t('manage_farms')}</span>
            </div>

            {/* 3. Booking History */}
            <div
              onClick={() => navigate('/farmer/bookings')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                📋
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('booking_schedule_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('booking_schedule_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-blue-700 mt-3 group-hover:translate-x-1 transition-transform">{t('view_schedule')}</span>
            </div>

            {/* 4. Online Payments & Invoices */}
            <div
              onClick={() => navigate('/farmer/payments')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-purple-50 text-purple-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                💳
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('payments_invoices_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('payments_invoices_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-purple-700 mt-3 group-hover:translate-x-1 transition-transform">{t('invoices_receipts')}</span>
            </div>

            {/* 5. Ratings & Reviews */}
            <div
              onClick={() => navigate('/farmer/bookings')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                ⭐
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('ratings_reviews_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('ratings_reviews_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-amber-600 mt-3 group-hover:translate-x-1 transition-transform">{t('rate_services')}</span>
            </div>

            {/* 6. Complaint Management */}
            <div
              onClick={() => navigate('/farmer/complaints')}
              className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:border-[#3E7B27] hover:shadow-md transition-all cursor-pointer group"
            >
              <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition-transform">
                🎧
              </div>
              <h3 className="text-base font-bold text-gray-900 group-hover:text-[#3E7B27] transition-colors">{t('support_complaints_title')}</h3>
              <p className="text-xs text-gray-500 mt-1">{t('support_complaints_desc')}</p>
              <span className="inline-flex items-center text-xs font-bold text-red-600 mt-3 group-hover:translate-x-1 transition-transform">{t('raise_ticket')}</span>
            </div>

          </div>
        </div>

        {/* Recent Bookings Activity List */}
        <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
              <span>🕒</span> {t('recent_bookings')}
            </h2>
            <Link to="/farmer/bookings" className="text-xs font-bold text-[#3E7B27] hover:underline">
              {t('view_all_bookings')}
            </Link>
          </div>

          {dashboardData.recentBookings && dashboardData.recentBookings.length > 0 ? (
            <div className="space-y-3">
              {dashboardData.recentBookings.map((b, idx) => (
                <div
                  key={idx}
                  className="p-4 bg-[#F7F6F0] rounded-2xl border border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-white text-[#3E7B27] flex items-center justify-center text-xl shadow-sm font-bold">
                      🚜
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-gray-900">{b.equipmentName}</h4>
                      <p className="text-xs text-gray-500">ID: {b.bookingId} • Date: {b.bookingDate}</p>
                    </div>
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-4">
                    <span className="text-sm font-extrabold text-gray-900">{b.totalCost}</span>
                    <span
                      className={`px-3 py-1 rounded-full text-xs font-extrabold ${
                        b.status === 'ACCEPTED'
                          ? 'bg-blue-100 text-blue-800'
                          : b.status === 'COMPLETED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {b.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-gray-500 py-4 text-center">No recent bookings found.</p>
          )}
        </div>

      </div>
    </div>
  );
}

export default FarmerDashboard;
