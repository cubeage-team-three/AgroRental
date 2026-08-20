import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {
  HardHat,
  Star,
  Phone,
  MapPin,
  CheckCircle2,
  ArrowLeft,
  Search,
  BadgeCheck,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { operatorService } from '../../services/operatorService';
import { getPartnerId } from '../../services/authService';

function AssignOperator() {
  const { id } = useParams();
  const navigate = useNavigate();
  const partnerId = getPartnerId();
  
  const [booking, setBooking] = useState(null);
  const [operators, setOperators] = useState([]);
  const [selectedOperator, setSelectedOperator] = useState(null);
  const [assigned, setAssigned] = useState(false);
  const [loading, setLoading] = useState(true);
  const [assigningLoading, setAssigningLoading] = useState(false);
  const [error, setError] = useState(null);
  const [assignError, setAssignError] = useState(null);

  const fetchDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const b = await bookingService.getBookingById(id);
      if (b) {
        setBooking(b);
        // Fetch available operators based on booking dates
        const ops = await operatorService.getAvailableOperators({
          partnerId,
          startDate: b.startDate,
          endDate: b.endDate,
        });
        setOperators(ops || []);
        
        if (b.operatorId) {
          const pre = ops?.find((o) => o.id === b.operatorId);
          if (pre) setSelectedOperator(pre);
        }
      }
    } catch (e) {
      console.error('Failed to load booking/operators:', e);
      setError(e.response?.data?.message || e.message || 'Could not fetch data');
    } finally {
      setLoading(false);
    }
  }, [id, partnerId]);

  useEffect(() => {
    fetchDetails();
  }, [fetchDetails]);

  const handleAssign = async (op) => {
    if (!window.confirm(`Are you sure you want to assign ${op.name} to this booking?`)) return;
    
    setAssigningLoading(true);
    setAssignError(null);
    try {
      if (id) {
        const updated = await bookingService.assignOperator(id, op.id);
        setBooking(updated);
        setSelectedOperator(op);
        setAssigned(true);
      }
    } catch (e) {
      console.error('Failed to assign operator:', e);
      setAssignError(e.response?.data?.message || e.message || 'Failed to assign operator.');
    } finally {
      setAssigningLoading(false);
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/partner/bookings"
              className="p-1.5 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Assign Certified Machine Operator
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            {booking
              ? `Assigning operator for Booking #${booking.id} (${booking.equipmentName || 'Equipment'})`
              : `Select an experienced driver or machinery operator for Booking #${id || ''}.`}
          </p>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchDetails} className="text-xs font-bold text-red-800 underline">
            Retry
          </button>
        </div>
      )}

      {assignError && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center shadow-xs">
          <AlertCircle className="w-5 h-5 text-red-500 shrink-0 mr-2" />
          <span>{assignError}</span>
        </div>
      )}

      {assigned && selectedOperator && (
        <div className="p-5 bg-emerald-50 border border-emerald-300 rounded-3xl text-emerald-900 space-y-2 shadow-xs">
          <div className="flex items-center gap-2 font-bold text-sm">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>✓ Operator Assigned Successfully!</span>
          </div>
          <p className="text-xs text-emerald-800">
            <strong>{selectedOperator.name}</strong> has been notified and scheduled for the upcoming booking dates.
          </p>
          <div className="pt-2">
             <Link
              to="/partner/bookings"
              className="inline-flex items-center justify-center px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-colors shadow-xs"
             >
               Back to Bookings
             </Link>
          </div>
        </div>
      )}

      {/* Operators List */}
      {loading ? (
        <div className="flex justify-center p-12">
          <RefreshCw className="w-8 h-8 text-emerald-600 animate-spin" />
        </div>
      ) : operators.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-4">
          <HardHat className="w-12 h-12 text-gray-300 mx-auto" />
          <div className="space-y-1 max-w-md mx-auto">
            <h3 className="text-lg font-black text-gray-900">No Available Operators</h3>
            <p className="text-xs text-gray-500">
              No certified operators are available for the selected booking dates. Please adjust dates or hire new operators.
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {operators.map((op) => (
            <div
              key={op.id}
              className={`bg-white rounded-3xl p-6 shadow-sm border transition-all flex flex-col justify-between space-y-4 ${
                selectedOperator?.id === op.id ? 'border-[#3E7B27] ring-2 ring-[#3E7B27] ring-opacity-20' : 'border-gray-100 hover:shadow-md'
              }`}
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center font-black text-lg">
                    <HardHat className="w-6 h-6" />
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-black text-amber-500 flex items-center gap-1 justify-end">
                      ★ {op.rating || '4.5'}
                    </span>
                    <span className="text-[10px] text-gray-400 font-semibold block">{op.jobsCompleted || 0} Jobs Done</span>
                  </div>
                </div>

                <div>
                  <div className="flex items-center gap-1.5">
                    <h3 className="text-base font-extrabold text-gray-900">{op.name}</h3>
                    {op.verified !== false && <BadgeCheck className="w-4 h-4 text-emerald-600" />}
                  </div>
                  <p className="text-xs font-bold text-[#3E7B27] mt-0.5">{op.specialty || `${op.experienceYears || 2} Years Exp.`}</p>
                </div>

                <div className="space-y-1 text-xs text-gray-500 font-medium pt-2 border-t border-gray-100">
                  <p className="flex items-center gap-1.5">
                    <Phone className="w-3.5 h-3.5 text-gray-400" />
                    <span>{op.phone}</span>
                  </p>
                  {op.location && (
                    <p className="flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-gray-400" />
                      <span>{op.location}</span>
                    </p>
                  )}
                </div>

                <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                  <span className="text-xs text-gray-400 font-bold uppercase">Rate</span>
                  <span className="text-sm font-black text-[#142E1C]">₹{op.dailyRate || 500}/day</span>
                </div>
              </div>

              <button
                type="button"
                disabled={assigningLoading || selectedOperator?.id === op.id || assigned}
                onClick={() => handleAssign(op)}
                className={`w-full py-2.5 text-xs font-bold rounded-xl transition-colors shadow-xs ${
                  selectedOperator?.id === op.id
                    ? 'bg-emerald-100 text-emerald-800 cursor-default'
                    : 'bg-[#3E7B27] hover:bg-[#2E6F22] text-white disabled:opacity-50'
                }`}
              >
                {selectedOperator?.id === op.id ? 'Assigned' : assigningLoading ? 'Assigning...' : 'Assign This Operator'}
              </button>
            </div>
          ))}
        </div>
      )}

    </div>
  );
}

export default AssignOperator;
