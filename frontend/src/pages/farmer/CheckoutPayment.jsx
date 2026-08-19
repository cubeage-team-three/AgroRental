import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  CreditCard,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Calendar,
  MapPin,
  Tractor,
  DollarSign,
  Lock,
  RefreshCw,
  Wallet,
} from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';

function CheckoutPayment() {
  const { id } = useParams();
  const navigate = useNavigate();
  const farmerId = getFarmerId();

  const [booking, setBooking] = useState(null);
  const [existingPayment, setExistingPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [paymentResult, setPaymentResult] = useState(null);

  useEffect(() => {
    async function loadCheckoutData() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const bookingData = await bookingService.getBookingById(id);
        setBooking(bookingData);

        try {
          const paymentData = await paymentService.getPaymentByBookingId(id);
          setExistingPayment(paymentData);
        } catch {
          // Payment not created yet
        }
      } catch (err) {
        console.error('Failed to load checkout details:', err);
        setError(err.message || 'Booking reservation not found.');
      } finally {
        setLoading(false);
      }
    }

    loadCheckoutData();
  }, [id]);

  const handleProcessPayment = async (simulateFailure = false) => {
    if (!booking) return;
    setProcessing(true);
    setError(null);
    try {
      const response = await paymentService.createPayment({
        bookingId: booking.id,
        farmerId,
        amount: booking.totalCost,
        paymentMethod: `SIMULATED_${paymentMethod}`,
        simulateFailure,
      });

      setPaymentResult(response);
      setExistingPayment(response);
    } catch (err) {
      console.error('Payment processing failed:', err);
      setError(err.message || 'Payment execution failed.');
    } finally {
      setProcessing(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto p-8 space-y-6 font-sans animate-pulse">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="h-64 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  if (error && !booking) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white border border-gray-100 rounded-3xl text-center space-y-4 font-sans">
        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
        <h2 className="text-xl font-extrabold text-gray-900">Checkout Error</h2>
        <p className="text-xs text-gray-500">{error}</p>
        <Link
          to="/farmer/my-bookings"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-700 text-white text-xs font-bold rounded-xl hover:bg-emerald-800"
        >
          <ArrowLeft className="w-4 h-4" /> Return to My Bookings
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      
      {/* Navigation Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div className="flex items-center gap-3">
          <Link
            to="/farmer/my-bookings"
            className="p-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100"
          >
            <ArrowLeft className="w-4 h-4" />
          </Link>
          <div>
            <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
              Machinery Rental Checkout
            </h1>
            <p className="text-xs text-gray-500">Secure simulated rental tariff settlement for Reservation #{booking.id}</p>
          </div>
        </div>

        <span className="flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-full text-xs font-bold">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          256-Bit Encrypted
        </span>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-xs font-bold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Already Paid Banner */}
      {existingPayment && existingPayment.paymentStatus === 'SUCCESS' ? (
        <div className="bg-white rounded-3xl border border-emerald-200 p-8 text-center space-y-4 shadow-sm">
          <CheckCircle2 className="w-16 h-16 text-emerald-600 mx-auto" />
          <h2 className="text-2xl font-black text-slate-900">Payment Already Completed!</h2>
          <p className="text-xs text-slate-600 max-w-md mx-auto">
            Payment for Reservation #{booking.id} was successfully processed on{' '}
            {new Date(existingPayment.createdAt).toLocaleDateString()}.
          </p>

          <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 max-w-md mx-auto text-xs space-y-1.5 text-left font-mono">
            <div className="flex justify-between">
              <span className="text-slate-500">Reference ID:</span>
              <span className="font-bold text-emerald-900">{existingPayment.paymentReference}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Amount Paid:</span>
              <span className="font-bold text-emerald-900">₹{existingPayment.amount}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Payment Method:</span>
              <span className="font-bold text-emerald-900">{existingPayment.paymentMethod}</span>
            </div>
          </div>

          <div className="pt-2">
            <Link
              to="/farmer/my-bookings"
              className="inline-flex items-center gap-2 px-6 py-3 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-md transition"
            >
              <ArrowLeft className="w-4 h-4" /> Back to My Bookings
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-5 gap-8">
          
          {/* Order Summary Column */}
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-3xl border border-gray-100 p-6 space-y-4 shadow-sm">
              <h3 className="text-xs font-black uppercase text-gray-400 tracking-wider border-b border-gray-100 pb-2">
                Reservation Order Summary
              </h3>

              <div className="space-y-3 text-xs">
                <div>
                  <span className="text-gray-400 block font-bold text-[10px] uppercase">Machinery Equipment</span>
                  <span className="font-black text-gray-900 text-base">{booking.equipmentName || `Machine #${booking.equipmentId}`}</span>
                </div>

                {booking.farmName && (
                  <div>
                    <span className="text-gray-400 block font-bold text-[10px] uppercase">Target Land Parcel</span>
                    <span className="font-bold text-emerald-800">{booking.farmName}</span>
                  </div>
                )}

                <div>
                  <span className="text-gray-400 block font-bold text-[10px] uppercase">Schedule Period</span>
                  <span className="font-bold text-gray-800">{booking.startDate} → {booking.endDate}</span>
                </div>

                <div className="pt-3 border-t border-gray-100 flex justify-between items-center text-sm">
                  <span className="font-bold text-gray-600">Total Payable Tariff</span>
                  <span className="text-2xl font-black text-emerald-700">₹{booking.totalCost}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Payment Selection Column */}
          <div className="lg:col-span-3 bg-white rounded-3xl border border-gray-100 p-6 sm:p-8 space-y-6 shadow-sm">
            <h3 className="text-lg font-black text-gray-900">Select Payment Method</h3>

            <div className="grid grid-cols-3 gap-3">
              <button
                type="button"
                onClick={() => setPaymentMethod('UPI')}
                className={`p-4 rounded-2xl border text-center transition-all ${
                  paymentMethod === 'UPI'
                    ? 'border-emerald-600 bg-emerald-50/60 ring-2 ring-emerald-100 font-extrabold text-emerald-900'
                    : 'border-gray-200 hover:border-gray-300 text-gray-600'
                }`}
              >
                <Wallet className="w-5 h-5 mx-auto mb-1.5 text-emerald-600" />
                <span className="text-xs block">UPI / QR</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMethod('CARD')}
                className={`p-4 rounded-2xl border text-center transition-all ${
                  paymentMethod === 'CARD'
                    ? 'border-emerald-600 bg-emerald-50/60 ring-2 ring-emerald-100 font-extrabold text-emerald-900'
                    : 'border-gray-200 hover:border-gray-300 text-gray-600'
                }`}
              >
                <CreditCard className="w-5 h-5 mx-auto mb-1.5 text-blue-600" />
                <span className="text-xs block">Debit / Credit</span>
              </button>

              <button
                type="button"
                onClick={() => setPaymentMethod('NETBANKING')}
                className={`p-4 rounded-2xl border text-center transition-all ${
                  paymentMethod === 'NETBANKING'
                    ? 'border-emerald-600 bg-emerald-50/60 ring-2 ring-emerald-100 font-extrabold text-emerald-900'
                    : 'border-gray-200 hover:border-gray-300 text-gray-600'
                }`}
              >
                <Lock className="w-5 h-5 mx-auto mb-1.5 text-purple-600" />
                <span className="text-xs block">Net Banking</span>
              </button>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl text-xs space-y-2">
              <div className="flex items-center gap-2 text-slate-800 font-bold">
                <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Simulated Sandbox Payment Gateway</span>
              </div>
              <p className="text-slate-500 leading-relaxed text-[11px]">
                This action executes a simulated rental payment transaction against the AgroRental backend service.
              </p>
            </div>

            <div className="space-y-3 pt-2">
              <button
                type="button"
                disabled={processing}
                onClick={() => handleProcessPayment(false)}
                className="w-full py-3.5 bg-emerald-700 hover:bg-emerald-800 text-white font-extrabold text-xs sm:text-sm rounded-xl shadow-md transition flex items-center justify-center gap-2 disabled:opacity-70"
              >
                {processing ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Processing Payment...</span>
                  </>
                ) : (
                  <>
                    <Lock className="w-4 h-4" />
                    <span>Pay ₹{booking.totalCost} & Complete Reservation</span>
                  </>
                )}
              </button>

              <button
                type="button"
                disabled={processing}
                onClick={() => handleProcessPayment(true)}
                className="w-full py-2 bg-red-50 hover:bg-red-100 text-red-700 text-xs font-bold rounded-xl border border-red-200 transition"
              >
                Simulate Payment Failure (Testing)
              </button>
            </div>
          </div>

        </div>
      )}

    </div>
  );
}

export default CheckoutPayment;
