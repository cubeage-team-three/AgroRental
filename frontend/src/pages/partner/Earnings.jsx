import { useState, useEffect } from 'react';
import {
  Wallet,
  TrendingUp,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { paymentService } from '../../services/paymentService';

function PartnerEarnings() {
  const partnerId = getPartnerId();

  const [summary, setSummary] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEarningsData = async () => {
    setLoading(true);
    setError(null);
    try {
      const summaryData = await paymentService.getPartnerEarnings(partnerId);
      setSummary(summaryData);

      const txData = await paymentService.getPartnerPayments(partnerId);
      setTransactions(txData || []);
    } catch (err) {
      console.error('Failed to load partner financial metrics:', err);
      setError(err.message || 'Failed to retrieve revenue metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEarningsData();
  }, [partnerId]);

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6 font-sans">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium text-sm">Calculating realized revenue & payouts...</p>
        </div>
      </div>
    );
  }

  const totalEarnings = summary?.totalRealizedEarnings || 0;
  const completedCount = summary?.completedTransactionCount || 0;

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Partner Earnings & Revenue Payouts
            </h1>
            <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-md text-[10px] font-black uppercase">
              Financial Hub
            </span>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Track daily machinery rental income, direct bank account settlements, and platform statement invoices.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchEarningsData}
            className="p-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 transition"
            title="Refresh Financials"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => alert(`Payout settlement request submitted for available balance ₹${Number(totalEarnings).toLocaleString('en-IN')}.`)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-bold rounded-xl shadow-md transition-all"
          >
            <Wallet className="w-4 h-4" />
            <span>Request Payout Settlement</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between">
          <span>{error}</span>
        </div>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Total Realized Revenue</span>
          <p className="text-3xl font-black text-[#142E1C]">₹{Number(totalEarnings).toLocaleString('en-IN')}</p>
          <span className="text-xs font-bold text-emerald-600 flex items-center gap-1">
            <TrendingUp className="w-4 h-4" /> Realtime Settlement Ledger
          </span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Successful Payout Orders</span>
          <p className="text-3xl font-black text-[#3E7B27]">{completedCount}</p>
          <span className="text-xs text-gray-500 font-medium">Completed rental transactions</span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Settlement Account</span>
          <p className="text-3xl font-black text-amber-600">Active</p>
          <span className="text-xs text-gray-500 font-medium">Direct Bank Settlement Enabled</span>
        </div>

      </div>

      {/* Transactions Table */}
      <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-gray-100 pb-3">
          <h3 className="text-base font-extrabold text-gray-900">Recent Revenue Transactions</h3>
          <span className="text-xs font-bold text-gray-400">Live Backend Transactions</span>
        </div>

        {transactions.length === 0 ? (
          <div className="p-8 text-center text-xs text-gray-400 font-medium">
            No completed revenue transactions logged for your equipment fleet yet.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-[#F8FAF8] text-[11px] font-black text-gray-400 uppercase tracking-wider">
                <tr>
                  <th className="px-4 py-3">Reference ID</th>
                  <th className="px-4 py-3">Booking ID</th>
                  <th className="px-4 py-3">Machinery</th>
                  <th className="px-4 py-3">Payment Method</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Credit Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 font-medium">
                {transactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-gray-50/80">
                    <td className="px-4 py-3.5 font-mono text-xs font-bold text-gray-800">{tx.paymentReference}</td>
                    <td className="px-4 py-3.5 font-bold text-gray-900">#{tx.bookingId}</td>
                    <td className="px-4 py-3.5 font-bold text-gray-900">{tx.equipmentName}</td>
                    <td className="px-4 py-3.5 text-xs text-gray-500">{tx.paymentMethod}</td>
                    <td className="px-4 py-3.5">
                      <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                        {tx.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-right font-black text-emerald-700">
                      +₹{Number(tx.amount).toLocaleString('en-IN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

    </div>
  );
}

export default PartnerEarnings;
