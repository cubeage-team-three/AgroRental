import { useState, useEffect } from 'react';
import { CreditCard, Calendar, CheckCircle2, AlertCircle, RefreshCw, FileText } from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { paymentService } from '../../services/paymentService';

function FarmerPayments() {
  const farmerId = getFarmerId();

  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchPayments = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await paymentService.getFarmerPayments(farmerId);
      setPayments(data || []);
    } catch (err) {
      console.error('Failed to load farmer payment history:', err);
      setError(err.message || 'Failed to retrieve transaction records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, [farmerId]);

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
    <div className="max-w-6xl mx-auto space-y-6 font-sans">
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Payments & Invoices
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            View transaction receipts, rental payment statuses, and billing invoices.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchPayments}
          className="p-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 transition"
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

      {payments.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-3">
          <CreditCard className="w-12 h-12 text-gray-300 mx-auto" />
          <h3 className="text-lg font-black text-gray-900">No Payment Records</h3>
          <p className="text-xs text-gray-500">Your completed machinery rental payments will be logged here.</p>
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4">
          <h3 className="text-sm font-black uppercase tracking-wider text-gray-400 border-b border-gray-100 pb-3">
            Transaction History ({payments.length})
          </h3>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-[#F8FAF8] text-[11px] font-black text-gray-400 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Reference ID</th>
                  <th className="px-4 py-3">Booking ID</th>
                  <th className="px-4 py-3">Machinery</th>
                  <th className="px-4 py-3">Method</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 font-medium">
                {payments.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50/80">
                    <td className="px-4 py-3.5 font-mono text-xs font-bold text-gray-900">{tx.paymentReference}</td>
                    <td className="px-4 py-3.5 font-bold text-gray-700">#{tx.bookingId}</td>
                    <td className="px-4 py-3.5 font-bold text-gray-900">{tx.equipmentName}</td>
                    <td className="px-4 py-3.5 text-xs text-gray-500">{tx.paymentMethod}</td>
                    <td className="px-4 py-3.5">
                      <span
                        className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                          tx.paymentStatus === 'SUCCESS'
                            ? 'bg-emerald-100 text-emerald-800'
                            : tx.paymentStatus === 'FAILED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {tx.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-black text-emerald-800">
                      ₹{tx.amount}
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
