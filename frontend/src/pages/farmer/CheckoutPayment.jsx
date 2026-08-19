import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  CreditCard, ShieldCheck, CheckCircle2, AlertCircle, ArrowLeft,
  Lock, Smartphone, Building, Wallet, FileText, RefreshCw
} from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';

function CheckoutPayment() {
  const { id } = useParams();
  const navigate = useNavigate();
  const farmerId = getFarmerId() || 1;

  const [booking, setBooking] = useState(null);
  const [existingPayment, setExistingPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [paymentSuccess, setPaymentSuccess] = useState(null);

  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [upiId, setUpiId] = useState('farmer@upi');
  const [cardNumber, setCardNumber] = useState('4111 2222 3333 4444');
  const [expiry, setExpiry] = useState('12/28');
  const [cvv, setCvv] = useState('123');

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

  const calculateSubtotal = () => {
    const total = booking?.totalCost ? Number(booking.totalCost) : 1500;
    return (total * 0.82).toFixed(2);
  };

  const calculateGST = () => {
    const total = booking?.totalCost ? Number(booking.totalCost) : 1500;
    return (total - Number(calculateSubtotal())).toFixed(2);
  };

  const handlePaymentSubmit = async (e) => {
    e.preventDefault();

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        bookingId: Number(id),
        farmerId: Number(farmerId),
        amount: booking?.totalCost ? Number(booking.totalCost) : 1500,
        paymentMethod: paymentMethod,
        paymentDetails: paymentMethod === 'UPI' ? upiId : `Card ending in ${cardNumber.slice(-4)}`,
      };

      const payment = await paymentService.processPayment(payload);

      setPaymentSuccess({
        transactionId: payment.transactionId || 'TXN-' + Date.now(),
        invoiceReference: payment.invoiceReference || 'INV-2026-00010',
        amount: payload.amount,
      });

      setTimeout(() => {
        navigate(`/farmer/bookings/${id}/invoice`);
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to process online payment. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Securing payment gateway...</p>
        </div>
      </div>
    );
  }

  if (error && !booking) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white border border-slate-200 rounded-3xl text-center space-y-4 font-sans">
        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
        <h2 className="text-xl font-extrabold text-slate-900">Checkout Error</h2>
        <p className="text-xs text-slate-500">{error}</p>
        <Link
          to="/farmer/bookings"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-600 text-white text-xs font-bold rounded-xl hover:bg-emerald-700"
        >
          <ArrowLeft className="w-4 h-4" /> Return to My Bookings
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <Link to={`/farmer/bookings`} className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700 hover:text-emerald-800 mb-2">
            <ArrowLeft className="h-4 w-4" /> Back to My Bookings
          </Link>
          <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">Checkout & Online Payment</h1>
          <p className="text-sm text-slate-600">Complete secure payment for Equipment Booking #{id}</p>
        </div>

        <span className="flex items-center gap-1.5 px-3 py-1 bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-full text-xs font-bold">
          <ShieldCheck className="w-4 h-4 text-emerald-600" />
          256-Bit SSL Encrypted
        </span>
      </div>

      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <span>{error}</span>
        </div>
      )}

      {existingPayment && (existingPayment.paymentStatus === 'SUCCESS' || existingPayment.status === 'SUCCESS') ? (
        <div className="bg-white rounded-3xl border border-emerald-200 p-8 text-center space-y-4 shadow-sm">
          <CheckCircle2 className="w-16 h-16 text-emerald-600 mx-auto" />
          <h2 className="text-2xl font-black text-slate-900">Payment Already Completed!</h2>
          <p className="text-xs text-slate-600 max-w-md mx-auto">
            Payment for Reservation #{booking?.id || id} was successfully processed.
          </p>

          <div className="pt-2 flex justify-center gap-3">
            <Link
              to={`/farmer/bookings/${id}/invoice`}
              className="inline-flex items-center gap-2 px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition"
            >
              <FileText className="w-4 h-4" /> View Tax Invoice
            </Link>
            <Link
              to="/farmer/bookings"
              className="inline-flex items-center gap-2 px-6 py-3 bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-bold rounded-xl transition"
            >
              <ArrowLeft className="w-4 h-4" /> Back to My Bookings
            </Link>
          </div>
        </div>
      ) : paymentSuccess ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-8 text-center shadow-sm">
          <CheckCircle2 className="mx-auto h-16 w-16 text-emerald-600 mb-4 animate-bounce" />
          <h2 className="text-2xl font-extrabold text-emerald-950 mb-2">Payment Successful!</h2>
          <p className="text-sm text-emerald-800 mb-6">Your transaction has been verified and confirmed by AgroRental Payment Gateway.</p>

          <div className="mx-auto max-w-md rounded-xl bg-white p-4 text-left border border-emerald-200 text-xs space-y-2 mb-6 shadow-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">Transaction ID:</span>
              <span className="font-bold text-slate-800">{paymentSuccess.transactionId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Invoice Reference:</span>
              <span className="font-bold text-emerald-700">{paymentSuccess.invoiceReference}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Amount Paid:</span>
              <span className="font-extrabold text-emerald-800">₹{paymentSuccess.amount}</span>
            </div>
          </div>

          <p className="text-xs text-slate-500 mb-4">Redirecting to your printable GST Tax Invoice...</p>
          <Link
            to={`/farmer/bookings/${id}/invoice`}
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-6 py-3 text-sm font-bold text-white shadow-md transition hover:bg-emerald-700"
          >
            <FileText className="h-4 w-4" /> View & Download Invoice
          </Link>
        </div>
      ) : (
        <div className="grid gap-8 lg:grid-cols-3">
          {/* Order Breakdown */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-1">
            <h2 className="text-lg font-bold text-slate-900 mb-4">Order Summary</h2>

            {booking && (
              <div className="space-y-3 text-xs text-slate-600 border-b border-slate-100 pb-4 mb-4">
                <div className="flex justify-between">
                  <span>Equipment:</span>
                  <span className="font-bold text-slate-800">{booking.equipmentName || `Equipment #${booking.equipmentId}`}</span>
                </div>
                <div className="flex justify-between">
                  <span>Dates:</span>
                  <span className="font-medium text-slate-800">{booking.startDate} to {booking.endDate}</span>
                </div>
                <div className="flex justify-between">
                  <span>Delivery Address:</span>
                  <span className="font-medium text-slate-800 text-right">{booking.deliveryAddress || 'Farm Location'}</span>
                </div>
              </div>
            )}

            <div className="space-y-2.5 text-sm text-slate-700">
              <div className="flex justify-between">
                <span>Base Rental Fee:</span>
                <span>₹{calculateSubtotal()}</span>
              </div>
              <div className="flex justify-between">
                <span>GST (18% Included):</span>
                <span>₹{calculateGST()}</span>
              </div>
              <div className="flex justify-between text-xs text-emerald-700 font-semibold">
                <span>Platform Support Fee:</span>
                <span>FREE (Waived)</span>
              </div>
              <div className="border-t border-slate-200 pt-3 flex justify-between text-lg font-bold text-slate-900">
                <span>Total Amount:</span>
                <span className="text-emerald-700">₹{booking?.totalCost || 1500}</span>
              </div>
            </div>

            <div className="mt-6 rounded-xl bg-slate-50 p-3.5 border border-slate-200 flex items-center gap-2 text-xs text-slate-600">
              <Lock className="h-4 w-4 text-emerald-600 shrink-0" />
              <span>256-bit SSL Encrypted Secure Checkout</span>
            </div>
          </div>

          {/* Payment Form */}
          <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-2">
            <form onSubmit={handlePaymentSubmit} className="space-y-6">
              <h2 className="text-lg font-bold text-slate-900">Select Payment Method</h2>

              {/* Payment Methods Grid */}
              <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                {[
                  { id: 'UPI', label: 'UPI / QR', icon: Smartphone },
                  { id: 'DEBIT_CARD', label: 'Debit Card', icon: CreditCard },
                  { id: 'CREDIT_CARD', label: 'Credit Card', icon: CreditCard },
                  { id: 'NET_BANKING', label: 'NetBanking', icon: Building },
                ].map((method) => {
                  const Icon = method.icon;
                  const isSelected = paymentMethod === method.id;
                  return (
                    <button
                      key={method.id}
                      type="button"
                      onClick={() => setPaymentMethod(method.id)}
                      className={`flex flex-col items-center justify-center gap-2 rounded-xl border p-3.5 text-xs font-semibold transition ${
                        isSelected
                          ? 'border-emerald-600 bg-emerald-50 text-emerald-900 shadow-sm font-extrabold'
                          : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      <Icon className={`h-5 w-5 ${isSelected ? 'text-emerald-600' : 'text-slate-400'}`} />
                      {method.label}
                    </button>
                  );
                })}
              </div>

              {/* Dynamic Payment Fields */}
              {paymentMethod === 'UPI' && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
                  <label className="block text-xs font-bold text-slate-700">Enter VPA / UPI ID (Google Pay, PhonePe, Paytm)</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. farmer@upi or 9876543210@ybl"
                    value={upiId}
                    onChange={(e) => setUpiId(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white font-semibold"
                  />
                  <p className="text-[11px] text-slate-500">You will receive a payment request notification on your UPI app.</p>
                </div>
              )}

              {(paymentMethod === 'DEBIT_CARD' || paymentMethod === 'CREDIT_CARD') && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Card Number</label>
                    <input
                      type="text"
                      required
                      value={cardNumber}
                      onChange={(e) => setCardNumber(e.target.value)}
                      className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white font-semibold"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">Expiry Date</label>
                      <input
                        type="text"
                        required
                        placeholder="MM/YY"
                        value={expiry}
                        onChange={(e) => setExpiry(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white font-semibold"
                      />
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-slate-700 mb-1">CVV / CVC</label>
                      <input
                        type="password"
                        required
                        maxLength={4}
                        placeholder="123"
                        value={cvv}
                        onChange={(e) => setCvv(e.target.value)}
                        className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white font-semibold"
                      />
                    </div>
                  </div>
                </div>
              )}

              {paymentMethod === 'NET_BANKING' && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
                  <label className="block text-xs font-bold text-slate-700">Select Bank</label>
                  <select className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white font-semibold">
                    <option>State Bank of India (SBI)</option>
                    <option>HDFC Bank</option>
                    <option>ICICI Bank</option>
                    <option>Bank of Baroda</option>
                    <option>Axis Bank</option>
                  </select>
                </div>
              )}

              {/* Action Button */}
              <button
                type="submit"
                disabled={submitting}
                className="w-full rounded-xl bg-emerald-600 py-3.5 text-sm font-bold text-white shadow-lg shadow-emerald-900/10 transition hover:bg-emerald-700 disabled:opacity-50 flex items-center justify-center gap-2"
              >
                {submitting ? (
                  <>
                    <RefreshCw className="h-4 w-4 animate-spin" />
                    <span>Processing Secure Payment...</span>
                  </>
                ) : (
                  <>
                    <ShieldCheck className="h-5 w-5" />
                    <span>Pay ₹{booking?.totalCost || 1500} & Confirm Booking</span>
                  </>
                )}
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default CheckoutPayment;
