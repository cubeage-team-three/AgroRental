import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  HardHat,
  Star,
  Phone,
  MapPin,
  CheckCircle2,
  ArrowLeft,
  Search,
  BadgeCheck,
} from 'lucide-react';

const MOCK_OPERATORS = [
  {
    id: 'OP-101',
    name: 'Santosh Gaikwad',
    phone: '+91 98224 88712',
    experience: '8 Years Exp.',
    specialty: 'Tractor 4WD & Rotavator Specialist',
    rating: 4.9,
    jobsCompleted: 142,
    location: 'Haveli, Pune (5 km away)',
    dailyRate: '₹600/day',
    verified: true,
  },
  {
    id: 'OP-102',
    name: 'Balasaheb Kadam',
    phone: '+91 97654 33219',
    experience: '12 Years Exp.',
    specialty: 'Combined Harvester & Seeder Master',
    rating: 4.8,
    jobsCompleted: 210,
    location: 'Shirur, Pune (12 km away)',
    dailyRate: '₹800/day',
    verified: true,
  },
  {
    id: 'OP-103',
    name: 'Anil Jadhav',
    phone: '+91 99231 44556',
    experience: '5 Years Exp.',
    specialty: 'Tractor Ploughing & Laser Leveling',
    rating: 4.7,
    jobsCompleted: 98,
    location: 'Khed, Pune (8 km away)',
    dailyRate: '₹550/day',
    verified: true,
  },
];

function AssignOperator() {
  const navigate = useNavigate();
  const [selectedOperator, setSelectedOperator] = useState(null);
  const [assigned, setAssigned] = useState(false);

  const handleAssign = (op) => {
    setSelectedOperator(op);
    setAssigned(true);
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/partner/bookings"
              className="p-1.5 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Assign Certified Machine Operator
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            Select an experienced driver or machinery operator for Booking #BK-2026-0891.
          </p>
        </div>
      </div>

      {assigned && selectedOperator && (
        <div className="p-5 bg-emerald-50 border border-emerald-300 rounded-3xl text-emerald-900 space-y-2 shadow-xs">
          <div className="flex items-center gap-2 font-bold text-sm">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>✓ Operator Assigned Successfully!</span>
          </div>
          <p className="text-xs text-emerald-800">
            <strong>{selectedOperator.name}</strong> has been notified and scheduled for the upcoming booking dates.
          </p>
        </div>
      )}

      {/* Operators List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {MOCK_OPERATORS.map((op) => (
          <div
            key={op.id}
            className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center font-black text-lg">
                  <HardHat className="w-6 h-6" />
                </div>
                <div className="text-right">
                  <span className="text-xs font-black text-amber-500 flex items-center gap-1 justify-end">
                    ★ {op.rating}
                  </span>
                  <span className="text-[10px] text-gray-400 font-semibold block">{op.jobsCompleted} Jobs Done</span>
                </div>
              </div>

              <div>
                <div className="flex items-center gap-1.5">
                  <h3 className="text-base font-extrabold text-gray-900">{op.name}</h3>
                  {op.verified && <BadgeCheck className="w-4 h-4 text-emerald-600" />}
                </div>
                <p className="text-xs font-bold text-[#3E7B27] mt-0.5">{op.specialty}</p>
              </div>

              <div className="space-y-1 text-xs text-gray-500 font-medium pt-2 border-t border-gray-100">
                <p className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-gray-400" />
                  <span>{op.phone}</span>
                </p>
                <p className="flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-gray-400" />
                  <span>{op.location}</span>
                </p>
              </div>

              <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                <span className="text-xs text-gray-400 font-bold uppercase">Rate</span>
                <span className="text-sm font-black text-[#142E1C]">{op.dailyRate}</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => handleAssign(op)}
              className="w-full py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl transition-colors shadow-xs"
            >
              Assign This Operator
            </button>
          </div>
        ))}
      </div>

    </div>
  );
}

export default AssignOperator;
