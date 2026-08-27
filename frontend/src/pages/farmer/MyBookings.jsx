import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, AlertCircle, CheckCircle, Clock, XCircle, Search, Sprout, CreditCard, FileText, Star, Eye } from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { getFarmerId } from '../../services/authService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [filteredBookings, setFilteredBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  const [activeTab, setActiveTab] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  const farmerId = getFarmerId();

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await bookingService.getBookingsByFarmer(farmerId);
      setBookings(data || []);
      setFilteredBookings(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch your bookings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  useEffect(() => {
    let result = [...bookings];

    if (activeTab !== 'ALL') {
      result = result.filter((b) => b.status === activeTab);
    }

    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      result = result.filter((b) =>
        (b.equipmentName && b.equipmentName.toLowerCase().includes(q)) ||
        (b.id && String(b.id).includes(q)) ||
        (b.equipmentCategory && b.equipmentCategory.toLowerCase().includes(q))
      );
    }

    setFilteredBookings(result);
  }, [activeTab, searchQuery, bookings]);

  const handleCancel = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this equipment reservation?')) return;

    try {
      setError(null);
      await bookingService.cancelBooking(id);
      setActionMessage(`Booking #${id} has been cancelled successfully.`);
      fetchBookings();
    } catch (err) {
      setError(err.data?.message || err.message || 'Failed to cancel booking.');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
      case 'ACCEPTED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800"><CheckCircle className="h-3.5 w-3.5 text-emerald-600" /> Confirmed</span>;
      case 'OPERATOR_ASSIGNED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-semibold text-indigo-800"><Clock className="h-3.5 w-3.5 text-indigo-600" /> Operator Assigned</span>;
      case 'ON_THE_WAY':
        return <span className="inline-flex items-center gap-1 rounded-full bg-cyan-100 px-2.5 py-0.5 text-xs font-semibold text-cyan-800"><Clock className="h-3.5 w-3.5 text-cyan-600 animate-pulse" /> On The Way</span>;
      case 'WORK_STARTED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-purple-100 px-2.5 py-0.5 text-xs font-semibold text-purple-800"><CheckCircle className="h-3.5 w-3.5 text-purple-600" /> Work Started</span>;
      case 'PENDING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800"><Clock className="h-3.5 w-3.5 text-amber-600" /> Pending Owner</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800"><XCircle className="h-3.5 w-3.5 text-red-600" /> Cancelled</span>;
      case 'REJECTED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-semibold text-rose-800"><XCircle className="h-3.5 w-3.5 text-rose-600" /> Rejected</span>;
      case 'COMPLETED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-800"><CheckCircle className="h-3.5 w-3.5 text-blue-600" /> Completed</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading your equipment reservation history...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Page Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-sm border border-slate-200 h-11 shrink-0">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">Booking History</h1>
            <p className="text-sm text-slate-600">Track current and past agricultural machinery rental requests</p>
          </div>
        </div>
        <Link
          to="/farmer/equipment"
          className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700"
        >
          <Search className="h-4 w-4" /> Rent New Machine
        </Link>
      </div>

      {actionMessage && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0" />
          <span>{actionMessage}</span>
        </div>
      )}

      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="h-5 w-5 text-red-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Filter Tabs & Search Bar */}
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        {/* Filter Tabs */}
        <div className="flex flex-wrap items-center gap-1.5 rounded-xl bg-slate-100 p-1.5 border border-slate-200 text-xs font-semibold">
          {['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`rounded-lg px-3 py-2 transition ${
                activeTab === tab
                  ? 'bg-white text-emerald-800 shadow-sm font-bold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {tab === 'ALL' ? 'All Bookings' : tab}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by ID or equipment name..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-slate-200 pl-9 pr-4 py-2 text-xs transition focus:border-emerald-500 focus:outline-none sm:w-64"
          />
        </div>
      </div>

      {filteredBookings.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <Calendar className="mx-auto h-12 w-12 text-slate-400 mb-3" />
          <h3 className="text-lg font-bold text-slate-900 mb-1">No Reservations Found</h3>
          <p className="text-sm text-slate-500 mb-4">No equipment bookings match your search or filter settings.</p>
          <Link
            to="/farmer/equipment"
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700"
          >
            Explore Equipment Marketplace
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredBookings.map((booking) => (
            <div key={booking.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex gap-4">
                  {booking.primaryImageUrl ? (
                    <img
                      src={booking.primaryImageUrl}
                      alt={booking.equipmentName}
                      className="h-24 w-28 rounded-xl object-cover"
                    />
                  ) : (
                    <div className="flex h-24 w-28 items-center justify-center rounded-xl bg-slate-100 text-xs font-bold text-slate-400">
                      🚜 Machinery
                    </div>
                  )}

                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold text-slate-400">#{booking.id}</span>
                      {getStatusBadge(booking.status)}
                    </div>
                    <h3 className="text-lg font-bold text-slate-900">{booking.equipmentName || `Equipment #${booking.equipmentId}`}</h3>
                    <p className="text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-2">{booking.equipmentCategory || 'AGRICULTURAL'}</p>

                    <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                      <span className="flex items-center gap-1 font-medium">
                        <Calendar className="h-3.5 w-3.5 text-slate-400" />
                        {booking.startDate} to {booking.endDate}
                      </span>
                      {booking.farmName && (
                        <span className="flex items-center gap-1 font-semibold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                          <Sprout className="h-3.5 w-3.5 text-emerald-600" />
                          {booking.farmName}
                        </span>
                      )}
                      {booking.deliveryAddress && (
                        <span className="flex items-center gap-1">
                          <MapPin className="h-3.5 w-3.5 text-slate-400" />
                          {booking.deliveryAddress}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="text-right flex flex-col justify-between items-end min-h-[96px]">
                  <div>
                    <p className="text-xs text-slate-500">Total Rental Cost</p>
                    <p className="text-xl font-bold text-emerald-700">₹{booking.totalCost}</p>
                  </div>

                  <div className="flex flex-wrap items-center gap-2 mt-3">
                    <Link
                      to={`/farmer/bookings/${booking.id}`}
                      className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:bg-slate-100"
                    >
                      <Eye className="h-3.5 w-3.5 text-emerald-600" /> Details
                    </Link>

                    <Link
                      to={`/farmer/bookings/${booking.id}/tracking`}
                      className="inline-flex items-center gap-1 rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-800"
                    >
                      <MapPin className="h-3.5 w-3.5 text-emerald-400" /> Track
                    </Link>
                    {booking.status !== 'CANCELLED' && (
                      <Link
                        to={`/farmer/bookings/${booking.id}/pay`}
                        className="inline-flex items-center gap-1 rounded-xl bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-700"
                      >
                        <CreditCard className="h-3.5 w-3.5" /> Pay Now
                      </Link>
                    )}

                    <Link
                      to={`/farmer/bookings/${booking.id}/invoice`}
                      className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:bg-slate-50"
                    >
                      <FileText className="h-3.5 w-3.5 text-emerald-600" /> Invoice
                    </Link>

                    {booking.status === 'COMPLETED' && (
                      <Link
                        to={`/farmer/bookings/${booking.id}/review`}
                        className="inline-flex items-center gap-1 rounded-xl bg-amber-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-amber-600"
                      >
                        <Star className="h-3.5 w-3.5 fill-white" /> Review
                      </Link>
                    )}

                    {booking.status !== 'CANCELLED' && booking.status !== 'COMPLETED' && (
                      <button
                        type="button"
                        onClick={() => handleCancel(booking.id)}
                        className="rounded-xl border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 transition hover:bg-red-50 hover:border-red-300"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MyBookings;
