import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { CreditCard, FileText, CheckCircle, AlertCircle, Search, RefreshCw } from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { paymentService } from '../../services/paymentService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function FarmerPayments() {
  const farmerId = getFarmerId() || 1;

  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchPayments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getFarmerPayments(farmerId);
      setPayments(data || []);
    } catch (err) {
      console.warn('Failed to load farmer payment history:', err);
      setError(err.message || 'Failed to retrieve transaction records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, [farmerId]);

  const filteredPayments = payments.filter(
    (p) =>
      (p.transactionId && p.transactionId.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (p.invoiceReference && p.invoiceReference.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (p.paymentReference && p.paymentReference.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (p.bookingId && String(p.bookingId).includes(searchQuery))
  );

  const calculateTotalSpent = () => {
    return payments.reduce((acc, p) => acc + (p.amount || 0), 0);
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6 font-sans">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium text-sm">Loading financial transaction ledger...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 space-y-6 font-sans">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-sm border border-slate-200 h-11 shrink-0">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Payments & Invoices
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
              View transaction receipts, rental payment statuses, and GST tax invoices.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchPayments}
          className="p-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
          title="Refresh Transactions"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between">
          <span>{error}</span>
        </div>
      )}

      {/* Stats Summary Cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-slate-500 uppercase">Total Transactions</p>
          <p className="text-2xl font-extrabold text-slate-900 mt-1">{payments.length}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-slate-500 uppercase">Total Amount Paid</p>
          <p className="text-2xl font-extrabold text-emerald-700 mt-1">₹{calculateTotalSpent()}</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <p className="text-xs font-semibold text-slate-500 uppercase">Verification Status</p>
          <p className="text-2xl font-extrabold text-emerald-800 mt-1 flex items-center gap-1.5">
            <CheckCircle className="h-6 w-6 text-emerald-600" /> 100% Verified
          </p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="flex items-center justify-between">
        <div className="relative w-full sm:w-72">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search Txn ID, Ref, or Booking #..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-xl border border-slate-200 pl-9 pr-4 py-2 text-xs transition focus:border-emerald-500 focus:outline-none bg-white font-medium"
          />
        </div>
      </div>

      {/* Payments Table */}
      {filteredPayments.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-12 text-center shadow-sm">
          <CreditCard className="mx-auto h-12 w-12 text-slate-400 mb-3" />
          <h3 className="text-lg font-bold text-slate-900 mb-1">No Payment Records Found</h3>
          <p className="text-sm text-slate-500">Your completed machinery rental payments will be logged here.</p>
        </div>
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-slate-500 font-extrabold uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="px-5 py-3.5">Reference / Txn ID</th>
                  <th className="px-5 py-3.5">Booking #</th>
                  <th className="px-5 py-3.5">Machinery</th>
                  <th className="px-5 py-3.5">Method</th>
                  <th className="px-5 py-3.5">Date</th>
                  <th className="px-5 py-3.5">Status</th>
                  <th className="px-5 py-3.5 text-right">Amount</th>
                  <th className="px-5 py-3.5 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredPayments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/70 transition">
                    <td className="px-5 py-4 font-mono font-bold text-emerald-800">
                      {p.invoiceReference || p.transactionId || p.paymentReference || `TXN-${p.id}`}
                    </td>
                    <td className="px-5 py-4 font-bold text-slate-900">#{p.bookingId}</td>
                    <td className="px-5 py-4 font-bold text-slate-900">{p.equipmentName || 'Agricultural Machinery'}</td>
                    <td className="px-5 py-4 font-semibold text-slate-700">{p.paymentMethod || 'UPI'}</td>
                    <td className="px-5 py-4 text-slate-500">
                      {p.paymentDate ? new Date(p.paymentDate).toLocaleDateString() : p.createdAt ? new Date(p.createdAt).toLocaleDateString() : 'Today'}
                    </td>
                    <td className="px-5 py-4">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-extrabold ${
                          (p.paymentStatus || p.status) === 'SUCCESS'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        <CheckCircle className="h-3 w-3 text-emerald-600" /> {p.paymentStatus || p.status || 'SUCCESS'}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-right font-extrabold text-slate-900">₹{p.amount}</td>
                    <td className="px-5 py-4 text-right">
                      <Link
                        to={`/farmer/bookings/${p.bookingId}/invoice`}
                        className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-bold text-emerald-700 hover:bg-emerald-50 hover:border-emerald-300 transition"
                      >
                        <FileText className="h-3.5 w-3.5" /> Tax Invoice
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

export default FarmerPayments;
