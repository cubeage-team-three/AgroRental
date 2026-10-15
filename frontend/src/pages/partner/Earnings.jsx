import { useState, useEffect } from 'react';
import {
  Wallet,
  TrendingUp,
  RefreshCw,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  Tractor,
  Users,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { paymentService } from '../../services/paymentService';

function PartnerEarnings() {
  const partnerId = getPartnerId();

  const [detail, setDetail] = useState(null);
  const [bookingReport, setBookingReport] = useState([]);
  const [equipmentReport, setEquipmentReport] = useState([]);
  const [customerReport, setCustomerReport] = useState([]);
  
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [timeFilter, setTimeFilter] = useState('TOTAL');
  const [reportTab, setReportTab] = useState('BOOKING');

  const fetchEarningsData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [detailData, bookingRes, equipmentRes, customerRes] = await Promise.all([
        paymentService.getPartnerEarningsDetail(partnerId),
        paymentService.getBookingRevenueReport(partnerId),
        paymentService.getEquipmentRevenueReport(partnerId),
        paymentService.getCustomerRevenueReport(partnerId)
      ]);
      
      setDetail(detailData);
      setBookingReport(bookingRes || []);
      setEquipmentReport(equipmentRes || []);
      setCustomerReport(customerRes || []);
    } catch (err) {
      console.error('Failed to load partner financial metrics:', err);
      setError(err.response?.data?.message || err.message || 'Failed to retrieve revenue metrics.');
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

  const getFilteredEarnings = () => {
    if (!detail) return 0;
    switch (timeFilter) {
      case 'DAILY': return detail.dailyEarnings;
      case 'WEEKLY': return detail.weeklyEarnings;
      case 'MONTHLY': return detail.monthlyEarnings;
      case 'YEARLY': return detail.yearlyEarnings;
      case 'TOTAL':
      default: return detail.totalRevenue;
    }
  };

  const filteredEarnings = getFilteredEarnings();
  const completedAmount = detail?.completedPaymentsAmount || 0;
  const completedCount = detail?.completedPaymentsCount || 0;
  const pendingAmount = detail?.pendingPaymentsAmount || 0;
  const pendingCount = detail?.pendingPaymentsCount || 0;

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Partner Earnings & Revenue
            </h1>
            <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-md text-[10px] font-black uppercase">
              Financial Hub
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5 font-medium">
            Track daily machinery rental income, payouts, and detailed revenue reports.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchEarningsData}
            className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 transition cursor-pointer"
            title="Refresh Financials"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            type="button"
            onClick={() => alert(`Payout settlement request submitted for available balance ₹${Number(completedAmount).toLocaleString('en-IN')}.`)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs sm:text-sm font-bold rounded-xl shadow-md transition-all cursor-pointer"
          >
            <Wallet className="w-4 h-4" />
            <span>Request Payout Settlement</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
        </div>
      )}

      {/* Time Filter for Revenue Card */}
      <div className="bg-white rounded-3xl p-2 shadow-sm border border-slate-200 flex items-center gap-2 overflow-x-auto text-xs w-max">
        {['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'TOTAL'].map((tf) => (
          <button
            key={tf}
            type="button"
            onClick={() => setTimeFilter(tf)}
            className={`px-4 py-2 rounded-xl font-bold transition-all shrink-0 cursor-pointer ${
              timeFilter === tf
                ? 'bg-emerald-700 text-white shadow-xs'
                : 'bg-transparent text-slate-500 hover:bg-slate-50'
            }`}
          >
            {tf}
          </button>
        ))}
      </div>

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 hover:border-emerald-600 transition-all space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">{timeFilter} Revenue</span>
          <p className="text-3xl font-black text-slate-900">₹{Number(filteredEarnings).toLocaleString('en-IN')}</p>
          <span className="text-xs font-bold text-emerald-700 flex items-center gap-1">
            <TrendingUp className="w-4 h-4" /> {timeFilter.toLowerCase()} revenue trends
          </span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 hover:border-emerald-600 transition-all space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">Completed Payments</span>
          <p className="text-3xl font-black text-emerald-700">₹{Number(completedAmount).toLocaleString('en-IN')}</p>
          <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" /> {completedCount} successful transactions
          </span>
        </div>

        <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 hover:border-emerald-600 transition-all space-y-2">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-400 block">Pending Payments</span>
          <p className="text-3xl font-black text-amber-600">₹{Number(pendingAmount).toLocaleString('en-IN')}</p>
          <span className="text-xs text-slate-500 font-medium flex items-center gap-1">
            <Clock className="w-4 h-4 text-amber-500" /> {pendingCount} awaiting clearance
          </span>
        </div>

      </div>

      {/* Reports Section */}
      <div className="space-y-4">
        {/* Report Filter Tabs */}
        <div className="bg-white rounded-3xl p-3 shadow-sm border border-slate-200 flex items-center gap-2 overflow-x-auto text-xs">
          {[
            { id: 'BOOKING', label: 'Booking Revenue', icon: <FileText className="w-4 h-4" /> },
            { id: 'EQUIPMENT', label: 'Equipment-wise', icon: <Tractor className="w-4 h-4" /> },
            { id: 'CUSTOMER', label: 'Customer-wise', icon: <Users className="w-4 h-4" /> }
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setReportTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold transition-all shrink-0 cursor-pointer ${
                reportTab === tab.id
                  ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                  : 'bg-transparent text-slate-500 hover:bg-slate-50 border border-transparent'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>

        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b border-slate-200 pb-3">
            <h3 className="text-base font-extrabold text-slate-900">
              {reportTab === 'BOOKING' && 'Booking Revenue Report'}
              {reportTab === 'EQUIPMENT' && 'Equipment-wise Revenue Report'}
              {reportTab === 'CUSTOMER' && 'Customer-wise Revenue Report'}
            </h3>
            <span className="text-xs font-bold text-slate-400">Live Backend Data</span>
          </div>

          <div className="overflow-x-auto">
            {reportTab === 'BOOKING' && (
              bookingReport.length === 0 ? (
                <div className="p-8 text-center text-xs text-slate-400 font-medium">No booking revenue records found.</div>
              ) : (
                <table className="w-full text-left text-sm text-slate-700">
                  <thead className="bg-slate-50 text-[11px] font-black text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Booking ID</th>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3">Customer (Farmer)</th>
                      <th className="px-4 py-3">Equipment</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3 text-right">Amount</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium">
                    {bookingReport.map((b, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/80">
                        <td className="px-4 py-3.5 font-bold text-slate-900">#{b.bookingId}</td>
                        <td className="px-4 py-3.5 text-xs">{new Date(b.paymentDate).toLocaleDateString()}</td>
                        <td className="px-4 py-3.5 text-xs">{b.farmerName}</td>
                        <td className="px-4 py-3.5 text-xs text-slate-600">{b.equipmentName}</td>
                        <td className="px-4 py-3.5">
                          <span className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-full ${
                            b.paymentStatus === 'SUCCESS' ? 'bg-emerald-100 text-emerald-800' :
                            b.paymentStatus === 'PENDING' ? 'bg-amber-100 text-amber-800' :
                            'bg-slate-100 text-slate-800'
                          }`}>
                            {b.paymentStatus}
                          </span>
                        </td>
                        <td className="px-4 py-3.5 text-right font-black text-emerald-700">
                          ₹{Number(b.amount).toLocaleString('en-IN')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            )}

            {reportTab === 'EQUIPMENT' && (
              equipmentReport.length === 0 ? (
                <div className="p-8 text-center text-xs text-slate-400 font-medium">No equipment revenue records found.</div>
              ) : (
                <table className="w-full text-left text-sm text-slate-700">
                  <thead className="bg-slate-50 text-[11px] font-black text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Equipment ID</th>
                      <th className="px-4 py-3">Equipment Name</th>
                      <th className="px-4 py-3 text-center">Total Bookings</th>
                      <th className="px-4 py-3 text-right">Total Revenue</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium">
                    {equipmentReport.map((e, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/80">
                        <td className="px-4 py-3.5 font-bold text-slate-900">#{e.equipmentId}</td>
                        <td className="px-4 py-3.5 text-xs text-slate-600">{e.equipmentName}</td>
                        <td className="px-4 py-3.5 text-center">{e.totalBookings}</td>
                        <td className="px-4 py-3.5 text-right font-black text-emerald-700">
                          ₹{Number(e.totalRevenue).toLocaleString('en-IN')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            )}

            {reportTab === 'CUSTOMER' && (
              customerReport.length === 0 ? (
                <div className="p-8 text-center text-xs text-slate-400 font-medium">No customer revenue records found.</div>
              ) : (
                <table className="w-full text-left text-sm text-slate-700">
                  <thead className="bg-slate-50 text-[11px] font-black text-slate-500 uppercase tracking-wider border-b border-slate-200">
                    <tr>
                      <th className="px-4 py-3">Customer ID</th>
                      <th className="px-4 py-3">Customer Name</th>
                      <th className="px-4 py-3 text-center">Total Bookings</th>
                      <th className="px-4 py-3 text-right">Total Revenue</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-medium">
                    {customerReport.map((c, idx) => (
                      <tr key={idx} className="hover:bg-slate-50/80">
                        <td className="px-4 py-3.5 font-bold text-slate-900">#{c.farmerId}</td>
                        <td className="px-4 py-3.5 text-xs text-slate-600">
                          {c.farmerName}
                          {c.mobileNumber && <span className="block text-[10px] text-slate-400">{c.mobileNumber}</span>}
                        </td>
                        <td className="px-4 py-3.5 text-center">{c.totalBookings}</td>
                        <td className="px-4 py-3.5 text-right font-black text-emerald-700">
                          ₹{Number(c.totalRevenue).toLocaleString('en-IN')}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            )}
          </div>
        </div>
      </div>

    </div>
  );
}

export default PartnerEarnings;
