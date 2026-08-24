import { useEffect, useState } from 'react';
import { motion, useMotionValue, useMotionValueEvent, useSpring } from 'framer-motion';
import { AlertCircle, CalendarClock, IndianRupee, Tractor, UserCheck, Users } from 'lucide-react';
import { adminService } from '../../services/adminService';

const STATUS_STYLES = {
  PENDING: 'border-amber-400/30 bg-amber-400/10 text-amber-300',
  ACCEPTED: 'border-sky-400/30 bg-sky-400/10 text-sky-300',
  CONFIRMED: 'border-indigo-400/30 bg-indigo-400/10 text-indigo-300',
  OPERATOR_ASSIGNED: 'border-violet-400/30 bg-violet-400/10 text-violet-300',
  WORK_STARTED: 'border-cyan-400/30 bg-cyan-400/10 text-cyan-300',
  COMPLETED: 'border-emerald-400/30 bg-emerald-400/10 text-emerald-300',
  CANCELLED: 'border-white/15 bg-white/5 text-white/40',
  REJECTED: 'border-red-400/30 bg-red-400/10 text-red-300',
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

function StatCard({ label, value, prefix, icon: Icon, glow, delay }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: [0.22, 1, 0.36, 1] }}
      className="relative overflow-hidden rounded-3xl border border-white/10 bg-white/[0.03] p-5 backdrop-blur-xl sm:p-6"
    >
      <div className={`pointer-events-none absolute -right-6 -top-6 h-28 w-28 rounded-full blur-3xl ${glow}`} />
      <div className="relative flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-white/40">{label}</p>
          <p className="mt-2 font-display text-3xl font-bold text-white">
            <AnimatedNumber value={value} prefix={prefix} />
          </p>
        </div>
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl border border-white/10 bg-white/5 text-emerald-300">
          <Icon className="h-5 w-5" />
        </span>
      </div>
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
    <div>
      <div>
        <h1 className="font-display text-2xl font-bold text-white sm:text-3xl">Overview</h1>
        <p className="mt-1.5 text-sm text-white/50">Platform-wide metrics and the latest booking activity.</p>
      </div>

      {error && (
        <div className="mt-6 flex items-start gap-2 rounded-2xl border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Total Farmers"
          value={stats?.totalFarmers ?? 0}
          icon={Users}
          glow="bg-emerald-500/20"
          delay={0}
        />
        <StatCard
          label="Active Operators"
          value={stats?.activeOperators ?? 0}
          icon={UserCheck}
          glow="bg-lime-400/20"
          delay={0.06}
        />
        <StatCard
          label="Total Revenue"
          value={stats?.totalRevenue ?? 0}
          prefix="₹"
          icon={IndianRupee}
          glow="bg-amber-400/20"
          delay={0.12}
        />
        <StatCard
          label="Pending Approvals"
          value={stats?.pendingApprovals ?? 0}
          icon={Tractor}
          glow="bg-violet-400/20"
          delay={0.18}
        />
      </div>

      <div className="mt-8 rounded-3xl border border-white/10 bg-white/[0.03] backdrop-blur-xl">
        <div className="flex items-center gap-2 border-b border-white/10 px-5 py-4 sm:px-6">
          <CalendarClock className="h-4 w-4 text-emerald-300" />
          <h2 className="text-sm font-semibold text-white">Recent Activity</h2>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead>
              <tr className="border-b border-white/10 text-xs font-semibold uppercase tracking-wide text-white/40">
                <th className="px-5 py-3 sm:px-6">Booking</th>
                <th className="px-5 py-3 sm:px-6">Farmer</th>
                <th className="px-5 py-3 sm:px-6">Equipment</th>
                <th className="px-5 py-3 sm:px-6">Amount</th>
                <th className="px-5 py-3 sm:px-6">Status</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-white/30 sm:px-6">
                    Loading recent activity…
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-5 py-8 text-center text-white/30 sm:px-6">
                    No bookings yet.
                  </td>
                </tr>
              ) : (
                bookings.map((booking) => (
                  <tr
                    key={booking.id}
                    className="border-b border-white/5 transition-colors duration-150 last:border-b-0 hover:bg-white/[0.03]"
                  >
                    <td className="px-5 py-3.5 font-medium text-white sm:px-6">#{booking.id}</td>
                    <td className="px-5 py-3.5 text-white/70 sm:px-6">{booking.farmerName || '—'}</td>
                    <td className="px-5 py-3.5 text-white/70 sm:px-6">{booking.equipmentName || '—'}</td>
                    <td className="px-5 py-3.5 text-white/70 sm:px-6">₹{formatINR(booking.totalCost)}</td>
                    <td className="px-5 py-3.5 sm:px-6">
                      <span
                        className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold ${
                          STATUS_STYLES[booking.status] || 'border-white/15 bg-white/5 text-white/50'
                        }`}
                      >
                        {booking.status?.replace('_', ' ')}
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
