import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Calendar, MapPin, CheckCircle, Clock, XCircle, ArrowLeft, CreditCard, FileText, UserCheck, ShieldCheck, Truck, AlertCircle } from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function BookingDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  const fetchBooking = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await bookingService.getBookingById(id);
      setBooking(data);
    } catch (err) {
      setError(err.response?.data?.message || `Failed to fetch booking details for #${id}.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (id) fetchBooking();
  }, [id]);

  const handleCancel = async () => {
    if (!window.confirm('Are you sure you want to cancel this equipment reservation?')) return;

    try {
      setError(null);
      await bookingService.cancelBooking(id);
      setActionMessage(`Booking #${id} has been cancelled successfully.`);
      fetchBooking();
    } catch (err) {
      setError(err.data?.message || err.message || 'Failed to cancel booking.');
    }
  };

  const getStepStatusIndex = (status) => {
    switch (status) {
      case 'PENDING': return 1;
      case 'CONFIRMED':
      case 'ACCEPTED': return 2;
      case 'OPERATOR_ASSIGNED': return 3;
      case 'WORK_STARTED':
      case 'ON_THE_WAY': return 4;
      case 'COMPLETED': return 5;
      case 'CANCELLED': return -1;
      default: return 1;
    }
  };

  const currentStep = booking ? getStepStatusIndex(booking.status) : 1;

  const steps = [
    { label: 'Booking Requested', desc: 'Sent to Equipment Owner' },
    { label: 'Owner Confirmed', desc: 'Partner Accepted Request' },
    { label: 'Operator Assigned', desc: 'Driver Assigned to Machine' },
    { label: 'Service In Progress', desc: 'Machine En Route / Working' },
    { label: 'Completed', desc: 'Field Job Finished' },
  ];

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading booking reservation details...</p>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="mx-auto max-w-3xl p-6">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center shadow-sm">
          <AlertCircle className="mx-auto h-12 w-12 text-red-600 mb-3" />
          <h2 className="text-xl font-bold text-red-900 mb-2">Booking Not Found</h2>
          <p className="text-red-800 mb-4">{error || 'Reservation ID does not exist.'}</p>
          <Link
            to="/farmer/my-bookings"
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md transition hover:bg-emerald-700"
          >
            <ArrowLeft className="h-4 w-4" /> Back to My Bookings
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      {/* Top Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-sm border border-slate-200 h-11 shrink-0">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
          <div>
            <Link to="/farmer/my-bookings" className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700 hover:text-emerald-800 mb-1">
              <ArrowLeft className="h-4 w-4" /> Back to Bookings List
            </Link>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">Booking Reservation #{booking.id}</h1>
              <span className={`inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold ${
                booking.status === 'CONFIRMED' ? 'bg-emerald-100 text-emerald-800' :
                booking.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                booking.status === 'CANCELLED' ? 'bg-red-100 text-red-800' : 'bg-amber-100 text-amber-800'
              }`}>
                {booking.status}
              </span>
            </div>
            <p className="text-sm text-slate-600 mt-1">Created on {new Date(booking.createdAt || Date.now()).toLocaleDateString()}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {booking.status !== 'CANCELLED' && (
            <Link
              to={`/farmer/bookings/${booking.id}/pay`}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700"
            >
              <CreditCard className="h-4 w-4" /> Proceed to Checkout
            </Link>
          )}

          <Link
            to={`/farmer/bookings/${booking.id}/invoice`}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:bg-slate-50"
          >
            <FileText className="h-4 w-4 text-emerald-600" /> Tax Invoice
          </Link>
        </div>
      </div>

      {actionMessage && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0" />
          <span>{actionMessage}</span>
        </div>
      )}

      {/* Visual Status Step Indicator */}
      {currentStep !== -1 ? (
        <div className="mb-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-500 mb-6">Service Progress & Lifecycle</h2>
          <div className="relative flex flex-col justify-between gap-6 md:flex-row md:items-center">
            {steps.map((step, idx) => {
              const stepNum = idx + 1;
              const isCompleted = stepNum <= currentStep;
              const isCurrent = stepNum === currentStep;

              return (
                <div key={step.label} className="flex items-center gap-3 md:flex-col md:text-center z-10 flex-1">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full text-sm font-bold transition-all ${
                    isCompleted ? 'bg-emerald-600 text-white shadow-md' : 'bg-slate-100 text-slate-400 border border-slate-200'
                  }`}>
                    {isCompleted ? <CheckCircle className="h-5 w-5" /> : stepNum}
                  </div>
                  <div>
                    <p className={`text-xs font-bold ${isCurrent ? 'text-emerald-700' : isCompleted ? 'text-slate-900' : 'text-slate-400'}`}>
                      {step.label}
                    </p>
                    <p className="text-[11px] text-slate-500">{step.desc}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ) : (
        <div className="mb-8 rounded-2xl border border-red-200 bg-red-50 p-6 text-center shadow-sm">
          <XCircle className="mx-auto h-10 w-10 text-red-600 mb-2" />
          <h3 className="text-lg font-bold text-red-900">Reservation Cancelled</h3>
          <p className="text-xs text-red-700">This machinery booking was cancelled before fulfillment.</p>
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Machine & Partner Info */}
        <div className="space-y-6 lg:col-span-1">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Equipment Details</h2>
            {booking.primaryImageUrl ? (
              <img src={booking.primaryImageUrl} alt={booking.equipmentName} className="h-44 w-full rounded-xl object-cover mb-4" />
            ) : (
              <div className="flex h-44 items-center justify-center rounded-xl bg-slate-100 text-slate-400 mb-4">
                No Image Available
              </div>
            )}
            <h3 className="text-lg font-bold text-slate-900">{booking.equipmentName}</h3>
            <p className="text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-3">{booking.equipmentCategory}</p>

            <div className="space-y-2 text-xs text-slate-600 border-t border-slate-100 pt-3">
              <div className="flex justify-between">
                <span>Machine ID:</span>
                <span className="font-semibold text-slate-800">#{booking.equipmentId}</span>
              </div>
              <div className="flex justify-between">
                <span>Owner Partner ID:</span>
                <span className="font-semibold text-slate-800">Partner #{booking.partnerId || 1}</span>
              </div>
              <div className="flex justify-between">
                <span>Assigned Operator:</span>
                <span className="font-semibold text-slate-800">{booking.operatorId ? `Operator #${booking.operatorId}` : 'Pending Assignment'}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Detailed Reservation Summary */}
        <div className="space-y-6 lg:col-span-2">
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Rental Reservation Summary</h2>

            <div className="grid gap-4 sm:grid-cols-2 text-sm text-slate-700">
              <div className="rounded-xl bg-slate-50 p-4 border border-slate-100">
                <div className="flex items-center gap-2 font-semibold text-emerald-800 mb-1">
                  <Calendar className="h-4 w-4" /> Service Dates
                </div>
                <p className="font-bold text-slate-900">{booking.startDate} to {booking.endDate}</p>
              </div>

              <div className="rounded-xl bg-slate-50 p-4 border border-slate-100">
                <div className="flex items-center gap-2 font-semibold text-emerald-800 mb-1">
                  <MapPin className="h-4 w-4" /> Delivery Location
                </div>
                <p className="font-bold text-slate-900">{booking.deliveryAddress || 'Registered Farm Location'}</p>
              </div>
            </div>

            {booking.notes && (
              <div className="mt-4 rounded-xl bg-slate-50 p-4 border border-slate-100 text-xs">
                <span className="font-bold text-slate-700 block mb-1">Work Description / Instructions:</span>
                <p className="text-slate-600 italic">{booking.notes}</p>
              </div>
            )}

            <div className="mt-6 rounded-2xl bg-emerald-900 p-5 text-white flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase font-semibold text-emerald-200">Total Rental Cost</p>
                <p className="text-3xl font-extrabold text-white">₹{booking.totalCost}</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Link
                  to={`/farmer/bookings/${booking.id}/tracking`}
                  className="rounded-xl border border-emerald-400/40 bg-emerald-700/60 px-4 py-2.5 text-xs font-bold text-emerald-100 hover:bg-emerald-600 hover:text-white transition flex items-center gap-1.5"
                >
                  <MapPin className="h-4 w-4 text-emerald-300" /> Live Tracking
                </Link>

                <Link
                  to={`/farmer/complaints?bookingId=${booking.id}`}
                  className="rounded-xl border border-amber-400/40 bg-amber-600/30 px-4 py-2.5 text-xs font-bold text-amber-200 hover:bg-amber-600 hover:text-white transition flex items-center gap-1.5"
                >
                  <AlertCircle className="h-4 w-4 text-amber-300" /> Log Complaint
                </Link>

                {booking.status !== 'CANCELLED' && booking.status !== 'COMPLETED' && (
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="rounded-xl border border-red-300/40 bg-red-600/30 px-4 py-2.5 text-xs font-bold text-red-200 hover:bg-red-600 hover:text-white transition"
                  >
                    Cancel Booking
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default BookingDetails;
