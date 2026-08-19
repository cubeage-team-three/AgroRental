import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { CreditCard, ShieldCheck, CheckCircle, AlertCircle, ArrowLeft, Lock, Smartphone, Building, Wallet, FileText } from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { paymentService } from '../../services/paymentService';

function CheckoutPayment() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [paymentSuccess, setPaymentSuccess] = useState(null);

  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [upiId, setUpiId] = useState('farmer@upi');
  const [cardNumber, setCardNumber] = useState('4111 2222 3333 4444');
  const [expiry, setExpiry] = useState('12/28');
  const [cvv, setCvv] = useState('123');

  const farmerId = 1; // Default mock farmer ID

  useEffect(() => {
    const fetchBookingDetails = async () => {
      try {
        setLoading(true);
        setError(null);
        if (id) {
          const data = await bookingService.getBookingById(id);
          setBooking(data);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load booking details for payment.');
      } finally {
        setLoading(false);
      }
    };

    fetchBookingDetails();
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
      }, 2500);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to process online payment. Please try again.');
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

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <Link to={`/farmer/bookings/${id}`} className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700 hover:text-emerald-800 mb-2">
            <ArrowLeft className="h-4 w-4" /> Back to Booking Details
          </Link>
          <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">Module 10 — Checkout & Online Payment</h1>
          <p className="text-sm text-slate-600">Complete secure payment for Equipment Booking #{id}</p>
        </div>
      </div>

      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <span>{error}</span>
        </div>
      )}

      {paymentSuccess ? (
        <div className="rounded-2xl border border-emerald-200 bg-emerald-50/80 p-8 text-center shadow-sm">
          <CheckCircle className="mx-auto h-16 w-16 text-emerald-600 mb-4 animate-bounce" />
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
                  <span className="font-bold text-slate-800">{booking.equipmentName}</span>
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
                          ? 'border-emerald-600 bg-emerald-50 text-emerald-900 shadow-sm'
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
                    className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white"
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
                      className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white"
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
                        className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white"
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
                        className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white"
                      />
                    </div>
                  </div>
                </div>
              )}

              {paymentMethod === 'NET_BANKING' && (
                <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4 space-y-3">
                  <label className="block text-xs font-bold text-slate-700">Select Bank</label>
                  <select className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm focus:border-emerald-500 focus:outline-none bg-white">
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
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                    <span>Processing Payment...</span>
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
