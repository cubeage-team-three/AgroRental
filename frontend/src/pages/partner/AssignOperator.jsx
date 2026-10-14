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
import { getPartnerId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { operatorService } from '../../services/operatorService';

function AssignOperator() {
  const { id } = useParams(); // Booking ID
  const navigate = useNavigate();
  const partnerId = getPartnerId();

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
    const opId = selectedOperator.operatorId || selectedOperator.id;

    setSubmitting(true);
    setError(null);
    try {
      await operatorService.assignOperator(id, {
        operatorId: opId,
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/partner/bookings"
              className="p-2 rounded-xl border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 transition-colors shadow-2xs"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Assign Certified Machine Operator
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500">
            {booking
              ? `Assigning operator for Booking #${booking.id} (${booking.equipmentName || 'Equipment'})`
              : `Select an experienced driver or machinery operator for Booking #${id || ''}.`}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            loadBookingAndAssignment();
            loadEligibleOperators();
          }}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-xl transition-colors shadow-2xs self-start sm:self-auto cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Success Notification */}
      {assignedSuccess && (
        <div className="p-5 bg-emerald-50 border border-emerald-300 rounded-3xl text-emerald-900 space-y-2 shadow-xs">
          <div className="flex items-center gap-2 font-bold text-sm">
            <CheckCircle2 className="w-5 h-5 text-emerald-600" />
            <span>✓ Operator Assigned Successfully!</span>
          </div>
          <p className="text-xs text-emerald-800">
            <strong>{selectedOperator?.fullName || selectedOperator?.name}</strong> has been assigned to Booking #{id}. The operator has been notified and task details are now available in their portal.
          </p>
          <div className="pt-2">
            <Link
              to="/partner/bookings"
              className="inline-flex items-center justify-center px-4 py-2 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl transition-colors shadow-xs"
            >
              Back to Bookings
            </Link>
          </div>
        </div>
      )}

      {/* Current Active Assignment Banner */}
      {currentAssignment && (
        <div className="p-5 bg-slate-50 border border-slate-200 rounded-3xl flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs">
          <div className="flex items-center gap-3">
            <span className="p-2.5 rounded-2xl bg-emerald-700 text-white font-black text-xs">
              <HardHat className="w-5 h-5" />
            </span>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-sm font-extrabold text-slate-900">
                  Assigned Operator: {currentAssignment.operatorName}
                </h4>
                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 border border-emerald-200">
                  {currentAssignment.assignmentStatus}
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-0.5">
                Contact: {currentAssignment.operatorMobile} • Assigned on {new Date(currentAssignment.assignedAt).toLocaleDateString()} by {currentAssignment.assignedBy}
              </p>
            </div>
          </div>
          <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1.5 rounded-xl border border-emerald-200 self-start sm:self-auto">
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
            className="text-xs font-bold text-red-800 underline cursor-pointer"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 shadow-sm border border-slate-200">
        <div className="relative w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search eligible operators by name, skills (e.g. Tractor, Harvester), mobile number, or district..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs sm:text-sm font-semibold text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 focus:bg-white transition-all"
          />
        </div>
      </div>

      {/* Operators Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="bg-white rounded-3xl p-6 space-y-3 animate-pulse border border-slate-200">
              <div className="w-12 h-12 bg-slate-100 rounded-2xl" />
              <div className="h-5 bg-slate-100 rounded w-3/4" />
              <div className="h-4 bg-slate-100 rounded w-1/2" />
              <div className="h-10 bg-slate-100 rounded-xl" />
            </div>
          ))}
        </div>
      ) : operators.length === 0 ? (
        <div className="bg-white border border-dashed border-slate-300 rounded-3xl p-12 text-center space-y-3 shadow-xs">
          <div className="w-14 h-14 bg-amber-50 text-amber-600 rounded-3xl flex items-center justify-center mx-auto text-2xl border border-amber-100">
            <HardHat className="w-7 h-7 text-amber-600" />
          </div>
          <h3 className="text-base font-extrabold text-slate-900">No Eligible Operators Found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto font-medium">
            {searchTerm
              ? 'No approved operators matched your search filter.'
              : 'There are currently no approved and active operators available for assignment.'}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {operators.map((op) => {
            const opId = op.operatorId || op.id;
            const isCurrentlyAssigned = currentAssignment?.operatorId === opId;
            const displayName = op.fullName || op.name;
            const displayMobile = op.mobileNumber || op.phone;
            const displayExperience = op.experience || op.experienceYears || 0;
            const displaySkills = op.skills || op.specialty || 'Machinery Operator';
            const displayAddress = op.address || op.location || 'Address on file';

            return (
              <div
                key={opId}
                className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200 hover:border-emerald-600 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
              >
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-700 flex items-center justify-center font-black text-lg border border-emerald-100">
                      <HardHat className="w-6 h-6 text-emerald-700" />
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-black text-emerald-700 flex items-center gap-1 justify-end">
                        <BadgeCheck className="w-4 h-4 text-emerald-600" /> Verified
                      </span>
                      <span className="text-[10px] text-slate-400 font-semibold block">ID #{opId}</span>
                    </div>
                  </div>

                  <div>
                    <h3 className="text-base font-extrabold text-slate-900">{displayName}</h3>
                    <p className="text-xs font-bold text-emerald-700 mt-0.5 line-clamp-1">
                      {displaySkills}
                    </p>
                  </div>

                  <div className="space-y-1.5 text-xs text-slate-500 font-medium pt-2 border-t border-slate-100">
                    <p className="flex items-center gap-1.5">
                      <Phone className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{displayMobile}</span>
                    </p>
                    <p className="flex items-center gap-1.5">
                      <Briefcase className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{displayExperience} Years Operational Experience</span>
                    </p>
                    <p className="flex items-center gap-1.5 truncate">
                      <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{displayAddress}</span>
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  disabled={isCurrentlyAssigned || submitting}
                  onClick={() => handleSelectToAssign(op)}
                  className={`w-full py-2.5 text-xs font-bold rounded-xl transition-all shadow-xs cursor-pointer ${isCurrentlyAssigned
                      ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                      : 'bg-emerald-700 hover:bg-emerald-800 text-white active:scale-98'
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
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3">
              <span className="p-3 bg-emerald-50 text-emerald-700 rounded-2xl border border-emerald-100">
                <HardHat className="w-6 h-6 text-emerald-700" />
              </span>
              <div>
                <h3 className="text-lg font-black text-slate-900">Confirm Assignment</h3>
                <p className="text-xs text-slate-500">Booking #{id}</p>
              </div>
            </div>

            <div className="p-4 bg-slate-50 border border-slate-200 rounded-2xl text-xs space-y-1 text-slate-700">
              <p>
                Assign <strong>{selectedOperator.fullName || selectedOperator.name}</strong> ({selectedOperator.mobileNumber || selectedOperator.phone}) to operate machinery for this booking?
              </p>
              <p className="text-[11px] text-slate-500">
                Experience: {selectedOperator.experience || selectedOperator.experienceYears || 0} years • Skills: {selectedOperator.skills || selectedOperator.specialty || 'All Machinery'}
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">
                Assignment Notes (Optional)
              </label>
              <textarea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add special instructions, field coordinates, or task specifics..."
                className="w-full p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 placeholder-slate-400 focus:ring-2 focus:ring-emerald-500 focus:bg-white focus:outline-none"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={submitting}
                onClick={() => setShowConfirmModal(false)}
                className="px-4 py-2 text-xs font-bold text-slate-600 hover:text-slate-800 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={submitting}
                onClick={handleConfirmAssignment}
                className="px-5 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs disabled:opacity-50 transition-all cursor-pointer"
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
