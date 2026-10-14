import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  CalendarRange,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  IndianRupee,
  Tractor,
  Search,
  RotateCw,
  Eye,
  ChevronLeft,
  ChevronRight,
  User,
  Phone,
  Mail,
  MapPin,
  Building2,
  HardHat,
  ShieldCheck,
  X,
  Calendar,
} from 'lucide-react';
import { adminService } from '../../services/adminService';

const STATUS_STYLES = {
  PENDING: 'border-amber-200 bg-amber-50 text-amber-800',
  ACCEPTED: 'border-sky-200 bg-sky-50 text-sky-800',
  CONFIRMED: 'border-indigo-200 bg-indigo-50 text-indigo-800',
  OPERATOR_ASSIGNED: 'border-violet-200 bg-violet-50 text-violet-800',
  ON_THE_WAY: 'border-blue-200 bg-blue-50 text-blue-800',
  WORK_STARTED: 'border-cyan-200 bg-cyan-50 text-cyan-800',
  COMPLETED: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  CANCELLED: 'border-slate-200 bg-slate-100 text-slate-600',
  REJECTED: 'border-red-200 bg-red-50 text-red-800',
};

const PAYMENT_STATUS_STYLES = {
  SUCCESS: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  PAID: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  PENDING: 'border-amber-200 bg-amber-50 text-amber-800',
  FAILED: 'border-red-200 bg-red-50 text-red-800',
  REFUNDED: 'border-purple-200 bg-purple-50 text-purple-800',
};

const STATUS_TABS = [
  { id: 'ALL', label: 'All Bookings' },
  { id: 'PENDING', label: 'Pending' },
  { id: 'ACCEPTED', label: 'Accepted' },
  { id: 'CONFIRMED', label: 'Confirmed' },
  { id: 'OPERATOR_ASSIGNED', label: 'Operator Assigned' },
  { id: 'ON_THE_WAY', label: 'On The Way' },
  { id: 'WORK_STARTED', label: 'In Progress' },
  { id: 'COMPLETED', label: 'Completed' },
  { id: 'CANCELLED', label: 'Cancelled' },
  { id: 'REJECTED', label: 'Declined' },
];

