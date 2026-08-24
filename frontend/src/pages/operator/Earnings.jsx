import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  Wallet,
  IndianRupee,
  Clock,
  CheckCircle2,
  Calendar,
  Layers,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  Tractor,
  MapPin,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Award,
  PauseCircle,
  FileSpreadsheet,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { getStoredUser } from '../../utils/auth';
import { formatCategoryLabel } from '../../utils/constants';

function OperatorEarnings() {
  const [summary, setSummary] = useState(null);
  const [historyPage, setHistoryPage] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [error, setError] = useState(null);

  const user = getStoredUser();
  const operatorName = user?.fullName || user?.name || 'Certified Operator';

  const fetchSummary = useCallback(async () => {
    try {
      const data = await operatorService.getEarningsSummary();
      setSummary(data);
    } catch (err) {
      console.error('Failed to load earnings summary:', err);
      throw err;
    }
  }, []);

  const fetchHistory = useCallback(async (page = 0) => {
    setHistoryLoading(true);
    try {
      const pageData = await operatorService.getEarningsHistory({ page, size: pageSize });
      setHistoryPage(pageData);
    } catch (err) {
      console.error('Failed to load earnings history:', err);
    } finally {
      setHistoryLoading(false);
    }
  }, [pageSize]);

  const loadAllData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      await Promise.all([fetchSummary(), fetchHistory(currentPage)]);
    } catch (err) {
      setError(err.message || 'Unable to load earnings data.');
    } finally {
      setLoading(false);
    }
  }, [fetchSummary, fetchHistory, currentPage]);

  useEffect(() => {
    loadAllData();
  }, [loadAllData]);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && (!historyPage || newPage < historyPage.totalPages)) {
      setCurrentPage(newPage);
      fetchHistory(newPage);
    }
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto space-y-6 font-sans animate-pulse">
        <div className="h-40 bg-gray-200 rounded-3xl" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-32 bg-gray-200 rounded-3xl" />
          ))}
        </div>
        <div className="h-80 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  if (error && !summary) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 font-sans">
        <div className="p-6 bg-red-50 border border-red-200 rounded-3xl text-red-800 space-y-4 shadow-sm">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-red-600 shrink-0" />
            <div>
              <h3 className="text-base font-bold">Unable to Load Earnings Module</h3>
              <p className="text-xs text-red-700 mt-1">{error}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={loadAllData}
            className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-xs transition-all flex items-center gap-2"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Try Again</span>
          </button>
        </div>
      </div>
    );
  }

  const historyItems = historyPage?.content || [];

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header Banner */}
      <div className="bg-linear-to-r from-[#142E1C] via-[#1F4529] to-[#2E6F22] rounded-3xl p-6 sm:p-8 text-white shadow-lg relative overflow-hidden flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div className="space-y-2 max-w-xl z-10">
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-emerald-500/20 border border-emerald-400/30 text-emerald-300 rounded-full text-[10px] font-black uppercase tracking-wider">
              Work Hours & Compensation Hub
            </span>
            <span className="text-xs text-emerald-200/80 font-medium">
              Operator: <strong>{operatorName}</strong>
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight">
            Earnings & Fieldwork Duration
          </h1>
          <p className="text-xs text-emerald-100/80 leading-relaxed">
            Transparent compensation logs calculated strictly from verified server-side lifecycle timestamps, excluding pause intervals.
          </p>
        </div>

        <button
          type="button"
          onClick={loadAllData}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-white/10 hover:bg-white/20 text-white text-xs font-bold rounded-2xl border border-white/20 backdrop-blur-xs transition-all shadow-sm shrink-0 self-start sm:self-auto"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Records</span>
        </button>
      </div>

      {/* Summary KPI Cards Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3.5">
        
        {/* Total Earnings */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-emerald-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-emerald-600">Total Earnings</span>
            <Wallet className="w-4 h-4 text-emerald-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-emerald-900 mt-2">
            ₹{Number(summary?.totalGrossEarnings || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Gross Payout</span>
        </div>

        {/* Logged Work Hours */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-gray-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-gray-500">Work Hours</span>
            <Clock className="w-4 h-4 text-gray-400" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-gray-900 mt-2">
            {summary?.totalWorkHours || 0} hrs
          </div>
          <span className="text-[10px] text-gray-400 mt-1">{summary?.totalWorkMinutes || 0} net mins</span>
        </div>

        {/* Paused Time */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-amber-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-amber-600">Paused Time</span>
            <PauseCircle className="w-4 h-4 text-amber-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-amber-900 mt-2">
            {summary?.totalPausedMinutes || 0} min
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Non-billable</span>
        </div>

        {/* Completed Tasks */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-green-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-green-600">Delivered</span>
            <CheckCircle2 className="w-4 h-4 text-green-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-green-900 mt-2">
            {summary?.totalCompletedJobs || 0}
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Finished Jobs</span>
        </div>

        {/* Average per Task */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-blue-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-blue-600">Avg / Job</span>
            <TrendingUp className="w-4 h-4 text-blue-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-blue-900 mt-2">
            ₹{Number(summary?.averageEarningsPerJob || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Per Completed Task</span>
        </div>

        {/* Base Hourly Tariff */}
        <div className="bg-white rounded-3xl p-4 sm:p-5 border border-purple-100 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-[10px] uppercase font-black tracking-wider text-purple-600">Hourly Tariff</span>
            <Award className="w-4 h-4 text-purple-500" />
          </div>
          <div className="text-xl sm:text-2xl font-black text-purple-900 mt-2">
            ₹{Number(summary?.hourlyRate || 500).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
          <span className="text-[10px] text-gray-400 mt-1">Rate per Hour</span>
        </div>

      </div>

      {/* Earnings History Section */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-100 pb-4">
          <div>
            <h2 className="text-base font-black text-[#142E1C] flex items-center gap-2">
              <FileSpreadsheet className="w-5 h-5 text-[#3E7B27]" />
              <span>Completed Tasks Earnings Ledger</span>
            </h2>
            <p className="text-xs text-gray-500 mt-0.5">
              Verified records of delivered machinery operations and compensation breakdown.
            </p>
          </div>
          <span className="text-xs font-bold text-gray-400">
            Total Records: {historyPage?.totalElements || 0}
          </span>
        </div>

        {historyLoading ? (
          <div className="p-12 text-center text-gray-400 animate-pulse space-y-2">
            <RefreshCw className="w-6 h-6 animate-spin mx-auto text-[#3E7B27]" />
            <p className="text-xs">Loading completed jobs ledger...</p>
          </div>
        ) : historyItems.length === 0 ? (
          <div className="p-12 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-200 space-y-3">
            <Tractor className="w-10 h-10 text-gray-300 mx-auto" />
            <h3 className="text-sm font-black text-gray-700">No Completed Job Earnings Yet</h3>
            <p className="text-xs text-gray-500 max-w-md mx-auto">
              Once you accept assignments, execute machinery operations, and mark tasks completed, detailed compensation calculations will appear here.
            </p>
            <Link
              to="/operator/jobs"
              className="inline-flex items-center gap-2 px-4 py-2 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl shadow-xs transition-all mt-2"
            >
              <span>View Assigned Jobs</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="bg-gray-50 text-gray-500 uppercase font-black text-[10px] tracking-wider border-b border-gray-100">
                  <th className="py-3.5 px-4 rounded-l-xl">Task / Booking</th>
                  <th className="py-3.5 px-4">Machinery</th>
                  <th className="py-3.5 px-4">Delivery Location</th>
                  <th className="py-3.5 px-4">Completion Date</th>
                  <th className="py-3.5 px-4">Work Duration</th>
                  <th className="py-3.5 px-4">Hourly Tariff</th>
                  <th className="py-3.5 px-4">Gross Compensation</th>
                  <th className="py-3.5 px-4 rounded-r-xl text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {historyItems.map((item) => (
                  <tr key={item.assignmentId} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-4 px-4 font-bold text-gray-900">
                      <div>
                        <span>Assignment #{item.assignmentId}</span>
                        {item.bookingId && (
                          <span className="text-[10px] text-gray-400 block font-normal">
                            Booking #{item.bookingId}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-4 px-4">
                      <div className="font-bold text-gray-800">{item.equipmentName}</div>
                      <span className="inline-block mt-0.5 px-2 py-0.5 bg-emerald-50 text-emerald-700 text-[10px] font-bold rounded-md">
                        {formatCategoryLabel(item.equipmentCategory)}
                      </span>
                    </td>
                    <td className="py-4 px-4 text-gray-600 max-w-xs truncate">
                      <div className="flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                        <span className="truncate">{item.deliveryAddress || 'On-site'}</span>
                      </div>
                    </td>
                    <td className="py-4 px-4 text-gray-600">
                      {item.completedAt ? new Date(item.completedAt).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      }) : 'N/A'}
                    </td>
                    <td className="py-4 px-4 font-semibold text-gray-800">
                      <div>
                        <span>{item.netWorkHours} hrs</span>
                        <span className="text-[10px] text-gray-400 block font-normal">
                          {item.netWorkMinutes} net mins
                        </span>
                      </div>
                    </td>
                    <td className="py-4 px-4 font-semibold text-gray-700">
                      ₹{Number(item.hourlyRate || 500).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}/hr
                    </td>
                    <td className="py-4 px-4 font-black text-emerald-800 text-sm">
                      ₹{Number(item.grossEarnings || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </td>
                    <td className="py-4 px-4 text-right">
                      <Link
                        to={`/operator/jobs/${item.assignmentId}`}
                        className="inline-flex items-center gap-1 px-3 py-1.5 bg-[#F0EFE9] hover:bg-[#3E7B27] hover:text-white text-[#142E1C] font-bold text-[11px] rounded-lg transition-all"
                      >
                        <span>Details</span>
                        <ArrowRight className="w-3 h-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Controls */}
        {historyPage && historyPage.totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t border-gray-100">
            <span className="text-xs text-gray-500">
              Showing page {currentPage + 1} of {historyPage.totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={currentPage === 0 || historyLoading}
                onClick={() => handlePageChange(currentPage - 1)}
                className="px-3 py-1.5 bg-white border border-gray-200 rounded-xl text-xs font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center gap-1"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Previous</span>
              </button>
              <button
                type="button"
                disabled={currentPage >= historyPage.totalPages - 1 || historyLoading}
                onClick={() => handlePageChange(currentPage + 1)}
                className="px-3 py-1.5 bg-white border border-gray-200 rounded-xl text-xs font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-all flex items-center gap-1"
              >
                <span>Next</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}
      </div>

    </div>
  );
}

export default OperatorEarnings;
