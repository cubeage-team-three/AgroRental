import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  CheckCircle2,
  Clock,
  MapPin,
  Tractor,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  TrendingUp,
  Navigation,
  Play,
  Pause,
  XCircle,
  ShieldCheck,
  Eye,
  Award,
  Activity,
  Layers,
  IndianRupee,
  Wallet,
  Star,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { getStoredUser } from '../../utils/auth';
import { DEFAULT_EQUIPMENT_IMAGE, formatCategoryLabel } from '../../utils/constants';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function OperatorDashboard() {
  const [metrics, setMetrics] = useState(null);
  const [earningsSummary, setEarningsSummary] = useState(null);
  const [ratingSummary, setRatingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const user = getStoredUser();
  const operatorName = user?.fullName || user?.name || 'Certified Operator';

  const fetchMetrics = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [data, earnings, ratings] = await Promise.all([
        operatorService.getDashboardMetrics(),
        operatorService.getEarningsSummary().catch((err) => {
          console.warn('Unable to load earnings summary for dashboard:', err);
          return null;
        }),
        operatorService.getMyRatingSummary().catch((err) => {
          console.warn('Unable to load rating summary for dashboard:', err);
          return null;
        }),
      ]);
      setMetrics(data);
      setEarningsSummary(earnings);
      const ratingData = ratings?.data || ratings;
      if (ratingData && typeof ratingData === 'object') {
        setRatingSummary(ratingData);
      }
    } catch (err) {
      console.error('Failed to load operator dashboard metrics:', err);
      setError(err.message || 'Unable to retrieve dashboard metrics.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchMetrics();
  }, [fetchMetrics]);

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ASSIGNED':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'ACCEPTED':
        return 'bg-blue-100 text-blue-800 border-blue-300';
      case 'TRAVELING':
        return 'bg-indigo-100 text-indigo-800 border-indigo-300';
      case 'REACHED':
        return 'bg-teal-100 text-teal-800 border-teal-300';
      case 'IN_PROGRESS':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'PAUSED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'COMPLETED':
        return 'bg-green-100 text-green-900 border-green-400';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-300';
      case 'CANCELLED':
        return 'bg-gray-100 text-gray-800 border-gray-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto space-y-6 font-sans animate-pulse">
        <div className="h-36 bg-gray-200 rounded-3xl" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-28 bg-gray-200 rounded-3xl" />
          ))}
        </div>
        <div className="h-64 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  if (error && !metrics) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 font-sans">
        <div className="p-6 bg-red-50 border border-red-200 rounded-3xl text-red-800 space-y-4 shadow-sm">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-red-600 shrink-0" />
            <div>
              <h3 className="text-base font-bold">Unable to Load Operator Dashboard</h3>
              <p className="text-xs text-red-700 mt-0.5">{error}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={fetchMetrics}
            className="px-4 py-2 bg-red-600 text-white text-xs font-bold rounded-xl hover:bg-red-700 transition-colors"
          >
            Retry Loading Dashboard
          </button>
        </div>
      </div>
    );
  }

  const activeJob = metrics?.activeJob;
  const activeJobImage = activeJob?.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">

      {/* Welcome Banner */}
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
                    ⚡ Operator Console
                  </span>
                  <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-full text-[11px] font-extrabold shadow-sm">
                    ✓ Certified Operator
                  </span>
                </div>
                <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                  Welcome back, {operatorName}! 👋
                </h1>
                <p className="text-xs sm:text-sm text-emerald-100 mt-1">
                  Fieldwork & Operations Dashboard • Real-time task allocations & field performance
                </p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2.5 shrink-0">
              <Link
                to="/operator/ratings"
                className="inline-flex items-center gap-1.5 px-3.5 py-2.5 bg-lime-400 hover:bg-lime-300 text-emerald-950 text-xs font-extrabold rounded-xl shadow-md transition-all"
              >
                <Star className="w-4 h-4 fill-emerald-950 text-emerald-950" />
                <span>{ratingSummary?.averageRating ? Number(ratingSummary.averageRating).toFixed(1) : '0.0'} ★ ({ratingSummary?.totalReviews || 0} Reviews)</span>
              </Link>

              <button
                type="button"
                onClick={fetchMetrics}
                className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-white/10 hover:bg-white/20 text-white text-xs font-bold rounded-xl border border-white/20 backdrop-blur-md transition-all shadow-sm shrink-0"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
                <span>Refresh</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Status Counts Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3.5">
        
        {/* Total Tasks */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm flex flex-col justify-between hover:border-emerald-600 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-bold text-slate-400">Total Jobs</span>
            <Layers className="w-4 h-4 text-slate-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-2">
            {metrics?.totalJobs || 0}
          </div>
        </div>

        {/* Assigned */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm flex flex-col justify-between hover:border-emerald-600 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-bold text-amber-600">Assigned</span>
            <Clock className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-amber-900 mt-2">
            {metrics?.assignedJobs || 0}
          </div>
        </div>

        {/* In Progress */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm flex flex-col justify-between hover:border-emerald-600 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-bold text-emerald-600">Working</span>
            <Activity className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-emerald-900 mt-2">
            {metrics?.inProgressJobs || 0}
          </div>
        </div>

        {/* Completed */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm flex flex-col justify-between hover:border-emerald-600 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-bold text-emerald-700">Completed</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-slate-900 mt-2">
            {metrics?.completedJobs || 0}
          </div>
        </div>

        {/* Declined */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-slate-200 shadow-sm flex flex-col justify-between hover:border-emerald-600 transition-all">
          <div className="flex items-center justify-between">
            <span className="text-xs uppercase font-bold text-red-600">Declined</span>
            <XCircle className="w-4 h-4 text-red-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-red-900 mt-2">
            {metrics?.rejectedJobs || 0}
          </div>
        </div>

      </div>

      {/* Performance & Scheduling Cards Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* Completion Rate KPI */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="p-2 rounded-xl bg-emerald-50 text-emerald-700">
                <Award className="w-5 h-5" />
              </span>
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Job Completion Rate</h3>
                <p className="text-lg font-extrabold text-slate-900">{metrics?.completionRate || 0}%</p>
              </div>
            </div>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
            <div
              className="bg-emerald-600 h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, metrics?.completionRate || 0)}%` }}
            />
          </div>
          <p className="text-[11px] text-slate-400">
            {metrics?.completedJobs || 0} finished out of {metrics?.totalJobs || 0} total assignments.
          </p>
        </div>

        {/* Acceptance Rate KPI */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="p-2 rounded-xl bg-blue-50 text-blue-600">
                <TrendingUp className="w-5 h-5" />
              </span>
              <div>
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">Acceptance Rate</h3>
                <p className="text-lg font-extrabold text-slate-900">{metrics?.acceptanceRate || 0}%</p>
              </div>
            </div>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
            <div
              className="bg-blue-600 h-full rounded-full transition-all duration-500"
              style={{ width: `${Math.min(100, metrics?.acceptanceRate || 0)}%` }}
            />
          </div>
          <p className="text-[11px] text-slate-400">
            Based on completed, active, and declined assignment responses.
          </p>
        </div>

        {/* Schedule Insights */}
        <div className="bg-white rounded-3xl p-5 border border-slate-200 shadow-sm flex flex-col justify-between space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-emerald-600" />
              <span>Schedule Insights</span>
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-2xl">
              <span className="text-xs uppercase font-bold text-slate-400 block">Today's Jobs</span>
              <span className="text-xl font-extrabold text-slate-900">{metrics?.todayJobs || 0}</span>
            </div>
            <div className="p-3 bg-slate-50 border border-slate-200/80 rounded-2xl">
              <span className="text-xs uppercase font-bold text-slate-400 block">Upcoming</span>
              <span className="text-xl font-extrabold text-slate-900">{metrics?.upcomingJobs || 0}</span>
            </div>
          </div>

          <Link
            to="/operator/jobs"
            className="text-xs font-bold text-emerald-700 hover:underline flex items-center gap-1 self-end"
          >
            <span>Browse Full Schedule</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

      </div>

      {/* Earnings & Work Hours Summary Card (FR-EARNINGS-SUMMARY) */}
      <div className="bg-gradient-to-r from-[#0F382C] via-[#1B4D3E] to-[#2E7D32] rounded-3xl p-6 text-white shadow-sm border border-emerald-900/40 relative overflow-hidden">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-300">
                <Wallet className="w-4 h-4" />
              </span>
              <span className="text-[10px] uppercase font-bold tracking-widest text-emerald-300">
                Compensation & Work Hours
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-extrabold tracking-tight text-white">
              Operator Earnings Summary
            </h2>
            <p className="text-xs text-emerald-100/80">
              Audited compensation derived from server-side lifecycle timestamps and hourly work logs.
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            <Link
              to="/operator/earnings"
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-lime-400 hover:bg-lime-300 text-emerald-950 text-xs font-extrabold rounded-xl transition-all shadow-md"
            >
              <span>View Earnings Breakdown</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3.5 mt-6 pt-6 border-t border-emerald-700/50 relative z-10">
          <div className="bg-white/10 backdrop-blur-md p-4 rounded-2xl border border-white/10">
            <span className="text-xs uppercase font-bold text-emerald-200 block">Total Earnings</span>
            <span className="text-xl sm:text-2xl font-extrabold text-white mt-1 block">
              ₹{Number(earningsSummary?.totalGrossEarnings || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
            <span className="text-[10px] text-emerald-200/70 mt-0.5 block">Gross Compensation</span>
          </div>

          <div className="bg-white/10 backdrop-blur-md p-4 rounded-2xl border border-white/10">
            <span className="text-xs uppercase font-bold text-emerald-200 block">Logged Hours</span>
            <span className="text-xl sm:text-2xl font-extrabold text-white mt-1 block">
              {earningsSummary?.totalWorkHours || 0} hrs
            </span>
            <span className="text-[10px] text-emerald-200/70 mt-0.5 block">Net Fieldwork Time</span>
          </div>

          <div className="bg-white/10 backdrop-blur-md p-4 rounded-2xl border border-white/10">
            <span className="text-xs uppercase font-bold text-emerald-200 block">Completed Jobs</span>
            <span className="text-xl sm:text-2xl font-extrabold text-white mt-1 block">
              {earningsSummary?.totalCompletedJobs || 0}
            </span>
            <span className="text-[10px] text-emerald-200/70 mt-0.5 block">Delivered Assignments</span>
          </div>

          <div className="bg-white/10 backdrop-blur-md p-4 rounded-2xl border border-white/10">
            <span className="text-xs uppercase font-bold text-emerald-200 block">Base Hourly Rate</span>
            <span className="text-xl sm:text-2xl font-extrabold text-white mt-1 block">
              ₹{Number(earningsSummary?.hourlyRate || 500).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}/hr
            </span>
            <span className="text-[10px] text-emerald-200/70 mt-0.5 block">Standard Operator Tariff</span>
          </div>
        </div>
      </div>

      {/* Active Work Assignment Spotlight */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 space-y-4 shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <Tractor className="w-5 h-5 text-emerald-700" />
            <h2 className="text-base font-extrabold text-slate-900 uppercase tracking-wider">
              Current Active Field Task
            </h2>
          </div>
          <Link
            to="/operator/jobs"
            className="text-xs font-bold text-emerald-700 hover:underline flex items-center gap-1"
          >
            <span>View All Tasks</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {activeJob ? (
          <div className="p-5 bg-[#F8FAF8] rounded-2xl border border-slate-200 flex flex-col md:flex-row md:items-center justify-between gap-5">
            <div className="flex items-center gap-4">
              <div className="w-20 h-20 rounded-2xl overflow-hidden bg-slate-100 shrink-0">
                <img
                  src={activeJobImage}
                  alt={activeJob.equipmentName || 'Equipment'}
                  className="w-full h-full object-cover"
                  onError={(e) => {
                    e.target.onerror = null;
                    e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                  }}
                />
              </div>
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span
                    className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border shadow-2xs ${getStatusBadge(
                      activeJob.status
                    )}`}
                  >
                    {activeJob.status}
                  </span>
                  <span className="text-[10px] font-bold text-slate-400">Task #{activeJob.assignmentId}</span>
                </div>
                <h3 className="text-base font-extrabold text-slate-900">
                  {activeJob.equipmentName || `Machinery #${activeJob.equipmentId}`}
                </h3>
                <p className="text-xs text-slate-500 flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span>{activeJob.deliveryAddress || 'Farm location specified in booking'}</span>
                </p>
              </div>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center gap-4 justify-between md:justify-end border-t md:border-t-0 pt-3 md:pt-0 border-slate-200">
              <div className="text-left sm:text-right text-xs">
                <span className="font-bold text-slate-900 block">
                  {activeJob.startDate} → {activeJob.endDate}
                </span>
                <span className="text-emerald-700 font-extrabold text-sm">
                  ₹{Number(activeJob.totalCost || 0).toLocaleString('en-IN')}
                </span>
              </div>

              <Link
                to={`/operator/jobs/${activeJob.assignmentId}`}
                className="px-5 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
              >
                <Eye className="w-4 h-4" />
                <span>Manage Task</span>
              </Link>
            </div>
          </div>
        ) : (
          <div className="p-8 text-center space-y-3 bg-[#F8FAF8] rounded-2xl border border-dashed border-slate-200">
            <div className="w-12 h-12 bg-emerald-50 text-emerald-700 rounded-2xl flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div className="space-y-1">
              <h3 className="text-sm font-bold text-slate-800">No Active Operations Right Now</h3>
              <p className="text-xs text-slate-500">
                You have no field operations currently assigned or in progress. Check assigned tasks for new partner allocations.
              </p>
            </div>
            <Link
              to="/operator/jobs"
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl transition-colors"
            >
              <span>View Assigned Jobs</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default OperatorDashboard;
