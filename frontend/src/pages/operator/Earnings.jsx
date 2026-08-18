import { useState } from "react";
import { Link } from "react-router-dom";

function OperatorEarnings() {
  const [activePeriod, setActivePeriod] = useState("ALL");

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">💰</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Operator Financials
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Earnings & Settlements
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Track your completed work payouts, pending releases, and daily field operation earnings.
          </p>
        </div>

        <Link
          to="/operator/dashboard"
          className="bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs px-4 py-2.5 rounded-xl transition"
        >
          ← Dashboard
        </Link>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-emerald-900 to-green-950 text-white p-6 rounded-3xl shadow-md space-y-2">
          <span className="text-[11px] font-bold text-emerald-200 uppercase tracking-wider block">
            Total Lifetime Earnings
          </span>
          <p className="text-3xl font-black text-lime-300">₹0.00</p>
          <p className="text-[10px] text-emerald-200/80">From all verified deployments</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
          <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider block">
            This Month
          </span>
          <p className="text-3xl font-black text-emerald-950">₹0.00</p>
          <p className="text-[10px] text-gray-400">Current cycle balance</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
          <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider block">
            Pending Settlements
          </span>
          <p className="text-3xl font-black text-amber-700">₹0.00</p>
          <p className="text-[10px] text-gray-400">Under partner verification</p>
        </div>

        <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
          <span className="text-[11px] font-bold text-blue-700 uppercase tracking-wider block">
            Completed Orders
          </span>
          <p className="text-3xl font-black text-blue-800">0</p>
          <p className="text-[10px] text-gray-400">Full shifts finished</p>
        </div>
      </div>

      {/* Earnings History Table Card */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-amber-100/60 pb-4">
          <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
            Settlement Transaction Records
          </h2>
          <div className="flex gap-1.5 bg-gray-100 p-1 rounded-xl text-xs">
            {["ALL", "PAID", "PENDING"].map((p) => (
              <button
                key={p}
                onClick={() => setActivePeriod(p)}
                className={`px-3 py-1 rounded-lg font-bold transition ${
                  activePeriod === p ? "bg-white text-emerald-900 shadow-xs" : "text-gray-500 hover:text-gray-800"
                }`}
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        {/* Empty State */}
        <div className="p-12 text-center space-y-3">
          <div className="w-14 h-14 bg-emerald-50 rounded-2xl flex items-center justify-center mx-auto text-2xl text-emerald-800 shadow-inner">
            💸
          </div>
          <h3 className="text-base font-bold text-emerald-950">
            No Earnings Records Yet
          </h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto">
            Once you complete your assigned farm machinery deployments, payout summaries and bank settlement receipts will appear here.
          </p>
        </div>
      </div>
    </div>
  );
}

export default OperatorEarnings;
