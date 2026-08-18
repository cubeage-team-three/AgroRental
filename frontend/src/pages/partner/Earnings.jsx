import { useState } from 'react';
import {
  Wallet,
  TrendingUp,
  ArrowDownLeft,
  ArrowUpRight,
  Download,
  Calendar,
  DollarSign,
  CheckCircle2,
  Clock,
} from 'lucide-react';

const MOCK_TRANSACTIONS = [
  {
    id: 'TXN-9021',
    description: 'Mahindra 575 DI Rental (3 Days)',
    farmer: 'Ramesh Yadav',
    date: '18 Aug 2026',
    amount: '₹7,500',
    status: 'COMPLETED',
    type: 'CREDIT',
  },
  {
    id: 'TXN-8840',
    description: 'Bank Payout to HDFC A/C •••• 4091',
    farmer: 'Withdrawal',
    date: '12 Aug 2026',
    amount: '₹25,000',
    status: 'TRANSFERRED',
    type: 'DEBIT',
  },
  {
    id: 'TXN-8711',
    description: 'John Deere Rotavator Rental (1 Day)',
    farmer: 'Suresh Patil',
    date: '10 Aug 2026',
    amount: '₹2,200',
    status: 'COMPLETED',
    type: 'CREDIT',
  },
  {
    id: 'TXN-8540',
    description: 'Combined Harvester Job (2 Days)',
    farmer: 'Dnyaneshwar Shinde',
    date: '04 Aug 2026',
    amount: '₹18,000',
    status: 'COMPLETED',
    type: 'CREDIT',
  },
];

function PartnerEarnings() {
  const [transactions] = useState(MOCK_TRANSACTIONS);

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

        <button
          type="button"
          onClick={() => alert('Payout request initiated for available balance.')}
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-bold rounded-xl shadow-md transition-all"
        >
          <Wallet className="w-4 h-4" />
          <span>Request Payout Settlement</span>
        </button>
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Total Lifetime Earnings</span>
          <p className="text-3xl font-black text-[#142E1C]">₹1,24,500</p>
          <span className="text-xs font-bold text-emerald-600 flex items-center gap-1">
            <TrendingUp className="w-4 h-4" /> +24% vs last month
          </span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Available Balance</span>
          <p className="text-3xl font-black text-[#3E7B27]">₹27,700</p>
          <span className="text-xs text-gray-500 font-medium">Ready for instant bank transfer</span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block">Pending Clearance</span>
          <p className="text-3xl font-black text-amber-600">₹7,500</p>
          <span className="text-xs text-gray-500 font-medium">1 in-progress rental order</span>
        </div>

      </div>

      {/* Transactions Table */}
      <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4">
        <div className="flex items-center justify-between border-b border-gray-100 pb-3">
          <h3 className="text-base font-extrabold text-gray-900">Recent Revenue Transactions</h3>
          <span className="text-xs font-bold text-gray-400">Showing last 30 days</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-700">
            <thead className="bg-[#F8FAF8] text-[11px] font-black text-gray-400 uppercase tracking-wider">
              <tr>
                <th className="px-4 py-3">Transaction ID</th>
                <th className="px-4 py-3">Description</th>
                <th className="px-4 py-3">Party / Farmer</th>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {transactions.map((tx) => (
                <tr key={tx.id} className="hover:bg-gray-50/80">
                  <td className="px-4 py-3.5 font-mono text-xs font-bold text-gray-800">{tx.id}</td>
                  <td className="px-4 py-3.5 font-bold text-gray-900">{tx.description}</td>
                  <td className="px-4 py-3.5 text-xs text-gray-600">{tx.farmer}</td>
                  <td className="px-4 py-3.5 text-xs text-gray-500">{tx.date}</td>
                  <td className="px-4 py-3.5">
                    <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                      {tx.status}
                    </span>
                  </td>
                  <td className={`px-4 py-3.5 text-right font-black ${tx.type === 'CREDIT' ? 'text-emerald-700' : 'text-gray-900'}`}>
                    {tx.type === 'CREDIT' ? `+${tx.amount}` : `-${tx.amount}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

    </div>
  );
}

export default PartnerEarnings;
