import { useEffect, useState } from 'react';
import { motion, useMotionValue, useMotionValueEvent, useSpring } from 'framer-motion';
import { AlertCircle, CalendarClock, IndianRupee, Tractor, UserCheck, Users } from 'lucide-react';
import { adminService } from '../../services/adminService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

const STATUS_STYLES = {
  PENDING: 'border-amber-200 bg-amber-50 text-amber-800',
  ACCEPTED: 'border-sky-200 bg-sky-50 text-sky-800',
  CONFIRMED: 'border-indigo-200 bg-indigo-50 text-indigo-800',
  OPERATOR_ASSIGNED: 'border-violet-200 bg-violet-50 text-violet-800',
  WORK_STARTED: 'border-blue-200 bg-blue-50 text-blue-800',
  COMPLETED: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  CANCELLED: 'border-slate-200 bg-slate-100 text-slate-600',
  REJECTED: 'border-red-200 bg-red-50 text-red-800',
};

function formatINR(value) {
  return Math.round(value || 0).toLocaleString('en-IN');
}

function AnimatedNumber({ value, prefix = '' }) {
  const motionValue = useMotionValue(0);
  const spring = useSpring(motionValue, { stiffness: 80, damping: 20, mass: 1 });
  const [display, setDisplay] = useState(0);

  useEffect(() => {
    motionValue.set(value);
  }, [value, motionValue]);

  useMotionValueEvent(spring, 'change', (latest) => setDisplay(Math.round(latest)));

  return (
    <span>
      {prefix}
      {display.toLocaleString('en-IN')}
    </span>
  );
}

function StatCard({ label, value, prefix, icon: Icon, iconBg, iconColor, delay }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay, ease: [0.22, 1, 0.36, 1] }}
      className="bg-white p-5 sm:p-6 rounded-3xl shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group"
    >
      <div className="flex items-center justify-between mb-3">
        <div className={`w-12 h-12 rounded-2xl ${iconBg} ${iconColor} flex items-center justify-center text-xl group-hover:scale-110 transition-transform shadow-2xs`}>
          <Icon className="h-6 w-6" />
        </div>
        <span className="text-xs font-bold text-slate-400 group-hover:text-emerald-700 group-hover:translate-x-0.5 transition-all">Details →</span>
      </div>
      <p className="text-xs font-bold uppercase tracking-wide text-slate-400">{label}</p>
      <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-1">
        <AnimatedNumber value={value} prefix={prefix} />
      </p>
    </motion.div>
  );
}

function AdminOverview() {
  const [stats, setStats] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [statsData, bookingsData] = await Promise.all([
          adminService.getDashboardStats(),
          adminService.getRecentBookings(),
        ]);
        if (!cancelled) {
          setStats(statsData);
          setBookings(bookingsData || []);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load dashboard data.');
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="space-y-6">
      {/* Signature AgroRent Banner */}
      <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="relative h-44 sm:h-48 bg-gradient-to-r from-[#0F382C] via-[#1B4D3E] to-[#2E7D32] overflow-hidden flex items-center px-6 sm:px-10">
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
                    🛡️ Admin Console
                  </span>
                  <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-full text-[11px] font-extrabold shadow-sm">
                    ✓ Super Admin Access
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                  System Operations Overview
                </h1>
                <p className="text-xs sm:text-sm text-emerald-100 mt-1">
                  Platform-wide metrics, partner equipment verifications, and rental workflows.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="flex items-start gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 shadow-2xs">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
          <span>{error}</span>
        </div>
      )}

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Total Farmers"
          value={stats?.totalFarmers ?? 0}
          icon={Users}
          iconBg="bg-emerald-50"
          iconColor="text-emerald-700"
          delay={0}
        />
        <StatCard
          label="Active Operators"
          value={stats?.activeOperators ?? 0}
          icon={UserCheck}
          iconBg="bg-blue-50"
          iconColor="text-blue-700"
          delay={0.06}
        />
        <StatCard
          label="Total Revenue"
          value={stats?.totalRevenue ?? 0}
          prefix="₹"
          icon={IndianRupee}
          iconBg="bg-amber-50"
          iconColor="text-amber-700"
          delay={0.12}
        />
        <StatCard
          label="Pending Approvals"
          value={stats?.pendingApprovals ?? 0}
          icon={Tractor}
          iconBg="bg-purple-50"
          iconColor="text-purple-700"
          delay={0.18}
        />
      </div>

      {/* Recent Activity Table Card */}
      <div className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
        <div className="flex items-center gap-2 border-b border-slate-100 px-5 py-4 sm:px-6 bg-white">
          <CalendarClock className="h-4 w-4 text-emerald-700" />
          <h2 className="text-sm font-extrabold text-slate-900">Recent Booking Activity</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-xs font-bold uppercase tracking-wider text-slate-500">
                <th className="px-5 py-3.5 sm:px-6">Booking</th>
                <th className="px-5 py-3.5 sm:px-6">Farmer</th>
                <th className="px-5 py-3.5 sm:px-6">Equipment</th>
                <th className="px-5 py-3.5 sm:px-6">Amount</th>
                <th className="px-5 py-3.5 sm:px-6">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-slate-400 sm:px-6">
                    <div className="flex items-center justify-center gap-2">
                      <div className="h-5 w-5 animate-spin rounded-full border-2 border-emerald-600 border-t-transparent" />
                      <span>Loading recent activity…</span>
                    </div>
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-slate-500 sm:px-6 font-medium">
                    No bookings recorded yet.
                  </td>
                </tr>
              ) : (
                bookings.map((booking) => (
                  <tr
                    key={booking.id}
                    className="transition-colors duration-150 hover:bg-slate-50/80"
                  >
                    <td className="px-5 py-3.5 font-bold text-slate-900 sm:px-6">#{booking.id}</td>
                    <td className="px-5 py-3.5 text-slate-700 sm:px-6">{booking.farmerName || '—'}</td>
                    <td className="px-5 py-3.5 text-slate-700 sm:px-6">{booking.equipmentName || '—'}</td>
                    <td className="px-5 py-3.5 text-slate-900 font-semibold sm:px-6">₹{formatINR(booking.totalCost)}</td>
                    <td className="px-5 py-3.5 sm:px-6">
                      <span
                        className={`inline-flex rounded-full border px-2.5 py-0.5 text-[11px] font-bold ${
                          STATUS_STYLES[booking.status] || 'border-slate-200 bg-slate-100 text-slate-600'
                        }`}
                      >
                        {booking.status?.replace(/_/g, ' ')}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default AdminOverview;