function formatINR(value) {
  if (value === null || value === undefined) return '0';
  return Number(value).toLocaleString('en-IN');
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A';
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

function calculateDays(startDate, endDate) {
  if (!startDate || !endDate) return 1;
  const start = new Date(startDate);
  const end = new Date(endDate);
  const diffTime = Math.abs(end - start);
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  return diffDays || 1;
}

export default function ManageBookings() {
  // Data states
  const [bookings, setBookings] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successToast, setSuccessToast] = useState('');

  // Filters & Pagination
  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [startDateFilter, setStartDateFilter] = useState('');
  const [endDateFilter, setEndDateFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Drawer & Action modals
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [cancelModalBooking, setCancelModalBooking] = useState(null);
  const [completeModalBooking, setCompleteModalBooking] = useState(null);
  const [actionSubmitting, setActionSubmitting] = useState(false);

  const activeRequestId = useRef(0);

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
    }, 400);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Fetch summary stats
  const fetchStats = useCallback(async () => {
    setStatsLoading(true);
    try {
      const data = await adminService.getBookingStats();
      setStats(data || null);
    } catch (err) {
      console.error('Failed to fetch booking stats:', err);
    } finally {
      setStatsLoading(false);
    }
  }, []);

  // Fetch paginated bookings
  const fetchBookings = useCallback(async (targetPage = 0) => {
    const currentRequestId = ++activeRequestId.current;
    setLoading(true);
    setError(null);

    try {
      const params = {
        page: targetPage,
        size: 10,
        sort: 'createdAt,desc',
      };
      if (activeTab !== 'ALL') {
        params.status = activeTab;
      }
      if (debouncedSearch.trim()) {
        params.search = debouncedSearch.trim();
      }
      if (startDateFilter) {
        params.startDate = startDateFilter;
      }
      if (endDateFilter) {
        params.endDate = endDateFilter;
      }

      const response = await adminService.getBookings(params);

      if (currentRequestId === activeRequestId.current) {
        if (response && response.content) {
          setBookings(response.content);
          setTotalPages(response.totalPages || 1);
          setTotalElements(response.totalElements || 0);
          setPage(targetPage);
        } else if (Array.isArray(response)) {
          setBookings(response);
          setTotalPages(1);
          setTotalElements(response.length);
          setPage(0);
        } else {
          setBookings([]);
          setTotalPages(1);
          setTotalElements(0);
        }
      }
    } catch (err) {
      if (currentRequestId === activeRequestId.current) {
        console.error('Failed to fetch bookings:', err);
        setError(err.message || 'Failed to retrieve bookings from server.');
      }
    } finally {
      if (currentRequestId === activeRequestId.current) {
        setLoading(false);
      }
    }
  }, [activeTab, debouncedSearch, startDateFilter, endDateFilter]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    fetchBookings(0);
  }, [fetchBookings]);

  // Manual refresh action
  const handleRefresh = async () => {
    await Promise.all([fetchStats(), fetchBookings(page)]);
  };

  // Open booking detail drawer
  const handleOpenDetails = async (booking) => {
    setSelectedBooking(booking);
    setDetailsLoading(true);
    try {
      const detail = await adminService.getBookingById(booking.id);
      if (detail) {
        setSelectedBooking(detail);
      }
    } catch (err) {
      console.error('Failed to load full booking details:', err);
    } finally {
      setDetailsLoading(false);
    }
  };

  // Confirm cancel booking action
  const handleConfirmCancel = async () => {
    if (!cancelModalBooking || actionSubmitting) return;
    setActionSubmitting(true);
    setError(null);
    try {
      const updated = await adminService.cancelBooking(cancelModalBooking.id);
      setSuccessToast(`✓ Booking #${cancelModalBooking.id} cancelled successfully.`);
      setCancelModalBooking(null);

      // If drawer was open for this booking, update it
      if (selectedBooking && selectedBooking.id === cancelModalBooking.id) {
        setSelectedBooking(updated);
      }

      // Refresh list & stats
      await Promise.all([fetchStats(), fetchBookings(page)]);
    } catch (err) {
      console.error('Failed to cancel booking:', err);
      setError(err.message || `Failed to cancel booking #${cancelModalBooking.id}`);
    } finally {
      setActionSubmitting(false);
    }
  };

  // Confirm complete booking action
  const handleConfirmComplete = async () => {
    if (!completeModalBooking || actionSubmitting) return;
    setActionSubmitting(true);
    setError(null);
    try {
      const updated = await adminService.updateBookingStatus(completeModalBooking.id, {
        status: 'COMPLETED',
      });
      setSuccessToast(`✓ Booking #${completeModalBooking.id} marked as Completed successfully.`);
      setCompleteModalBooking(null);

      // If drawer was open for this booking, update it
      if (selectedBooking && selectedBooking.id === completeModalBooking.id) {
        setSelectedBooking(updated);
      }

      // Refresh list & stats
      await Promise.all([fetchStats(), fetchBookings(page)]);
    } catch (err) {
      console.error('Failed to complete booking:', err);
      setError(err.message || `Failed to complete booking #${completeModalBooking.id}`);
    } finally {
      setActionSubmitting(false);
    }
  };

  // Clear all filters
  const handleClearFilters = () => {
    setActiveTab('ALL');
    setSearchQuery('');
    setStartDateFilter('');
    setEndDateFilter('');
  };

  const isFiltered =
    activeTab !== 'ALL' ||
    debouncedSearch.trim() !== '' ||
    startDateFilter !== '' ||
    endDateFilter !== '';

  return (
    <div className="space-y-6">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-0.5 text-[11px] font-bold text-emerald-800">
            <ShieldCheck className="h-3.5 w-3.5" />
            AgroRent Admin Panel
          </span>
          <h1 className="mt-2 text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900">
            Booking Management
          </h1>
          <p className="mt-1 text-xs sm:text-sm text-slate-500">
            Monitor machinery reservations, track lifecycle statuses, and manage booking operations platform-wide.
          </p>
        </div>

        <button
          type="button"
          onClick={handleRefresh}
          disabled={loading || statsLoading}
          className="inline-flex items-center gap-2 self-start rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 shadow-2xs transition hover:bg-slate-50 hover:text-slate-900 disabled:opacity-50 sm:self-auto"
        >
          <RotateCw className={`h-3.5 w-3.5 ${loading || statsLoading ? 'animate-spin text-emerald-600' : ''}`} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Success Notification */}
      {successToast && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-xs font-bold text-emerald-900 shadow-2xs"
        >
          <div className="flex items-center gap-2.5">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600" />
            <span>{successToast}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessToast('')}
            className="text-emerald-700 hover:text-emerald-900 font-bold"
          >
            ✕
          </button>
        </motion.div>
      )}

      {/* Error Alert */}
      {error && (
        <motion.div
          initial={{ opacity: 0, y: -8 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center justify-between rounded-2xl border border-red-200 bg-red-50 p-4 text-xs font-bold text-red-900 shadow-2xs"
        >
          <div className="flex items-center gap-2.5">
            <AlertCircle className="h-4 w-4 shrink-0 text-red-600" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => fetchBookings(page)}
            className="rounded-lg bg-red-200/60 px-2.5 py-1 text-xs text-red-900 hover:bg-red-200 font-bold"
          >
            Retry
          </button>
        </motion.div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Total Bookings</p>
            <div className="w-8 h-8 rounded-xl bg-slate-50 text-slate-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <CalendarRange className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-slate-900">
            {statsLoading ? '—' : stats?.totalBookings ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">All reservations</p>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-amber-700">Pending</p>
            <div className="w-8 h-8 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Clock className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-amber-700">
            {statsLoading ? '—' : stats?.pendingBookings ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">Awaiting acceptance</p>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-sky-700">Active</p>
            <div className="w-8 h-8 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <Tractor className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-sky-700">
            {statsLoading ? '—' : stats?.activeBookings ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">Confirmed or in field</p>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-emerald-700">Completed</p>
            <div className="w-8 h-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <CheckCircle2 className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-emerald-700">
            {statsLoading ? '—' : stats?.completedBookings ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">Successfully serviced</p>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-red-700">Cancelled</p>
            <div className="w-8 h-8 rounded-xl bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <XCircle className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-red-700">
            {statsLoading ? '—' : stats?.cancelledBookings ?? 0}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">Cancelled / declined</p>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 shadow-sm p-4 sm:p-5 hover:border-emerald-600 hover:shadow-md transition-all duration-200 group">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-wider text-purple-700">Total Volume</p>
            <div className="w-8 h-8 rounded-xl bg-purple-50 text-purple-600 flex items-center justify-center group-hover:scale-110 transition-transform">
              <IndianRupee className="h-4 w-4" />
            </div>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-purple-700">
            {statsLoading ? '—' : `₹${formatINR(stats?.totalAmount || stats?.totalRevenue)}`}
          </p>
          <p className="mt-1 text-[11px] text-slate-400 font-medium">Rental order value</p>
        </div>
      </div>

      {/* Filter, Search & Date Controls Bar */}
      <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm space-y-4">
        {/* Status Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 bg-slate-100 p-1.5 rounded-2xl border border-slate-200/80">
          {STATUS_TABS.map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
                className={`rounded-xl px-3.5 py-1.5 text-xs font-bold transition-all shrink-0 ${
                  active
                    ? 'bg-lime-400 text-emerald-950 shadow-sm font-extrabold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-white/60'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* Search & Date Controls Bar */}
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between border-t border-slate-100 pt-4">
          {/* Search Box */}
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by ID, equipment, farmer, owner, or address…"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/50 pl-10 pr-4 py-2 text-xs text-slate-800 placeholder-slate-400 focus:bg-white focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 focus:outline-none transition-all shadow-2xs"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 text-xs"
              >
                ✕
              </button>
            )}
          </div>

          {/* Date Range Filters */}
          <div className="flex flex-wrap items-center gap-2.5">
            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-xs text-slate-600 shadow-2xs">
              <Calendar className="h-3.5 w-3.5 text-slate-400" />
              <span className="text-slate-400 font-medium">From:</span>
              <input
                type="date"
                value={startDateFilter}
                onChange={(e) => setStartDateFilter(e.target.value)}
                className="bg-transparent text-slate-800 text-xs focus:outline-none font-semibold"
              />
            </div>

            <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/50 px-3 py-1.5 text-xs text-slate-600 shadow-2xs">
              <Calendar className="h-3.5 w-3.5 text-slate-400" />
              <span className="text-slate-400 font-medium">To:</span>
              <input
                type="date"
                value={endDateFilter}
                onChange={(e) => setEndDateFilter(e.target.value)}
                className="bg-transparent text-slate-800 text-xs focus:outline-none font-semibold"
              />
            </div>

            {isFiltered && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 hover:bg-slate-50 shadow-2xs"
              >
                Clear Filters
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Bookings Table Card */}
      <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200 bg-slate-50 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                <th className="px-5 py-4">Booking ID</th>
                <th className="px-5 py-4">Farmer</th>
                <th className="px-5 py-4">Equipment</th>
                <th className="px-5 py-4">Equipment Owner</th>
                <th className="px-5 py-4">Rental Dates</th>
                <th className="px-5 py-4">Amount</th>
                <th className="px-5 py-4">Payment</th>
                <th className="px-5 py-4">Status</th>
                <th className="px-5 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {loading ? (
                // Loading Skeleton Rows
                Array.from({ length: 5 }).map((_, idx) => (
                  <tr key={`skeleton-${idx}`} className="animate-pulse">
                    <td className="px-5 py-4">
                      <div className="h-4 w-14 rounded bg-slate-200" />
                      <div className="mt-1 h-3 w-20 rounded bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-4 w-28 rounded bg-slate-200" />
                      <div className="mt-1 h-3 w-20 rounded bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-4 w-32 rounded bg-slate-200" />
                      <div className="mt-1 h-3 w-16 rounded bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-4 w-24 rounded bg-slate-200" />
                      <div className="mt-1 h-3 w-20 rounded bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-4 w-28 rounded bg-slate-200" />
                      <div className="mt-1 h-3 w-14 rounded bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-4 w-16 rounded bg-slate-200" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-5 w-16 rounded-full bg-slate-100" />
                    </td>
                    <td className="px-5 py-4">
                      <div className="h-5 w-20 rounded-full bg-slate-100" />
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="inline-block h-7 w-16 rounded-lg bg-slate-100" />
                    </td>
                  </tr>
                ))
              ) : bookings.length === 0 ? (
                // Empty State
                <tr>
                  <td colSpan={9} className="px-5 py-16 text-center">
                    <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl border border-slate-200 bg-slate-50 text-slate-400 shadow-2xs">
                      <Tractor className="h-7 w-7" />
                    </div>
                    <p className="mt-4 text-sm font-bold text-slate-900">No Bookings Found</p>
                    <p className="mt-1 text-xs text-slate-500 max-w-sm mx-auto">
                      {isFiltered
                        ? 'No reservations match your search or filter criteria. Try clearing some filters.'
                        : 'There are currently no bookings registered on the platform.'}
                    </p>
                    {isFiltered && (
                      <button
                        type="button"
                        onClick={handleClearFilters}
                        className="mt-4 inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-bold text-emerald-800 hover:bg-slate-50 shadow-2xs"
                      >
                        Reset All Filters
                      </button>
                    )}
                  </td>
                </tr>
              ) : (
                // Data Rows
                bookings.map((booking) => {
                  const statusStyle =
                    STATUS_STYLES[booking.status] || 'border-slate-200 bg-slate-100 text-slate-600';
                  const paymentStyle =
                    PAYMENT_STATUS_STYLES[booking.paymentStatus] ||
                    'border-slate-200 bg-slate-100 text-slate-600';

                  const rentalDays = calculateDays(booking.startDate, booking.endDate);
                  const isCancellable =
                    booking.status !== 'CANCELLED' &&
                    booking.status !== 'REJECTED' &&
                    booking.status !== 'COMPLETED';
                  const isCompletable =
                    booking.status === 'WORK_STARTED' ||
                    booking.status === 'ON_THE_WAY';

                  return (
                    <tr
                      key={booking.id}
                      className="transition-colors hover:bg-slate-50/80"
                    >
                      {/* Booking ID & Date */}
                      <td className="px-5 py-4">
                        <span className="font-mono font-bold text-slate-900">#{booking.id}</span>
                        <p className="mt-0.5 text-[11px] text-slate-400">
                          {formatDate(booking.createdAt)}
                        </p>
                      </td>

                      {/* Farmer */}
                      <td className="px-5 py-4">
                        <div className="font-bold text-slate-900">
                          {booking.farmerName || `Farmer #${booking.farmerId}`}
                        </div>
                        <div className="mt-0.5 text-[11px] text-slate-400 flex items-center gap-1">
                          {booking.farmerMobile || `ID: ${booking.farmerId}`}
                        </div>
                      </td>

                      {/* Equipment */}
                      <td className="px-5 py-4">
                        <div className="flex items-center gap-2.5">
                          {booking.primaryImageUrl ? (
                            <img
                              src={booking.primaryImageUrl}
                              alt={booking.equipmentName}
                              className="h-8 w-8 rounded-lg object-cover border border-slate-200 shrink-0 shadow-2xs"
                            />
                          ) : (
                            <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 bg-emerald-50 text-emerald-700 shrink-0">
                              <Tractor className="h-4 w-4" />
                            </div>
                          )}
                          <div className="min-w-0">
                            <p className="truncate font-bold text-slate-900">{booking.equipmentName || 'Equipment'}</p>
                            <p className="truncate text-[11px] text-slate-400">{booking.equipmentCategory || 'Machinery'}</p>
                          </div>
                        </div>
                      </td>

                      {/* Equipment Owner / Partner */}
                      <td className="px-5 py-4">
                        <div className="font-bold text-slate-900">
                          {booking.partnerBusinessName || booking.partnerName || (booking.partnerId ? `Partner #${booking.partnerId}` : '—')}
                        </div>
                        {booking.partnerBusinessName && booking.partnerName && (
                          <p className="text-[11px] text-slate-500">{booking.partnerName}</p>
                        )}
                        {booking.partnerMobile && (
                          <p className="text-[11px] text-slate-400">{booking.partnerMobile}</p>
                        )}
                      </td>

                      {/* Dates & Duration */}
                      <td className="px-5 py-4">
                        <div className="font-semibold text-slate-900">
                          {formatDate(booking.startDate)} → {formatDate(booking.endDate)}
                        </div>
                        <p className="mt-0.5 text-[11px] text-slate-400 font-medium">
                          {rentalDays} {rentalDays === 1 ? 'day' : 'days'}
                        </p>
                      </td>

                      {/* Amount */}
                      <td className="px-5 py-4 font-mono font-bold text-slate-900">
                        ₹{formatINR(booking.totalCost)}
                      </td>

                      {/* Payment Status */}
                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${paymentStyle}`}
                        >
                          {booking.paymentStatus || 'UNPAID'}
                        </span>
                      </td>

                      {/* Booking Status */}
                      <td className="px-5 py-4">
                        <span
                          className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${statusStyle}`}
                        >
                          {booking.status?.replace(/_/g, ' ')}
                        </span>
                      </td>

                      {/* Action buttons */}
                      <td className="px-5 py-4 text-right">
                        <div className="inline-flex items-center gap-1.5">
                          <button
                            type="button"
                            onClick={() => handleOpenDetails(booking)}
                            title="View Booking Details"
                            className="flex h-7 items-center gap-1 rounded-lg border border-slate-200 bg-white px-2.5 text-[11px] font-bold text-slate-700 hover:bg-slate-50 hover:text-slate-900 shadow-2xs"
                          >
                            <Eye className="h-3.5 w-3.5 text-slate-500" />
                            <span>Details</span>
                          </button>

                          {isCompletable && (
                            <button
                              type="button"
                              onClick={() => setCompleteModalBooking(booking)}
                              title="Mark Booking Completed"
                              className="flex h-7 items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2 text-[11px] font-bold text-emerald-800 hover:bg-emerald-100 shadow-2xs"
                            >
                              <CheckCircle2 className="h-3.5 w-3.5" />
                              <span>Complete</span>
                            </button>
                          )}

                          {isCancellable && (
                            <button
                              type="button"
                              onClick={() => setCancelModalBooking(booking)}
                              title="Cancel Booking"
                              className="flex h-7 items-center gap-1 rounded-lg border border-red-200 bg-red-50 px-2 text-[11px] font-bold text-red-800 hover:bg-red-100 shadow-2xs"
                            >
                              <XCircle className="h-3.5 w-3.5" />
                              <span>Cancel</span>
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-t border-slate-100 bg-slate-50/50 px-5 py-4">
          <p className="text-xs text-slate-500">
            Showing <span className="font-bold text-slate-900">{bookings.length}</span> of{' '}
            <span className="font-bold text-slate-900">{totalElements}</span> total bookings
          </p>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => fetchBookings(page - 1)}
              disabled={page <= 0 || loading}
              className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 hover:bg-slate-100 shadow-2xs disabled:opacity-40"
            >
              <ChevronLeft className="h-3.5 w-3.5" />
              <span>Previous</span>
            </button>

            <span className="px-2 text-xs text-slate-600 font-medium">
              Page <span className="font-bold text-slate-900">{page + 1}</span> of{' '}
              <span className="font-bold text-slate-900">{Math.max(totalPages, 1)}</span>
            </span>

            <button
              type="button"
              onClick={() => fetchBookings(page + 1)}
              disabled={page >= totalPages - 1 || loading}
              className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-slate-700 hover:bg-slate-100 shadow-2xs disabled:opacity-40"
            >
              <span>Next</span>
              <ChevronRight className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Booking Details Drawer / Modal */}
      <AnimatePresence>
        {selectedBooking && (
          <div className="fixed inset-0 z-50 flex items-center justify-end bg-black/50 backdrop-blur-xs p-0 sm:p-4">
            <motion.div
              initial={{ opacity: 0, x: 80 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 80 }}
              transition={{ duration: 0.25, ease: 'easeOut' }}
              className="flex h-full w-full max-w-2xl flex-col border border-slate-200 bg-white shadow-2xl overflow-hidden sm:rounded-3xl"
            >
              {/* Drawer Header */}
              <div className="flex items-center justify-between border-b border-slate-100 bg-white p-5">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-2xl border border-slate-200 bg-emerald-50 text-emerald-700">
                    <CalendarRange className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-lg font-extrabold text-slate-900">
                        Booking #{selectedBooking.id}
                      </h2>
                      <span
                        className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${
                          STATUS_STYLES[selectedBooking.status] || 'border-slate-200 bg-slate-100 text-slate-600'
                        }`}
                      >
                        {selectedBooking.status?.replace(/_/g, ' ')}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400">
                      Created on {formatDate(selectedBooking.createdAt)}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => setSelectedBooking(null)}
                  className="flex h-8 w-8 items-center justify-center rounded-xl border border-slate-200 text-slate-400 hover:text-slate-700 hover:bg-slate-50"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {/* Drawer Content */}
              <div className="flex-1 overflow-y-auto p-5 sm:p-6 space-y-5 text-xs text-slate-700 bg-white">
                {detailsLoading ? (
                  <div className="flex items-center justify-center py-20 text-slate-400">
                    <RotateCw className="h-6 w-6 animate-spin text-emerald-600 mr-2" />
                    <span>Loading booking details…</span>
                  </div>
                ) : (
                  <>
                    {/* Equipment Details Card */}
                    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-3">
                      <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-slate-500">
                        <Tractor className="h-4 w-4 text-emerald-700" />
                        <span>Equipment Information</span>
                      </div>
                      <div className="flex items-start gap-4 pt-1">
                        {selectedBooking.primaryImageUrl ? (
                          <img
                            src={selectedBooking.primaryImageUrl}
                            alt={selectedBooking.equipmentName}
                            className="h-20 w-20 rounded-xl object-cover border border-slate-200 shrink-0 shadow-2xs"
                          />
                        ) : (
                          <div className="flex h-20 w-20 items-center justify-center rounded-xl border border-slate-200 bg-emerald-50 text-emerald-700 shrink-0">
                            <Tractor className="h-8 w-8" />
                          </div>
                        )}
                        <div className="space-y-1">
                          <h3 className="text-base font-bold text-slate-900">
                            {selectedBooking.equipmentName || 'Equipment Machinery'}
                          </h3>
                          <p className="text-slate-600">
                            Category: <span className="font-bold text-slate-900">{selectedBooking.equipmentCategory || 'Standard'}</span>
                          </p>
                          <p className="text-slate-400">
                            Equipment ID: <span className="font-mono text-slate-700">#{selectedBooking.equipmentId}</span>
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* Schedule & Financial Summary */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {/* Schedule */}
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-2">
                        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          <Calendar className="h-3.5 w-3.5 text-sky-600" />
                          <span>Rental Schedule</span>
                        </div>
                        <div className="pt-1">
                          <p className="text-xs text-slate-500">Start Date</p>
                          <p className="font-bold text-slate-900">{formatDate(selectedBooking.startDate)}</p>
                        </div>
                        <div>
                          <p className="text-xs text-slate-500">End Date</p>
                          <p className="font-bold text-slate-900">{formatDate(selectedBooking.endDate)}</p>
                        </div>
                        <p className="text-[11px] text-slate-500 pt-1 border-t border-slate-200">
                          Duration:{' '}
                          <span className="font-bold text-slate-900">
                            {calculateDays(selectedBooking.startDate, selectedBooking.endDate)} days
                          </span>
                        </p>
                      </div>

                      {/* Financials */}
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-2">
                        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          <IndianRupee className="h-3.5 w-3.5 text-violet-600" />
                          <span>Cost & Payment</span>
                        </div>
                        <div className="pt-1">
                          <p className="text-xs text-slate-500">Total Cost</p>
                          <p className="text-xl font-extrabold text-emerald-800">
                            ₹{formatINR(selectedBooking.totalCost)}
                          </p>
                        </div>
                        <div>
                          <p className="text-xs text-slate-500">Payment Status</p>
                          <span
                            className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide mt-1 ${
                              PAYMENT_STATUS_STYLES[selectedBooking.paymentStatus] ||
                              'border-slate-200 bg-slate-100 text-slate-600'
                            }`}
                          >
                            {selectedBooking.paymentStatus || 'UNPAID'}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Parties Involved: Farmer & Owner */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {/* Farmer Profile */}
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-2.5">
                        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          <User className="h-3.5 w-3.5 text-emerald-700" />
                          <span>Farmer Details</span>
                        </div>
                        <div>
                          <p className="font-bold text-slate-900">
                            {selectedBooking.farmerName || `Farmer #${selectedBooking.farmerId}`}
                          </p>
                          {selectedBooking.farmerMobile && (
                            <p className="text-slate-600 flex items-center gap-1.5 mt-0.5">
                              <Phone className="h-3 w-3 text-slate-400" />
                              {selectedBooking.farmerMobile}
                            </p>
                          )}
                          {selectedBooking.farmerEmail && (
                            <p className="text-slate-600 flex items-center gap-1.5 mt-0.5">
                              <Mail className="h-3 w-3 text-slate-400" />
                              {selectedBooking.farmerEmail}
                            </p>
                          )}
                        </div>
                        {selectedBooking.deliveryAddress && (
                          <div className="pt-2 border-t border-slate-200">
                            <p className="text-[11px] text-slate-500 flex items-center gap-1">
                              <MapPin className="h-3 w-3 text-slate-400" />
                              Delivery Address:
                            </p>
                            <p className="mt-0.5 text-slate-700 font-medium">{selectedBooking.deliveryAddress}</p>
                          </div>
                        )}
                      </div>

                      {/* Equipment Owner / Partner */}
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-2.5">
                        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          <Building2 className="h-3.5 w-3.5 text-sky-600" />
                          <span>Equipment Owner (Partner)</span>
                        </div>
                        <div>
                          <p className="font-bold text-slate-900">
                            {selectedBooking.partnerBusinessName || selectedBooking.partnerName || `Partner #${selectedBooking.partnerId || '—'}`}
                          </p>
                          {selectedBooking.partnerBusinessName && selectedBooking.partnerName && (
                            <p className="text-slate-600 mt-0.5">Contact: {selectedBooking.partnerName}</p>
                          )}
                          {selectedBooking.partnerMobile && (
                            <p className="text-slate-600 flex items-center gap-1.5 mt-0.5">
                              <Phone className="h-3 w-3 text-slate-400" />
                              {selectedBooking.partnerMobile}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Operator Assignment Card */}
                    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-2">
                      <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                        <HardHat className="h-3.5 w-3.5 text-amber-600" />
                        <span>Operator Assignment</span>
                      </div>
                      {selectedBooking.operatorId ? (
                        <div className="flex items-center justify-between pt-1">
                          <div>
                            <p className="font-bold text-slate-900">
                              {selectedBooking.operatorName || `Operator #${selectedBooking.operatorId}`}
                            </p>
                            {selectedBooking.operatorMobile && (
                              <p className="text-slate-600 flex items-center gap-1.5 mt-0.5">
                                <Phone className="h-3 w-3 text-slate-400" />
                                {selectedBooking.operatorMobile}
                              </p>
                            )}
                          </div>
                          <span className="rounded-xl border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-800">
                            ASSIGNED
                          </span>
                        </div>
                      ) : (
                        <p className="text-slate-400 pt-1">
                          No certified operator assigned to this booking yet.
                        </p>
                      )}
                    </div>

                    {/* Farm Details if present */}
                    {selectedBooking.farmName && (
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-1.5">
                        <div className="flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          <MapPin className="h-3.5 w-3.5 text-emerald-700" />
                          <span>Farm Profile</span>
                        </div>
                        <p className="font-bold text-slate-900">{selectedBooking.farmName}</p>
                        {selectedBooking.farmLocation && (
                          <p className="text-slate-600">{selectedBooking.farmLocation}</p>
                        )}
                      </div>
                    )}

                    {/* Notes & Rejection Reason */}
                    {selectedBooking.notes && (
                      <div className="rounded-2xl border border-slate-200 bg-slate-50/80 p-4 space-y-1">
                        <p className="text-[11px] font-bold uppercase tracking-wider text-slate-500">
                          Farmer Notes
                        </p>
                        <p className="text-slate-700 italic">&ldquo;{selectedBooking.notes}&rdquo;</p>
                      </div>
                    )}

                    {selectedBooking.rejectionReason && (
                      <div className="rounded-2xl border border-red-200 bg-red-50 p-4 space-y-1">
                        <p className="text-[11px] font-bold uppercase tracking-wider text-red-800">
                          Rejection / Decline Reason
                        </p>
                        <p className="text-red-700">{selectedBooking.rejectionReason}</p>
                      </div>
                    )}
                  </>
                )}
              </div>

              {/* Drawer Footer Actions */}
              <div className="flex flex-wrap items-center justify-between gap-2.5 border-t border-slate-100 bg-slate-50/50 p-5">
                <button
                  type="button"
                  onClick={() => setSelectedBooking(null)}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-100 shadow-2xs"
                >
                  Close Drawer
                </button>

                <div className="flex items-center gap-2">
                  {selectedBooking &&
                    (selectedBooking.status === 'WORK_STARTED' ||
                      selectedBooking.status === 'ON_THE_WAY') && (
                      <button
                        type="button"
                        onClick={() => setCompleteModalBooking(selectedBooking)}
                        className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2.5 text-xs font-bold text-emerald-800 hover:bg-emerald-100 shadow-2xs"
                      >
                        Mark as Completed
                      </button>
                    )}

                  {selectedBooking &&
                    selectedBooking.status !== 'CANCELLED' &&
                    selectedBooking.status !== 'REJECTED' &&
                    selectedBooking.status !== 'COMPLETED' && (
                      <button
                        type="button"
                        onClick={() => setCancelModalBooking(selectedBooking)}
                        className="rounded-xl border border-red-200 bg-red-50 px-4 py-2.5 text-xs font-bold text-red-800 hover:bg-red-100 shadow-2xs"
                      >
                        Cancel This Booking
                      </button>
                    )}
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Confirmation Modal for Consequential Actions (Cancel Booking) */}
      <AnimatePresence>
        {cancelModalBooking && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl space-y-4"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-red-200 bg-red-50 text-red-600 shrink-0">
                  <AlertCircle className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Cancel Booking #{cancelModalBooking.id}?
                  </h3>
                  <p className="text-xs text-slate-500">Consequential administrative action</p>
                </div>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-xs text-slate-700 space-y-2">
                <p>
                  You are about to cancel booking for{' '}
                  <span className="font-bold text-slate-900">
                    {cancelModalBooking.equipmentName || 'machinery'}
                  </span>{' '}
                  reserved by{' '}
                  <span className="font-bold text-slate-900">
                    {cancelModalBooking.farmerName || `Farmer #${cancelModalBooking.farmerId}`}
                  </span>
                  .
                </p>
                <p className="text-slate-500">
                  • Equipment availability will be restored to <span className="text-emerald-700 font-bold">AVAILABLE</span>.
                  <br />
                  • Any active operator job assignment will be cleaned up and marked cancelled.
                  <br />
                  • Notifications will be dispatched to the farmer, owner, and operator.
                </p>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setCancelModalBooking(null)}
                  disabled={actionSubmitting}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 shadow-2xs disabled:opacity-50"
                >
                  Keep Booking
                </button>
                <button
                  type="button"
                  onClick={handleConfirmCancel}
                  disabled={actionSubmitting}
                  className="inline-flex items-center gap-2 rounded-xl border border-red-300 bg-red-600 px-4 py-2 text-xs font-bold text-white hover:bg-red-700 shadow-sm disabled:opacity-50"
                >
                  {actionSubmitting && <RotateCw className="h-3.5 w-3.5 animate-spin" />}
                  <span>Confirm Cancellation</span>
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Confirmation Modal for Consequential Actions (Complete Booking) */}
      <AnimatePresence>
        {completeModalBooking && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95 }}
              className="w-full max-w-md rounded-3xl border border-slate-200 bg-white p-6 shadow-2xl space-y-4"
            >
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-emerald-200 bg-emerald-50 text-emerald-700 shrink-0">
                  <CheckCircle2 className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Mark Booking #{completeModalBooking.id} as Completed?
                  </h3>
                  <p className="text-xs text-slate-500">Consequential administrative action</p>
                </div>
              </div>

              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-xs text-slate-700 space-y-2">
                <p>
                  You are marking booking #{completeModalBooking.id} for{' '}
                  <span className="font-bold text-slate-900">
                    {completeModalBooking.equipmentName || 'machinery'}
                  </span>{' '}
                  reserved by{' '}
                  <span className="font-bold text-slate-900">
                    {completeModalBooking.farmerName || `Farmer #${completeModalBooking.farmerId}`}
                  </span>{' '}
                  as successfully completed.
                </p>
                <p className="text-slate-500">
                  • Equipment availability will be restored to <span className="text-emerald-700 font-bold">AVAILABLE</span>.
                  <br />
                  • Completion notifications will be dispatched to the farmer and equipment owner.
                </p>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setCompleteModalBooking(null)}
                  disabled={actionSubmitting}
                  className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-700 hover:bg-slate-100 shadow-2xs disabled:opacity-50"
                >
                  Dismiss
                </button>
                <button
                  type="button"
                  onClick={handleConfirmComplete}
                  disabled={actionSubmitting}
                  className="inline-flex items-center gap-2 rounded-xl border border-emerald-300 bg-emerald-700 px-4 py-2 text-xs font-bold text-white hover:bg-emerald-800 shadow-sm disabled:opacity-50"
                >
                  {actionSubmitting && <RotateCw className="h-3.5 w-3.5 animate-spin" />}
                  <span>Confirm Completion</span>
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
