import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  History,
  Calendar,
  Filter,
  Download,
  Printer,
  Search,
  CheckCircle2,
  XCircle,
  Clock,
  IndianRupee,
  Star,
  Tractor,
  MapPin,
  PauseCircle,
  Eye,
  X,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  SlidersHorizontal,
  FileText,
  Building2,
  User,
  ShieldCheck,
  AlertCircle
} from 'lucide-react';
import operatorService from '../../services/operatorService';

export default function JobHistory() {
  // State: Data
  const [historyData, setHistoryData] = useState({ content: [], totalElements: 0, totalPages: 0 });
  const [summaryData, setSummaryData] = useState(null);
  const [selectedJob, setSelectedJob] = useState(null);

  // State: Loading & Errors
  const [loading, setLoading] = useState(true);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [error, setError] = useState(null);

  // State: Filters
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [datePreset, setDatePreset] = useState('ALL');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');

  // State: Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  // Calculate Date Ranges based on Preset
  const computedDateRange = useMemo(() => {
    const today = new Date();
    const formatDate = (d) => d.toISOString().split('T')[0];

    if (datePreset === 'LAST_7_DAYS') {
      const past = new Date();
      past.setDate(today.getDate() - 7);
      return { start: formatDate(past), end: formatDate(today) };
    } else if (datePreset === 'LAST_30_DAYS') {
      const past = new Date();
      past.setDate(today.getDate() - 30);
      return { start: formatDate(past), end: formatDate(today) };
    } else if (datePreset === 'THIS_MONTH') {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      return { start: formatDate(firstDay), end: formatDate(today) };
    } else if (datePreset === 'CUSTOM') {
      return { start: customStartDate, end: customEndDate };
    }
    return { start: '', end: '' };
  }, [datePreset, customStartDate, customEndDate]);

  // Fetch History Data
  const fetchJobHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size: pageSize,
        status: statusFilter,
        equipmentCategory: categoryFilter,
        search: searchQuery,
        startDate: computedDateRange.start,
        endDate: computedDateRange.end,
      };

      const res = await operatorService.getJobHistory(params);
      const data = res?.data || res || {};
      setHistoryData({
        content: data.content || [],
        totalElements: data.totalElements || 0,
        totalPages: data.totalPages || 1,
      });
    } catch (err) {
      console.error('Failed to fetch operator job history:', err);
      setError(err.response?.data?.message || 'Failed to load historical field job records.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusFilter, categoryFilter, searchQuery, computedDateRange]);

  // Fetch Summary Analytics
  const fetchSummary = useCallback(async () => {
    setSummaryLoading(true);
    try {
      const params = {
        equipmentCategory: categoryFilter,
        startDate: computedDateRange.start,
        endDate: computedDateRange.end,
      };
      const res = await operatorService.getJobHistorySummary(params);
      setSummaryData(res?.data || res || null);
    } catch (err) {
      console.error('Failed to fetch job history summary:', err);
    } finally {
      setSummaryLoading(false);
    }
  }, [categoryFilter, computedDateRange]);

  useEffect(() => {
    fetchJobHistory();
  }, [fetchJobHistory]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  // Reset Filters
  const handleResetFilters = () => {
    setStatusFilter('ALL');
    setCategoryFilter('ALL');
    setSearchQuery('');
    setDatePreset('ALL');
    setCustomStartDate('');
    setCustomEndDate('');
    setPage(0);
  };

  // Export CSV
  const handleExportCSV = () => {
    if (!historyData.content || historyData.content.length === 0) {
      alert('No historical records available to export.');
      return;
    }

    const headers = [
      'Assignment ID',
      'Booking ID',
      'Equipment Name',
      'Category',
      'Model',
      'Status',
      'Booking Start',
      'Booking End',
      'Delivery Address',
      'Work Started At',
      'Completed At',
      'Elapsed (mins)',
      'Paused (mins)',
      'Net Work Hours',
      'Hourly Rate (INR)',
      'Gross Earnings (INR)',
      'Customer Rating'
    ];

    const rows = historyData.content.map(job => [
      job.assignmentId,
      job.bookingId || 'N/A',
      `"${(job.equipmentName || '').replace(/"/g, '""')}"`,
      job.equipmentCategory || 'GENERAL',
      `"${(job.equipmentModel || '').replace(/"/g, '""')}"`,
      job.assignmentStatus,
      job.bookingStartDate || '',
      job.bookingEndDate || '',
      `"${(job.deliveryAddress || '').replace(/"/g, '""')}"`,
      job.workStartedAt || '',
      job.completedAt || '',
      job.totalElapsedMinutes || 0,
      job.totalPausedMinutes || 0,
      job.netWorkHours || '0.00',
      job.hourlyRate || '500.00',
      job.grossEarnings || '0.00',
      job.customerRating ? `${job.customerRating} Stars` : 'Unrated'
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Operator_Job_History_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Print Statement
  const handlePrintStatement = () => {
    window.print();
  };

  // Helper formatting
  const formatDateTime = (dtStr) => {
    if (!dtStr) return '—';
    try {
      const d = new Date(dtStr);
      return d.toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dtStr;
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'REJECTED':
        return 'bg-rose-100 text-rose-800 border-rose-300';
      case 'CANCELLED':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800 border-blue-300 animate-pulse';
      case 'PAUSED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      default:
        return 'bg-slate-100 text-slate-800 border-slate-300';
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 p-4 sm:p-6 lg:p-8 font-sans">
      <div className="max-w-7xl mx-auto space-y-6">

        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <div className="p-2.5 bg-emerald-600 rounded-xl text-white shadow-md shadow-emerald-200">
                <History className="w-6 h-6" />
              </div>
              <h1 className="text-2xl font-black text-slate-900 tracking-tight">Job History & Field Analytics</h1>
            </div>
            <p className="text-slate-600 text-sm">
              Comprehensive historical work archive, field operational logs, compensation calculations, and customer feedback.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleExportCSV}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-white text-slate-700 hover:bg-slate-100 border border-slate-300 rounded-xl text-sm font-bold shadow-sm transition-all active:scale-95"
            >
              <Download className="w-4 h-4 text-emerald-600" />
              Export CSV
            </button>
            <button
              onClick={handlePrintStatement}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white rounded-xl text-sm font-bold shadow-md shadow-emerald-200 transition-all active:scale-95"
            >
              <Printer className="w-4 h-4" />
              Print Statement
            </button>
          </div>
        </div>

        {/* KPI Performance Summary Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {/* Total Jobs */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Total Operations</p>
              <h3 className="text-2xl font-black text-slate-900">
                {summaryLoading ? '—' : summaryData?.totalHistoricalJobs ?? historyData.totalElements}
              </h3>
              <p className="text-xs text-slate-500 mt-1">Recorded field tasks</p>
            </div>
            <div className="p-3 bg-blue-50 text-blue-600 rounded-xl border border-blue-100">
              <FileText className="w-6 h-6" />
            </div>
          </div>

          {/* Completed Jobs */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Completed</p>
              <h3 className="text-2xl font-black text-emerald-700">
                {summaryLoading ? '—' : summaryData?.completedJobs ?? 0}
              </h3>
              <p className="text-xs text-emerald-600 font-semibold mt-1">
                {summaryData?.totalHistoricalJobs > 0
                  ? `${Math.round((summaryData.completedJobs / summaryData.totalHistoricalJobs) * 100)}% completion`
                  : '100% completion'}
              </p>
            </div>
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl border border-emerald-100">
              <CheckCircle2 className="w-6 h-6" />
            </div>
          </div>

          {/* Work Hours */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Logged Work Hours</p>
              <h3 className="text-2xl font-black text-indigo-700">
                {summaryLoading ? '—' : `${summaryData?.totalWorkHours ?? '0.00'} hrs`}
              </h3>
              <p className="text-xs text-slate-500 mt-1">Net field machinery run</p>
            </div>
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl border border-indigo-100">
              <Clock className="w-6 h-6" />
            </div>
          </div>

          {/* Gross Earnings */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Cumulative Pay</p>
              <h3 className="text-2xl font-black text-emerald-700 flex items-center gap-0.5">
                <IndianRupee className="w-5 h-5" />
                {summaryLoading ? '—' : Number(summaryData?.totalGrossEarnings ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </h3>
              <p className="text-xs text-slate-500 mt-1">₹500.00/hr standard rate</p>
            </div>
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl border border-emerald-100">
              <IndianRupee className="w-6 h-6" />
            </div>
          </div>

          {/* Customer Satisfaction Rating */}
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Farmer Rating</p>
              <div className="flex items-center gap-1.5">
                <h3 className="text-2xl font-black text-amber-600">
                  {summaryLoading ? '—' : (summaryData?.averageRating ? summaryData.averageRating.toFixed(1) : '5.0')}
                </h3>
                <Star className="w-5 h-5 fill-amber-400 text-amber-500" />
              </div>
              <p className="text-xs text-slate-500 mt-1">
                {summaryLoading ? '—' : `${summaryData?.totalReviewsCount ?? 0} reviews received`}
              </p>
            </div>
            <div className="p-3 bg-amber-50 text-amber-600 rounded-xl border border-amber-100">
              <Star className="w-6 h-6 fill-amber-400" />
            </div>
          </div>
        </div>

        {/* Filter Toolbar */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">

            {/* Quick Date Range Pills */}
            <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1 mr-1">
                <Calendar className="w-3.5 h-3.5" /> Date:
              </span>
              {[
                { label: 'All Time', value: 'ALL' },
                { label: 'Last 7 Days', value: 'LAST_7_DAYS' },
                { label: 'Last 30 Days', value: 'LAST_30_DAYS' },
                { label: 'This Month', value: 'THIS_MONTH' },
                { label: 'Custom', value: 'CUSTOM' },
              ].map(preset => (
                <button
                  key={preset.value}
                  onClick={() => {
                    setDatePreset(preset.value);
                    setPage(0);
                  }}
                  className={`px-3 py-1.5 text-xs font-bold rounded-lg transition-colors whitespace-nowrap ${
                    datePreset === preset.value
                      ? 'bg-emerald-700 text-white shadow-sm'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {preset.label}
                </button>
              ))}
            </div>

            {/* Search Input */}
            <div className="relative min-w-[280px]">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search equipment, location, ID..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setPage(0);
                }}
                className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-900 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:bg-white transition-all"
              />
            </div>
          </div>

          {/* Secondary Filter Row: Status, Machinery, Custom Dates */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 pt-3 border-t border-slate-100">
            {/* Status Dropdown */}
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                Assignment Status
              </label>
              <select
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:bg-white"
              >
                <option value="ALL">All Statuses</option>
                <option value="COMPLETED">Completed</option>
                <option value="REJECTED">Rejected</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            {/* Equipment Category Dropdown */}
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">
                Machinery Category
              </label>
              <select
                value={categoryFilter}
                onChange={(e) => {
                  setCategoryFilter(e.target.value);
                  setPage(0);
                }}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-800 focus:ring-2 focus:ring-emerald-500 focus:bg-white"
              >
                <option value="ALL">All Machinery</option>
                <option value="TRACTOR">Tractors</option>
                <option value="HARVESTER">Combine Harvesters</option>
                <option value="TILLER">Power Tillers</option>
                <option value="ROTAVATOR">Rotavators</option>
                <option value="SPRAYER">Crop Sprayers</option>
              </select>
            </div>

            {/* Custom Start Date */}
            {datePreset === 'CUSTOM' ? (
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">Start Date</label>
                <input
                  type="date"
                  value={customStartDate}
                  onChange={(e) => {
                    setCustomStartDate(e.target.value);
                    setPage(0);
                  }}
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            ) : (
              <div className="flex items-end">
                <button
                  onClick={handleResetFilters}
                  className="w-full py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl flex items-center justify-center gap-1.5 transition-colors"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  Reset Filters
                </button>
              </div>
            )}

            {/* Custom End Date */}
            {datePreset === 'CUSTOM' && (
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-1">End Date</label>
                <input
                  type="date"
                  value={customEndDate}
                  onChange={(e) => {
                    setCustomEndDate(e.target.value);
                    setPage(0);
                  }}
                  className="w-full px-3 py-1.5 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 focus:ring-2 focus:ring-emerald-500"
                />
              </div>
            )}
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="bg-rose-50 border border-rose-200 p-4 rounded-xl flex items-center justify-between text-rose-800 text-sm">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-5 h-5 text-rose-600 flex-shrink-0" />
              <span>{error}</span>
            </div>
            <button onClick={fetchJobHistory} className="font-bold underline hover:text-rose-900">
              Retry
            </button>
          </div>
        )}

        {/* Main Job History Ledger */}
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
            <div>
              <h2 className="text-lg font-black text-slate-900">Historical Operation Ledger</h2>
              <p className="text-xs text-slate-500">
                Showing {historyData.content.length} of {historyData.totalElements} records
              </p>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-500">Rows per page:</span>
              <select
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setPage(0);
                }}
                className="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-semibold text-slate-700"
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
            </div>
          </div>

          {/* Loading Skeleton */}
          {loading ? (
            <div className="p-12 text-center space-y-4">
              <div className="w-10 h-10 border-4 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto" />
              <p className="text-sm font-semibold text-slate-500">Loading historical task archives...</p>
            </div>
          ) : historyData.content.length === 0 ? (
            /* Empty State */
            <div className="p-12 text-center space-y-4">
              <div className="w-16 h-16 bg-slate-100 text-slate-400 rounded-2xl flex items-center justify-center mx-auto">
                <History className="w-8 h-8" />
              </div>
              <h3 className="text-base font-black text-slate-800">No Job History Found</h3>
              <p className="text-sm text-slate-500 max-w-md mx-auto">
                No historical field operations match your active filter criteria. Try adjusting the date range or status filters.
              </p>
              <button
                onClick={handleResetFilters}
                className="inline-flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition-colors"
              >
                <RefreshCw className="w-4 h-4" /> Reset All Filters
              </button>
            </div>
          ) : (
            /* Data Table (Desktop View) */
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-slate-100 bg-slate-50 text-[11px] font-black text-slate-500 uppercase tracking-wider">
                    <th className="py-3 px-4">Job / Booking</th>
                    <th className="py-3 px-4">Machinery & Category</th>
                    <th className="py-3 px-4">Delivery Field</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Duration & Pauses</th>
                    <th className="py-3 px-4">Gross Compensation</th>
                    <th className="py-3 px-4">Rating</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-sm">
                  {historyData.content.map((job) => (
                    <tr key={job.assignmentId} className="hover:bg-slate-50/80 transition-colors group">
                      {/* Job ID & Booking */}
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900 flex items-center gap-1.5">
                          <span>Job #{job.assignmentId}</span>
                        </div>
                        <div className="text-xs text-slate-400">
                          Booking #{job.bookingId || '—'}
                        </div>
                        <div className="text-[11px] text-slate-500 mt-0.5">
                          {job.bookingStartDate || '—'}
                        </div>
                      </td>

                      {/* Machinery */}
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-800 flex items-center gap-1.5">
                          <Tractor className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                          <span className="truncate max-w-[180px]">{job.equipmentName}</span>
                        </div>
                        <div className="text-xs text-slate-500 mt-0.5">
                          {job.equipmentModel} • <span className="text-emerald-700 font-semibold">{job.equipmentCategory}</span>
                        </div>
                      </td>

                      {/* Delivery Field Location */}
                      <td className="py-3.5 px-4">
                        <div className="text-xs text-slate-700 font-medium flex items-center gap-1 truncate max-w-[200px]" title={job.deliveryAddress}>
                          <MapPin className="w-3.5 h-3.5 text-slate-400 flex-shrink-0" />
                          <span>{job.deliveryAddress}</span>
                        </div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          Partner: {job.partnerName}
                        </div>
                      </td>

                      {/* Status */}
                      <td className="py-3.5 px-4">
                        <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-black border ${getStatusBadgeClass(job.assignmentStatus)}`}>
                          {job.assignmentStatus}
                        </span>
                      </td>

                      {/* Duration */}
                      <td className="py-3.5 px-4">
                        <div className="font-bold text-slate-900 flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5 text-indigo-500" />
                          <span>{job.netWorkHours ?? '0.00'} hrs</span>
                        </div>
                        <div className="text-xs text-slate-500 mt-0.5 flex items-center gap-1">
                          <span>Elapsed: {job.totalElapsedMinutes || 0}m</span>
                          {job.totalPausedMinutes > 0 && (
                            <span className="text-amber-600 font-semibold">• Pause: {job.totalPausedMinutes}m</span>
                          )}
                        </div>
                      </td>

                      {/* Gross Earnings */}
                      <td className="py-3.5 px-4">
                        <div className="font-black text-emerald-700 flex items-center">
                          <IndianRupee className="w-3.5 h-3.5" />
                          <span>{Number(job.grossEarnings || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
                        </div>
                        <div className="text-[11px] text-slate-400 mt-0.5">
                          @ ₹{job.hourlyRate || '500.00'}/hr
                        </div>
                      </td>

                      {/* Customer Rating */}
                      <td className="py-3.5 px-4">
                        {job.customerRating ? (
                          <div className="inline-flex items-center gap-1 px-2 py-0.5 bg-amber-50 border border-amber-200 text-amber-800 rounded-lg text-xs font-bold">
                            <span>{job.customerRating}</span>
                            <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-500" />
                          </div>
                        ) : (
                          <span className="text-xs text-slate-400 font-medium italic">Pending</span>
                        )}
                      </td>

                      {/* Actions */}
                      <td className="py-3.5 px-4 text-right">
                        <button
                          onClick={() => setSelectedJob(job)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-emerald-50 text-slate-700 hover:text-emerald-700 rounded-lg text-xs font-bold transition-all border border-slate-200"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          Breakdown
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Server-Side Pagination Footer */}
          {historyData.totalPages > 1 && (
            <div className="p-4 border-t border-slate-100 flex items-center justify-between bg-slate-50/50">
              <span className="text-xs font-bold text-slate-500">
                Page {page + 1} of {historyData.totalPages} ({historyData.totalElements} Total)
              </span>
              <div className="flex items-center gap-2">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="p-2 rounded-lg bg-white border border-slate-200 text-slate-700 font-bold text-xs disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <span className="px-3 py-1 bg-emerald-700 text-white rounded-lg text-xs font-bold">
                  {page + 1}
                </span>
                <button
                  disabled={page + 1 >= historyData.totalPages}
                  onClick={() => setPage((p) => p + 1)}
                  className="p-2 rounded-lg bg-white border border-slate-200 text-slate-700 font-bold text-xs disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-100 transition-colors"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Detailed Performance Breakdown Modal */}
        {selectedJob && (
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95 duration-150">
              {/* Modal Header */}
              <div className="p-6 border-b border-slate-100 flex items-center justify-between sticky top-0 bg-white/95 backdrop-blur-md z-10">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 bg-emerald-100 text-emerald-800 rounded-xl">
                    <Tractor className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="text-lg font-black text-slate-900">
                      Operation #{selectedJob.assignmentId} Details
                    </h3>
                    <p className="text-xs text-slate-500">
                      Booking #{selectedJob.bookingId} • {selectedJob.equipmentName}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setSelectedJob(null)}
                  className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-6 space-y-6">

                {/* Status & Key Metrics */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100">
                    <p className="text-[11px] font-bold text-slate-400 uppercase">Status</p>
                    <span className={`inline-block mt-1 px-2.5 py-0.5 rounded-full text-xs font-black border ${getStatusBadgeClass(selectedJob.assignmentStatus)}`}>
                      {selectedJob.assignmentStatus}
                    </span>
                  </div>
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100">
                    <p className="text-[11px] font-bold text-slate-400 uppercase">Net Duration</p>
                    <p className="text-base font-black text-slate-900 mt-1">{selectedJob.netWorkHours ?? '0.00'} hrs</p>
                  </div>
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100">
                    <p className="text-[11px] font-bold text-slate-400 uppercase">Gross Pay</p>
                    <p className="text-base font-black text-emerald-700 mt-1">
                      ₹{Number(selectedJob.grossEarnings || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                    </p>
                  </div>
                  <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-100">
                    <p className="text-[11px] font-bold text-slate-400 uppercase">Customer Rating</p>
                    <div className="flex items-center gap-1 mt-1">
                      <span className="text-base font-black text-amber-600">{selectedJob.customerRating || 'Unrated'}</span>
                      {selectedJob.customerRating && <Star className="w-4 h-4 fill-amber-400 text-amber-500" />}
                    </div>
                  </div>
                </div>

                {/* Machinery & Field Delivery Specs */}
                <div className="bg-slate-50/70 p-4 rounded-2xl border border-slate-100 space-y-2">
                  <h4 className="text-xs font-black text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4 text-emerald-600" /> Operational Context
                  </h4>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                    <div>
                      <span className="text-slate-400 font-medium">Machinery Registration:</span>
                      <p className="font-bold text-slate-800">{selectedJob.equipmentRegistrationNumber || 'N/A'}</p>
                    </div>
                    <div>
                      <span className="text-slate-400 font-medium">Partner Organization:</span>
                      <p className="font-bold text-slate-800">{selectedJob.partnerName}</p>
                    </div>
                    <div className="sm:col-span-2">
                      <span className="text-slate-400 font-medium">Delivery Field Location:</span>
                      <p className="font-bold text-slate-800 flex items-center gap-1 mt-0.5">
                        <MapPin className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                        {selectedJob.deliveryAddress}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Audited Lifecycle Timestamps */}
                <div className="space-y-3">
                  <h4 className="text-xs font-black text-slate-500 uppercase tracking-wider flex items-center gap-1.5">
                    <Clock className="w-4 h-4 text-indigo-600" /> Lifecycle Audit Timestamps
                  </h4>
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5 text-xs">
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Assigned</span>
                      <span className="font-bold text-slate-800">{formatDateTime(selectedJob.assignedAt)}</span>
                    </div>
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Accepted</span>
                      <span className="font-bold text-slate-800">{formatDateTime(selectedJob.acceptedAt)}</span>
                    </div>
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Started Travel</span>
                      <span className="font-bold text-slate-800">{formatDateTime(selectedJob.travelingAt)}</span>
                    </div>
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Reached Farm</span>
                      <span className="font-bold text-slate-800">{formatDateTime(selectedJob.reachedAt)}</span>
                    </div>
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Work Started</span>
                      <span className="font-bold text-slate-800">{formatDateTime(selectedJob.workStartedAt)}</span>
                    </div>
                    <div className="p-2.5 bg-slate-50 rounded-xl">
                      <span className="text-slate-400 font-medium block">Completed</span>
                      <span className="font-bold text-emerald-700">{formatDateTime(selectedJob.completedAt)}</span>
                    </div>
                  </div>
                </div>

                {/* Pause Breakdown & Reason */}
                {selectedJob.totalPausedMinutes > 0 && (
                  <div className="p-4 bg-amber-50/70 border border-amber-200/70 rounded-2xl space-y-1">
                    <div className="flex items-center gap-2 text-amber-800 font-bold text-xs">
                      <PauseCircle className="w-4 h-4 text-amber-600" />
                      <span>Pause Duration Logged: {selectedJob.totalPausedMinutes} minutes</span>
                    </div>
                    {selectedJob.pauseReason && (
                      <p className="text-xs text-amber-900 mt-1 italic pl-6">
                        Reason: "{selectedJob.pauseReason}"
                      </p>
                    )}
                  </div>
                )}

                {/* Customer Review & Comment */}
                {selectedJob.customerReview && (
                  <div className="p-4 bg-emerald-50/70 border border-emerald-200/70 rounded-2xl space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-black text-emerald-900 flex items-center gap-1.5">
                        <Star className="w-4 h-4 fill-amber-400 text-amber-500" /> Customer Feedback
                      </span>
                      <span className="text-[11px] text-emerald-700 font-medium">
                        {formatDateTime(selectedJob.reviewSubmittedAt)}
                      </span>
                    </div>
                    <p className="text-xs text-emerald-950 italic">
                      "{selectedJob.customerReview}"
                    </p>
                  </div>
                )}

                {/* Spatial / GPS Coordinates if recorded */}
                {selectedJob.hasGpsData && selectedJob.latestLatitude && (
                  <div className="p-3 bg-slate-50 rounded-2xl border border-slate-100 flex items-center justify-between text-xs text-slate-600">
                    <span className="font-medium flex items-center gap-1.5">
                      <MapPin className="w-4 h-4 text-emerald-600" />
                      Recorded GPS: {selectedJob.latestLatitude.toFixed(5)}, {selectedJob.latestLongitude.toFixed(5)}
                    </span>
                    <span className="text-[11px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                      Audited
                    </span>
                  </div>
                )}
              </div>

              {/* Modal Footer */}
              <div className="p-5 border-t border-slate-100 bg-slate-50/50 flex items-center justify-between">
                <Link
                  to={`/operator/jobs/${selectedJob.assignmentId}`}
                  className="text-xs font-bold text-emerald-700 hover:text-emerald-800 underline"
                >
                  Go to Job Details Page →
                </Link>
                <button
                  onClick={() => setSelectedJob(null)}
                  className="px-5 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold transition-all"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
