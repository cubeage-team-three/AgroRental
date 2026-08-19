import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, AlertCircle, CheckCircle, Clock, XCircle, Search, Sprout, CreditCard, Star } from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { getFarmerId } from '../../services/authService';

function MyBookings() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  const farmerId = getFarmerId();

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await bookingService.getBookingsByFarmer(farmerId);
      setBookings(data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch your bookings.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const handleCancel = async (id) => {
    if (!window.confirm('Are you sure you want to cancel this equipment reservation?')) return;

    try {
      await bookingService.cancelBooking(id);
      setActionMessage(`Booking #${id} has been cancelled successfully.`);
      fetchBookings();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel booking.');
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800"><CheckCircle className="h-3.5 w-3.5" /> Confirmed</span>;
      case 'PENDING':
        return <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800"><Clock className="h-3.5 w-3.5" /> Pending</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-semibold text-red-800"><XCircle className="h-3.5 w-3.5" /> Cancelled</span>;
      case 'COMPLETED':
        return <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-800"><CheckCircle className="h-3.5 w-3.5" /> Completed</span>;
      default:
        return <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-700">{status}</span>;
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading your equipment reservations...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">My Equipment Bookings</h1>
          <p className="text-sm text-slate-600">Track and manage your agricultural machinery reservations</p>
        </div>
        <Link
          to="/farmer/search-equipment"
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

      {bookings.length === 0 ? (
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <Calendar className="mx-auto h-12 w-12 text-slate-400 mb-3" />
          <h3 className="text-lg font-bold text-slate-900 mb-1">No Bookings Found</h3>
          <p className="text-sm text-slate-500 mb-4">You haven't reserved any machinery yet.</p>
          <Link
            to="/farmer/search-equipment"
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-700"
          >
            Explore Equipment Marketplace
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking) => (
            <div key={booking.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div className="flex gap-4">
                  {booking.primaryImageUrl ? (
                    <img
                      src={booking.primaryImageUrl}
                      alt={booking.equipmentName}
                      className="h-20 w-24 rounded-xl object-cover"
                    />
                  ) : (
                    <div className="flex h-20 w-24 items-center justify-center rounded-xl bg-slate-100 text-xs text-slate-400">
                      No Image
                    </div>
                  )}

                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-bold text-slate-400">#{booking.id}</span>
                      {getStatusBadge(booking.status)}
                    </div>
                    <h3 className="text-lg font-bold text-slate-900">{booking.equipmentName}</h3>
                    <p className="text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-2">{booking.equipmentCategory}</p>

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

                <div className="text-right space-y-2">
                  <p className="text-xs text-slate-500">Total Rental Cost</p>
                  <p className="text-xl font-bold text-emerald-700">₹{booking.totalCost}</p>

                  <div className="flex flex-col items-end gap-2 pt-1">
                    {booking.status === 'CONFIRMED' && (
                      <Link
                        to={`/farmer/bookings/${booking.id}/pay`}
                        className="inline-flex items-center gap-1.5 px-4 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition"
                      >
                        <CreditCard className="w-4 h-4" /> Pay Now
                      </Link>
                    )}

                    {booking.status === 'COMPLETED' && (
                      <Link
                        to={`/farmer/bookings/${booking.id}/review`}
                        className="inline-flex items-center gap-1.5 px-4 py-2 bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold rounded-xl shadow-xs transition"
                      >
                        <Star className="w-4 h-4 fill-white" /> Rate Experience
                      </Link>
                    )}

                    {booking.status !== 'CANCELLED' && booking.status !== 'COMPLETED' && (
                      <button
                        onClick={() => handleCancel(booking.id)}
                        className="rounded-xl border border-red-200 px-3 py-1.5 text-xs font-semibold text-red-600 transition hover:bg-red-50 hover:border-red-300"
                      >
                        Cancel Reservation
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
