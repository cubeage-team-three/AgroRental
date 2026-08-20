import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import {
  HardHat,
  Star,
  Phone,
  MapPin,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Search,
  BadgeCheck,
  Briefcase,
  Tractor,
  RefreshCw,
} from 'lucide-react';
import { bookingService } from '../../services/bookingService';
import { operatorService } from '../../services/operatorService';

function AssignOperator() {
  const { id } = useParams(); // Booking ID
  const navigate = useNavigate();

  const [booking, setBooking] = useState(null);
  const [currentAssignment, setCurrentAssignment] = useState(null);
  const [operators, setOperators] = useState([]);
  const [selectedOperator, setSelectedOperator] = useState(null);
  const [notes, setNotes] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [assignedSuccess, setAssignedSuccess] = useState(false);
  const [error, setError] = useState(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  // 1. Fetch booking details and active assignment
  const loadBookingAndAssignment = useCallback(async () => {
    if (!id) return;
    try {
      const b = await bookingService.getBookingById(id);
      setBooking(b);

      try {
        const activeAssign = await operatorService.getBookingAssignment(id);
        if (activeAssign) {
          setCurrentAssignment(activeAssign);
        }
      } catch (err) {
        console.log('No active operator assignment found for booking:', id);
      }
    } catch (e) {
      console.warn('Could not fetch booking:', e);
      setError(e.message || 'Failed to load booking details');
    }
  }, [id]);

  // 2. Fetch eligible approved operators
  const loadEligibleOperators = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await operatorService.getEligibleOperators({
        search: searchTerm,
        page: 0,
        size: 12,
      });

      if (response && response.content) {
        setOperators(response.content);
      } else if (Array.isArray(response)) {
        setOperators(response);
      } else {
        setOperators([]);
      }
    } catch (err) {
      console.error('Failed to load eligible operators:', err);
      setError(err.message || 'Failed to search eligible operators');
    } finally {
      setLoading(false);
    }
  }, [searchTerm]);

  useEffect(() => {
    loadBookingAndAssignment();
  }, [loadBookingAndAssignment]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadEligibleOperators();
    }, 300);
    return () => clearTimeout(timer);
  }, [loadEligibleOperators]);

  const handleSelectToAssign = (op) => {
    setSelectedOperator(op);
    setShowConfirmModal(true);
  };

  const handleConfirmAssignment = async () => {
    if (!selectedOperator || !id) return;

    setSubmitting(true);
    setError(null);
    try {
      await operatorService.assignOperator(id, {
        operatorId: selectedOperator.operatorId,
        notes: notes ? notes.trim() : `Assigned for ${booking?.equipmentName || 'machinery service'}`,
      });

      setAssignedSuccess(true);
      setShowConfirmModal(false);
      await loadBookingAndAssignment();
    } catch (err) {
      console.error('Operator assignment failed:', err);
      setError(err.message || 'Failed to assign operator to booking');
      setShowConfirmModal(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/partner/bookings"
              className="p-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors shadow-2xs"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Assign Certified Machine Operator
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            {booking
              ? `Assign a verified operator for Booking #${booking.id} (${booking.equipmentName || 'Equipment'})`
              : `Select a certified operator for Booking #${id || 'BK-2026'}.`}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            loadBookingAndAssignment();
            loadEligibleOperators();
          }}
          className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors shadow-2xs self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Success Notification */}
      {assignedSuccess && (
        <div className="p-5 bg-emerald-50 border border-emerald-300 rounded-3xl text-emerald-900 space-y-1 shadow-xs">
          <div className="flex items-center gap-2 font-bold text-sm">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>✓ Operator Assigned Successfully!</span>
          </div>
          <p className="text-xs text-emerald-800">
            <strong>{selectedOperator?.fullName}</strong> has been assigned to Booking #{id}. The operator has been notified and task details are now available in their portal.
          </p>
        </div>
      )}

      {/* Current Active Assignment Banner */}
      {currentAssignment && (
        <div className="p-5 bg-[#F0EFE9] border border-gray-200 rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-3">
            <span className="p-2.5 rounded-2xl bg-emerald-600 text-white font-black text-xs">
              <HardHat className="w-5 h-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-extrabold text-gray-900">
                  Assigned Operator: {currentAssignment.operatorName}
                </h4>
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 border border-emerald-200">
                  {currentAssignment.assignmentStatus}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-0.5">
                Contact: {currentAssignment.operatorMobile} • Assigned on {new Date(currentAssignment.assignedAt).toLocaleDateString()} by {currentAssignment.assignedBy}
              </p>
            </div>
          </div>
          <span className="text-xs font-bold text-[#3E7B27] bg-emerald-50 px-3 py-1.5 rounded-xl border border-emerald-200 self-start sm:self-auto">
            Active Assignment
          </span>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="text-xs font-bold text-red-800 underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 shadow-sm border border-gray-100">
        <div className="relative w-full">
          <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search eligible operators by name, skills (e.g. Tractor, Harvester), mobile number, or district..."
            className="w-full pl-10 pr-4 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
          />
        </div>
      </div>

      {/* Operators Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="bg-white rounded-3xl p-6 space-y-3 animate-pulse border border-gray-100">
              <div className="w-12 h-12 bg-gray-200 rounded-2xl" />
              <div className="h-5 bg-gray-200 rounded w-3/4" />
              <div className="h-4 bg-gray-200 rounded w-1/2" />
              <div className="h-10 bg-gray-200 rounded-xl" />
            </div>
          ))}
        </div>
      ) : operators.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-3 shadow-xs">
          <div className="w-14 h-14 bg-amber-50 text-amber-600 rounded-3xl flex items-center justify-center mx-auto text-2xl">
            <HardHat className="w-7 h-7 text-amber-600" />
          </div>
          <h3 className="text-base font-extrabold text-gray-900">No Eligible Operators Found</h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto">
            {searchTerm
              ? 'No approved operators matched your search filter.'
              : 'There are currently no approved and active operators available for assignment.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {operators.map((op) => {
            const isCurrentlyAssigned = currentAssignment?.operatorId === op.operatorId;

            return (
              <div
                key={op.operatorId}
                className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-black text-lg">
                      <HardHat className="w-6 h-6 text-[#3E7B27]" />
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-black text-emerald-600 flex items-center gap-1 justify-end">
                        <BadgeCheck className="w-4 h-4 text-emerald-600" /> Verified
                      </span>
                      <span className="text-[10px] text-gray-400 font-semibold block">ID #{op.operatorId}</span>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-base font-extrabold text-gray-900">{op.fullName}</h3>
                    <p className="text-xs font-bold text-[#3E7B27] mt-0.5 line-clamp-1">
                      {op.skills || 'Machinery Operator'}
                    </p>
                  </div>

                  <div className="space-y-1.5 text-xs text-gray-500 font-medium pt-2 border-t border-gray-100">
                    <p className="flex items-center gap-1.5">
                      <Phone className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{op.mobileNumber}</span>
                    </p>
                    <p className="flex items-center gap-1.5">
                      <Briefcase className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{op.experience || 0} Years Operational Experience</span>
                    </p>
                    <p className="flex items-center gap-1.5 truncate">
                      <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{op.address || 'Address on file'}</span>
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  disabled={isCurrentlyAssigned || submitting}
                  onClick={() => handleSelectToAssign(op)}
                  className={`w-full py-2.5 text-xs font-bold rounded-xl transition-all shadow-xs ${
                    isCurrentlyAssigned
                      ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                      : 'bg-[#3E7B27] hover:bg-[#2E6F22] text-white active:scale-98'
                  }`}
                >
                  {isCurrentlyAssigned ? 'Already Assigned' : 'Assign Operator'}
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal */}
      {showConfirmModal && selectedOperator && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3">
              <span className="p-3 bg-emerald-50 text-emerald-700 rounded-2xl">
                <HardHat className="w-6 h-6 text-[#3E7B27]" />
              </span>
              <div>
                <h3 className="text-lg font-black text-gray-900">Confirm Assignment</h3>
                <p className="text-xs text-gray-500">Booking #{id}</p>
              </div>
            </div>

            <div className="p-4 bg-[#F0EFE9] rounded-2xl text-xs space-y-1 text-gray-700">
              <p>
                Assign <strong>{selectedOperator.fullName}</strong> ({selectedOperator.mobileNumber}) to operate machinery for this booking?
              </p>
              <p className="text-[11px] text-gray-500">
                Experience: {selectedOperator.experience || 0} years • Skills: {selectedOperator.skills || 'All Machinery'}
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-gray-700">
                Assignment Notes (Optional)
              </label>
              <textarea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add special instructions, field coordinates, or task specifics..."
                className="w-full p-3 bg-[#F0EFE9] border border-transparent rounded-xl text-xs focus:ring-2 focus:ring-[#3E7B27] focus:bg-white focus:outline-none"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={submitting}
                onClick={() => setShowConfirmModal(false)}
                className="px-4 py-2 text-xs font-bold text-gray-600 hover:text-gray-800 rounded-xl hover:bg-gray-100 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={handleConfirmAssignment}
                className="px-5 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl shadow-xs disabled:opacity-50 transition-all"
              >
                {submitting ? 'Assigning...' : 'Confirm Assignment'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

export default AssignOperator;
