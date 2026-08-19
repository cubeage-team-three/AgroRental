import { useState } from 'react';
import {
  CalendarCheck,
  User,
  MapPin,
  Calendar,
  Clock,
  CheckCircle,
  XCircle,
  HardHat,
  Filter,
  Sparkles,
} from 'lucide-react';

const MOCK_REQUESTS = [
  {
    id: 'BK-2026-0891',
    farmerName: 'Ramesh Yadav',
    farmerPhone: '+91 98765 43210',
    equipmentName: 'Mahindra 575 DI 45HP Tractor',
    dates: '20 Aug 2026 – 22 Aug 2026 (3 Days)',
    location: 'Village Khed, Taluka Haveli, Pune',
    amount: '₹7,500',
    status: 'PENDING',
    crop: 'Sugarcane Ploughing',
  },
  {
    id: 'BK-2026-0885',
    farmerName: 'Suresh Patil',
    farmerPhone: '+91 98221 55678',
    equipmentName: 'John Deere Rotavator 6ft',
    dates: '24 Aug 2026 (1 Day)',
    location: 'Manchar, Ambegaon, Pune',
    amount: '₹2,200',
    status: 'ACCEPTED',
    crop: 'Soil Preparation',
  },
  {
    id: 'BK-2026-0870',
    farmerName: 'Dnyaneshwar Shinde',
    farmerPhone: '+91 94220 99881',
    equipmentName: 'Preet 987 Combined Harvester',
    dates: '15 Aug 2026 – 16 Aug 2026 (2 Days)',
    location: 'Shirur, Pune',
    amount: '₹18,000',
    status: 'COMPLETED',
    crop: 'Wheat Harvesting',
  },
];

function BookingRequests() {
  const [filter, setFilter] = useState('ALL');
  const [requests, setRequests] = useState(MOCK_REQUESTS);

  const handleAction = (id, action) => {
    setRequests((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status: action === 'accept' ? 'ACCEPTED' : 'REJECTED' } : r))
    );
  };

  const filtered = requests.filter((r) => (filter === 'ALL' ? true : r.status === filter));

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Booking Requests
            </h1>
            <span className="px-2.5 py-0.5 bg-lime-400 text-emerald-950 rounded-md text-[10px] font-black uppercase">
              Module Preview
            </span>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Review incoming machinery rental orders from regional farmers and assign certified operators.
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="bg-white rounded-3xl p-4 shadow-sm border border-gray-100 flex items-center gap-2 overflow-x-auto text-xs">
        {['ALL', 'PENDING', 'ACCEPTED', 'COMPLETED', 'REJECTED'].map((st) => (
          <button
            key={st}
            type="button"
            onClick={() => setFilter(st)}
            className={`px-4 py-2 rounded-xl font-bold transition-all ${
              filter === st
                ? 'bg-[#3E7B27] text-white shadow-xs'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {st} ({st === 'ALL' ? requests.length : requests.filter((r) => r.status === st).length})
          </button>
        ))}
      </div>

      {/* Booking Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filtered.map((b) => (
          <div
            key={b.id}
            className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-mono font-bold text-gray-400">ID: {b.id}</span>
                <span
                  className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                    b.status === 'PENDING'
                      ? 'bg-purple-100 text-purple-800'
                      : b.status === 'ACCEPTED'
                      ? 'bg-blue-100 text-blue-800'
                      : b.status === 'COMPLETED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-red-100 text-red-800'
                  }`}
                >
                  {b.status}
                </span>
              </div>

              <div>
                <h3 className="text-base font-extrabold text-gray-900">{b.equipmentName}</h3>
                <p className="text-xs text-[#3E7B27] font-bold mt-0.5">🌾 Crop: {b.crop}</p>
              </div>

              <div className="space-y-1.5 text-xs text-gray-600 pt-2 border-t border-gray-100 font-medium">
                <p className="flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-gray-400" />
                  <span>{b.farmerName} ({b.farmerPhone})</span>
                </p>
                <p className="flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-gray-400" />
                  <span>{b.dates}</span>
                </p>
                <p className="flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-gray-400" />
                  <span>{b.location}</span>
                </p>
              </div>

              <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                <span className="text-xs text-gray-400 font-bold uppercase">Total Payout</span>
                <span className="text-lg font-black text-[#142E1C]">{b.amount}</span>
              </div>
            </div>

            {/* Action Buttons */}
            {b.status === 'PENDING' ? (
              <div className="flex items-center gap-2 pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => handleAction(b.id, 'accept')}
                  className="flex-1 py-2 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl transition-colors shadow-xs"
                >
                  Accept Order
                </button>
                <button
                  type="button"
                  onClick={() => handleAction(b.id, 'reject')}
                  className="py-2 px-3 bg-gray-100 hover:bg-red-50 text-gray-600 hover:text-red-600 text-xs font-bold rounded-xl transition-colors"
                >
                  Decline
                </button>
              </div>
            ) : b.status === 'ACCEPTED' ? (
              <div className="pt-2 border-t border-gray-100">
                <a
                  href="/partner/bookings/1/assign-operator"
                  className="w-full flex items-center justify-center gap-1.5 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl transition-colors shadow-xs"
                >
                  <HardHat className="w-3.5 h-3.5" />
                  <span>Assign Driver / Operator</span>
                </a>
              </div>
            ) : (
              <div className="text-[11px] text-gray-400 text-center font-semibold pt-1 border-t border-gray-100">
                Order status finalized
              </div>
            )}

          </div>
        ))}
      </div>

    </div>
  );
}

export default BookingRequests;
