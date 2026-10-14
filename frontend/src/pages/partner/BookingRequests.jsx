import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  CalendarCheck,
  User,
  MapPin,
  Calendar,
  Clock,
  CheckCircle,
  XCircle,
  HardHat,
  Filter,
  Sparkles,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Sprout,
  Phone,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { DEFAULT_EQUIPMENT_IMAGE, formatCategoryLabel } from '../../utils/constants';

function BookingRequests() {
  const partnerId = getPartnerId();

  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('ALL');
  const [actionLoading, setActionLoading] = useState(null);
  const [successToast, setSuccessToast] = useState('');

  const fetchBookings = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await bookingService.getBookingsByPartner(partnerId);
      setRequests(data || []);
    } catch (err) {
      console.error('Failed to load partner booking requests:', err);
      setError(err.message || 'Failed to retrieve booking requests.');
    } finally {
      setLoading(false);
    }
  }, [partnerId]);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  const handleAction = async (id, action) => {
    let rejectionReason = '';
    if (action === 'reject') {
      rejectionReason = window.prompt("Please provide a reason for declining this booking:");
      if (!rejectionReason || !rejectionReason.trim()) {
        alert("Rejection reason is mandatory.");
        return;
      }
    }

    setActionLoading(id);
    setSuccessToast('');
    try {
      if (action === 'accept') {
        await bookingService.acceptBooking(id);
        setSuccessToast(`✓ Booking #${id} Accepted & Confirmed!`);
      } else if (action === 'reject') {
        await bookingService.rejectBooking(id, rejectionReason.trim());
        setSuccessToast(`✓ Booking #${id} Declined.`);
      }
      
      await fetchBookings();
    } catch (err) {
      // Show backend validation/errors clearly
      alert(err.response?.data?.message || err.message || `Failed to ${action} booking`);
    } finally {
      setActionLoading(null);
    }
  };

  const filtered = requests.filter((r) => (filter === 'ALL' ? true : r.status === filter));

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Booking Requests
            </h1>
            <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-full text-[10px] font-extrabold uppercase shadow-sm">
              Live Fleet Requests
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Review incoming machinery rental orders from regional farmers and assign certified operators.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchBookings}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 text-xs font-bold rounded-xl transition-colors shadow-2xs shrink-0"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Refresh Requests</span>
        </button>
      </div>

      {/* Success Toast / Feedback */}
      {successToast && (
        <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-900 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button type="button" onClick={() => setSuccessToast('')} className="text-emerald-700 font-bold ml-2">✕</button>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchBookings} className="text-xs font-bold text-red-800 underline">
            Retry
          </button>
        </div>
      )}

      {/* Filter Tabs */}
      <div className="bg-white rounded-3xl p-3 shadow-sm border border-slate-200 flex items-center gap-1.5 overflow-x-auto text-xs">
        {['ALL', 'PENDING', 'CONFIRMED', 'OPERATOR_ASSIGNED', 'WORK_STARTED', 'COMPLETED', 'REJECTED', 'CANCELLED'].map((st) => (
          <button
            key={st}
            type="button"
            onClick={() => setFilter(st)}
            className={`px-3.5 py-1.5 rounded-xl font-bold transition-all shrink-0 ${
              filter === st
                ? 'bg-emerald-700 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            {st} ({st === 'ALL' ? requests.length : requests.filter((r) => r.status === st).length})
          </button>
        ))}
      </div>

      {/* Booking Cards Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="bg-white rounded-3xl p-4 space-y-3 animate-pulse border border-slate-200">
              <div className="w-full h-40 bg-slate-200 rounded-2xl" />
              <div className="h-5 bg-slate-200 rounded w-3/4" />
              <div className="h-4 bg-slate-200 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-white border border-dashed border-slate-300 rounded-3xl p-12 text-center space-y-4">
          <CalendarCheck className="w-12 h-12 text-slate-300 mx-auto" />
          <div className="space-y-1 max-w-md mx-auto">
            <h3 className="text-lg font-extrabold text-slate-900">No Booking Requests Found</h3>
            <p className="text-xs text-slate-500">
              {filter !== 'ALL'
                ? `No rental booking requests found with status "${filter}".`
                : 'You currently have no incoming rental bookings from farmers.'}
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filtered.map((b) => {
            const isProcessing = actionLoading === b.id;
            const imageSrc = b.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

            return (
              <div
                key={b.id}
                className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm hover:border-emerald-600 hover:shadow-md transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="relative h-40 w-full bg-slate-100 overflow-hidden">
                    <img
                      src={imageSrc}
                      alt={b.equipmentName || 'Machine'}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                      }}
                    />
                    <span className="absolute top-3 left-3 bg-[#0F382C] text-white text-[10px] font-extrabold px-2.5 py-0.5 rounded-lg uppercase tracking-wider">
                      Booking #{b.id}
                    </span>
                    <span
                      className={`absolute top-3 right-3 text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full border shadow-xs ${
                        b.status === 'CONFIRMED'
                          ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                          : b.status === 'COMPLETED'
                          ? 'bg-blue-100 text-blue-800 border-blue-300'
                          : b.status === 'CANCELLED' || b.status === 'REJECTED'
                          ? 'bg-red-100 text-red-800 border-red-300'
                          : b.status === 'OPERATOR_ASSIGNED' || b.status === 'WORK_STARTED'
                          ? 'bg-purple-100 text-purple-800 border-purple-300'
                          : 'bg-amber-100 text-amber-800 border-amber-300'
                      }`}
                    >
                      {b.status}
                    </span>
                  </div>

                  <div className="p-5 space-y-3">
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-emerald-700">
                        {formatCategoryLabel(b.equipmentCategory)}
                      </span>
                      <h3 className="text-base font-extrabold text-slate-900 line-clamp-1 mt-0.5">
                        {b.equipmentName || `Machine #${b.equipmentId}`}
                      </h3>
                    </div>

                    <div className="space-y-1.5 text-xs text-gray-600 pt-2 border-t border-gray-100 font-medium">
                      <p className="flex items-center gap-1.5 truncate">
                        <User className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                        <span>{b.farmerName || `Farmer #${b.farmerId}`}</span>
                      </p>
                      {b.farmerMobile && (
                        <p className="flex items-center gap-1.5 truncate">
                          <Phone className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                          <a href={`tel:${b.farmerMobile}`} className="text-blue-600 hover:underline">{b.farmerMobile}</a>
                        </p>
                      )}
                      {b.farmName && (
                        <p className="flex items-center gap-1.5 font-bold text-emerald-800 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200 truncate">
                          <Sprout className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                          <span>{b.farmName} {b.farmLocation ? `(${b.farmLocation})` : ''}</span>
                        </p>
                      )}
                      <p className="flex items-center gap-1.5 truncate">
                        <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                        <span>{b.startDate} → {b.endDate}</span>
                      </p>
                      <p className="flex items-center gap-1.5">
                        <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                        <span className="truncate">{b.deliveryAddress || 'Field Address Specified'}</span>
                      </p>
                      {b.operatorId && (
                        <p className="flex items-center gap-1.5 text-emerald-700 font-bold">
                          <HardHat className="w-3.5 h-3.5 shrink-0" />
                          <span>Assigned Operator: {b.operatorName || `#${b.operatorId}`}</span>
                        </p>
                      )}
                      {b.notes && (
                        <p className="mt-1 text-[11px] text-gray-500 italic bg-gray-50 p-2 rounded-lg line-clamp-3">
                          "{b.notes}"
                        </p>
                      )}
                    </div>

                    <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                      <span className="text-xs text-slate-400 font-bold uppercase">Total Payout</span>
                      <span className="text-lg font-extrabold text-slate-900">
                        ₹{Number(b.totalCost || 0).toLocaleString('en-IN')}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="p-4 bg-slate-50/70 border-t border-slate-100">
                  {b.status === 'PENDING' ? (
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        disabled={isProcessing}
                        onClick={() => handleAction(b.id, 'accept')}
                        className="flex-1 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl transition-colors shadow-2xs disabled:opacity-50"
                      >
                        {isProcessing ? 'Updating...' : 'Accept Order'}
                      </button>
                      <button
                        type="button"
                        disabled={isProcessing}
                        onClick={() => handleAction(b.id, 'reject')}
                        className="py-2 px-3 bg-white hover:bg-red-50 text-slate-600 hover:text-red-600 border border-slate-200 text-xs font-bold rounded-xl transition-colors disabled:opacity-50"
                      >
                        Decline
                      </button>
                    </div>
                  ) : b.status === 'CONFIRMED' || b.status === 'OPERATOR_ASSIGNED' ? (
                    <div className="flex flex-col gap-2">
                      <Link
                        to={`/partner/bookings/${b.id}/assign-operator`}
                        className={`w-full flex items-center justify-center gap-1.5 py-2 text-white text-xs font-bold rounded-xl transition-colors shadow-2xs ${
                          b.status === 'OPERATOR_ASSIGNED' ? 'bg-emerald-700 hover:bg-emerald-800' : 'bg-emerald-700 hover:bg-emerald-800'
                        }`}
                      >
                        <HardHat className="w-3.5 h-3.5" />
                        <span>{b.operatorId || b.status === 'OPERATOR_ASSIGNED' ? 'Driver Assigned (Manage)' : 'Assign Driver / Operator'}</span>
                      </Link>
                    </div>
                  ) : (
                    <div className="text-[11px] text-slate-400 text-center font-semibold py-1">
                      Order status: {b.status}
                    </div>
                  )}
                </div>

              </div>
            );
          })}
        </div>
      )}

    </div>
  );
}

export default BookingRequests;

