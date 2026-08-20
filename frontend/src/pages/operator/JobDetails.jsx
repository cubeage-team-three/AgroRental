import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  MapPin,
  Tractor,
  AlertCircle,
  ArrowLeft,
  DollarSign,
  User,
  ShieldCheck,
  RefreshCw,
  Clock,
  FileText,
  CheckCircle2,
  XCircle,
  Navigation,
  Play,
  Pause,
  Flag,
  HelpCircle,
  Info,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
} from '../../utils/constants';

function JobDetails() {
  const { id } = useParams(); // Assignment ID

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Modal Dialog States
  const [modalType, setModalType] = useState(null); // 'ACCEPT', 'REJECT', 'TRAVEL', 'REACHED', 'START_WORK', 'PAUSE', 'RESUME', 'COMPLETE'
  const [rejectionReason, setRejectionReason] = useState('');
  const [pauseReason, setPauseReason] = useState('');
  const [completionNotes, setCompletionNotes] = useState('');

  const fetchJobDetails = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);

    try {
      const data = await operatorService.getAssignedJob(id);
      setJob(data);
    } catch (err) {
      console.error('Failed to load job details:', err);
      setError(err.message || 'Job assignment not found or access is denied.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchJobDetails();
  }, [fetchJobDetails]);

  const handleAction = async () => {
    if (!job || !id) return;
    setActionLoading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      let updated;
      switch (modalType) {
        case 'ACCEPT':
          updated = await operatorService.acceptJob(id);
          setSuccessMessage('Job assignment accepted successfully! Ready to dispatch.');
          break;
        case 'REJECT':
          if (!rejectionReason.trim()) {
            throw new Error('Please provide a reason for declining this assignment.');
          }
          updated = await operatorService.rejectJob(id, { rejectionReason: rejectionReason.trim() });
          setSuccessMessage('Job assignment has been declined.');
          break;
        case 'TRAVEL':
          updated = await operatorService.startTravel(id);
          setSuccessMessage('Status updated: You are now en route to the farm location.');
          break;
        case 'REACHED':
          updated = await operatorService.markReached(id);
          setSuccessMessage('Arrival confirmed at the service destination.');
          break;
        case 'START_WORK':
          updated = await operatorService.startWork(id);
          setSuccessMessage('Machinery operations started. Work is now IN PROGRESS.');
          break;
        case 'PAUSE':
          if (!pauseReason.trim()) {
            throw new Error('Please provide a reason for pausing field operations.');
          }
          updated = await operatorService.pauseJob(id, { pauseReason: pauseReason.trim() });
          setSuccessMessage('Work operations paused.');
          break;
        case 'RESUME':
          updated = await operatorService.resumeWork(id);
          setSuccessMessage('Operations resumed. Work is back IN PROGRESS.');
          break;
        case 'COMPLETE':
          updated = await operatorService.completeJob(id, { completionNotes: completionNotes.trim() });
          setSuccessMessage('Congratulations! Fieldwork marked as completed.');
          break;
        default:
          break;
      }

      if (updated) {
        setJob(updated);
      } else {
        await fetchJobDetails();
      }
      setModalType(null);
    } catch (err) {
      console.error('Lifecycle action error:', err);
      setError(err.message || 'Failed to update job status.');
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ASSIGNED':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'ACCEPTED':
        return 'bg-blue-100 text-blue-800 border-blue-300';
      case 'TRAVELING':
        return 'bg-indigo-100 text-indigo-800 border-indigo-300';
      case 'REACHED':
        return 'bg-teal-100 text-teal-800 border-teal-300';
      case 'IN_PROGRESS':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'PAUSED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'COMPLETED':
        return 'bg-green-100 text-green-900 border-green-400';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-300';
      case 'CANCELLED':
        return 'bg-gray-100 text-gray-800 border-gray-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto p-6 space-y-6 animate-pulse font-sans">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="h-80 bg-gray-200 rounded-3xl" />
          <div className="lg:col-span-2 space-y-4">
            <div className="h-6 bg-gray-200 rounded w-3/4" />
            <div className="h-4 bg-gray-200 rounded w-1/2" />
            <div className="h-40 bg-gray-200 rounded-2xl" />
          </div>
        </div>
      </div>
    );
  }

  if (error && !job) {
    return (
      <div className="max-w-4xl mx-auto space-y-6 font-sans">
        <Link
          to="/operator/jobs"
          className="inline-flex items-center gap-2 text-xs font-bold text-gray-600 hover:text-gray-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Assigned Jobs</span>
        </Link>

        <div className="p-6 bg-red-50 border border-red-200 rounded-3xl text-red-800 space-y-4">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-6 h-6 text-red-600 shrink-0" />
            <div>
              <h3 className="text-base font-bold">Unable to Load Job Assignment</h3>
              <p className="text-xs text-red-700 mt-0.5">{error}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={fetchJobDetails}
            className="px-4 py-2 bg-red-600 text-white text-xs font-bold rounded-xl hover:bg-red-700 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const imageSrc = job.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

  // Lifecycle Steps for Timeline
  const timelineSteps = [
    { key: 'ASSIGNED', label: 'Assigned', time: job.assignedAt },
    { key: 'ACCEPTED', label: 'Accepted', time: job.acceptedAt },
    { key: 'TRAVELING', label: 'Traveling', time: job.travelingAt },
    { key: 'REACHED', label: 'Reached', time: job.reachedAt },
    { key: 'IN_PROGRESS', label: 'In Progress', time: job.workStartedAt },
    { key: 'COMPLETED', label: 'Completed', time: job.completedAt },
  ];

  const statusOrder = ['ASSIGNED', 'ACCEPTED', 'TRAVELING', 'REACHED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED'];
  const currentIndex = statusOrder.indexOf(job.assignmentStatus);

  return (
    <div className="max-w-5xl mx-auto space-y-6 font-sans">

      {/* Navigation Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <Link
              to="/operator/jobs"
              className="p-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors shadow-2xs"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
                  Task #{job.assignmentId} Control Panel
                </h1>
                <span
                  className={`text-[11px] font-black uppercase px-3 py-1 rounded-full border shadow-2xs ${getStatusBadge(
                    job.assignmentStatus
                  )}`}
                >
                  {job.assignmentStatus}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-0.5">
                Booking Reference: #{job.bookingId} • Assigned by {job.assignedBy || 'Partner'}
              </p>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={fetchJobDetails}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors shadow-2xs self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Success Alert */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-900 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="text-xs underline hover:text-emerald-950"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-xs font-medium flex items-center justify-between shadow-xs">
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

      {/* Lifecycle Progress Bar / Timeline */}
      {job.assignmentStatus !== 'REJECTED' && job.assignmentStatus !== 'CANCELLED' && (
        <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-xs font-black uppercase tracking-wider text-gray-500">
              Work Lifecycle Timeline
            </h3>
            {job.assignmentStatus === 'PAUSED' && (
              <span className="text-xs font-extrabold text-amber-700 bg-amber-50 px-2.5 py-1 rounded-lg border border-amber-200 flex items-center gap-1">
                <Pause className="w-3.5 h-3.5" /> Paused ({job.pauseReason || 'Break'})
              </span>
            )}
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-2">
            {timelineSteps.map((step, idx) => {
              const isPastOrCurrent = currentIndex >= idx;
              const isCurrent = job.assignmentStatus === step.key || (job.assignmentStatus === 'PAUSED' && step.key === 'IN_PROGRESS');

              return (
                <div
                  key={step.key}
                  className={`p-3 rounded-2xl border text-center transition-all ${isCurrent
                      ? 'bg-emerald-50 border-emerald-400 text-emerald-950 ring-2 ring-emerald-500/20'
                      : isPastOrCurrent
                        ? 'bg-gray-50 border-gray-200 text-gray-700'
                        : 'bg-white border-dashed border-gray-200 text-gray-400'
                    }`}
                >
                  <div className="text-[10px] font-black uppercase tracking-wider">
                    {step.label}
                  </div>
                  <div className="text-[10px] text-gray-500 mt-1 font-medium truncate">
                    {step.time ? new Date(step.time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Rejected / Cancelled Notice */}
      {job.assignmentStatus === 'REJECTED' && (
        <div className="p-5 bg-red-50 border border-red-200 rounded-3xl text-red-900 space-y-1 shadow-xs">
          <div className="flex items-center gap-2 font-bold text-sm">
            <XCircle className="w-5 h-5 text-red-600" />
            <span>Job Declined by Operator</span>
          </div>
          <p className="text-xs text-red-800">
            <strong>Reason:</strong> {job.rejectionReason || 'No reason specified'}
          </p>
          <p className="text-[11px] text-red-600">Declined on {job.rejectedAt ? new Date(job.rejectedAt).toLocaleString() : ''}</p>
        </div>
      )}

      {/* Main Details Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* Machinery Info */}
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 space-y-4">
            <div className="relative h-56 w-full rounded-2xl overflow-hidden bg-gray-100">
              <img
                src={imageSrc}
                alt={job.equipmentName || 'Equipment'}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                }}
              />
              <span className="absolute bottom-3 left-3 bg-[#142E1C]/90 text-white text-[10px] font-black px-2.5 py-1 rounded-lg uppercase tracking-wider backdrop-blur-xs">
                {formatCategoryLabel(job.equipmentCategory)}
              </span>
            </div>

            <div>
              <h2 className="text-lg font-black text-gray-900">
                {job.equipmentName || `Machinery #${job.equipmentId}`}
              </h2>
              <p className="text-xs text-gray-500 mt-0.5">
                Equipment ID: #{job.equipmentId || 'N/A'}
              </p>
            </div>

            <div className="p-3 bg-emerald-50 rounded-2xl border border-emerald-100 space-y-1">
              <div className="text-[11px] font-bold text-emerald-900 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-600" />
                <span>Verified Agricultural Machine</span>
              </div>
              <p className="text-[10px] text-emerald-700">
                Follow all machinery safety instructions during operation.
              </p>
            </div>
          </div>
        </div>

        {/* Task Details & Dynamic Action Panel */}
        <div className="lg:col-span-2 space-y-6">

          {/* Action Control Panel Card */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-5">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-base font-black text-[#142E1C] flex items-center gap-2">
                <Tractor className="w-5 h-5 text-[#3E7B27]" />
                <span>Field Operation Controls</span>
              </h3>
              <span className="text-xs text-gray-400 font-semibold">Active State: {job.assignmentStatus}</span>
            </div>

            {/* Dynamic Buttons based on current state */}
            <div className="space-y-3">
              {job.assignmentStatus === 'ASSIGNED' && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setModalType('ACCEPT')}
                    className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Accept Job Assignment</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setModalType('REJECT')}
                    className="w-full py-3.5 bg-white border border-red-300 hover:bg-red-50 text-red-700 text-xs font-black rounded-xl shadow-2xs transition-all flex items-center justify-center gap-2"
                  >
                    <XCircle className="w-4 h-4" />
                    <span>Decline Assignment</span>
                  </button>
                </div>
              )}

              {job.assignmentStatus === 'ACCEPTED' && (
                <button
                  type="button"
                  onClick={() => setModalType('TRAVEL')}
                  className="w-full py-3.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                >
                  <Navigation className="w-4 h-4" />
                  <span>Start Traveling to Location</span>
                </button>
              )}

              {job.assignmentStatus === 'TRAVELING' && (
                <button
                  type="button"
                  onClick={() => setModalType('REACHED')}
                  className="w-full py-3.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                >
                  <MapPin className="w-4 h-4" />
                  <span>Confirm Arrival at Destination</span>
                </button>
              )}

              {job.assignmentStatus === 'REACHED' && (
                <button
                  type="button"
                  onClick={() => setModalType('START_WORK')}
                  className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                >
                  <Play className="w-4 h-4" />
                  <span>Start Field Operations</span>
                </button>
              )}

              {job.assignmentStatus === 'IN_PROGRESS' && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setModalType('PAUSE')}
                    className="w-full py-3.5 bg-amber-500 hover:bg-amber-600 text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                  >
                    <Pause className="w-4 h-4" />
                    <span>Pause Field Work</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setModalType('COMPLETE')}
                    className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Complete Fieldwork</span>
                  </button>
                </div>
              )}

              {job.assignmentStatus === 'PAUSED' && (
                <button
                  type="button"
                  onClick={() => setModalType('RESUME')}
                  className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-xs transition-all flex items-center justify-center gap-2"
                >
                  <Play className="w-4 h-4" />
                  <span>Resume Field Operations</span>
                </button>
              )}

              {job.assignmentStatus === 'COMPLETED' && (
                <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-900 text-xs space-y-1">
                  <div className="flex items-center gap-2 font-black text-sm text-emerald-800">
                    <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                    <span>Task Completed Successfully</span>
                  </div>
                  {job.completionNotes && (
                    <p className="text-emerald-800 pt-1">
                      <strong>Completion Notes:</strong> {job.completionNotes}
                    </p>
                  )}
                  <p className="text-[11px] text-emerald-700">
                    Completed at: {job.completedAt ? new Date(job.completedAt).toLocaleString() : 'N/A'}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Service & Booking Information */}
          <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-5">
            <h3 className="text-base font-black text-[#142E1C] flex items-center gap-2">
              <FileText className="w-4 h-4 text-[#3E7B27]" />
              <span>Service & Farm Location</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="p-4 bg-[#F0EFE9] rounded-2xl space-y-1">
                <span className="text-[10px] uppercase font-black tracking-wider text-gray-500">
                  Scheduled Start Date
                </span>
                <p className="text-sm font-extrabold text-gray-900 flex items-center gap-1.5">
                  <Calendar className="w-4 h-4 text-emerald-600" />
                  <span>{job.startDate || 'N/A'}</span>
                </p>
              </div>

              <div className="p-4 bg-[#F0EFE9] rounded-2xl space-y-1">
                <span className="text-[10px] uppercase font-black tracking-wider text-gray-500">
                  Scheduled End Date
                </span>
                <p className="text-sm font-extrabold text-gray-900 flex items-center gap-1.5">
                  <Calendar className="w-4 h-4 text-emerald-600" />
                  <span>{job.endDate || 'N/A'}</span>
                </p>
              </div>
            </div>

            <div className="p-4 bg-[#F0EFE9] rounded-2xl space-y-1">
              <span className="text-[10px] uppercase font-black tracking-wider text-gray-500">
                Service Delivery Address
              </span>
              <p className="text-xs sm:text-sm font-semibold text-gray-800 flex items-start gap-1.5">
                <MapPin className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                <span>{job.deliveryAddress || 'Address will be provided upon dispatch'}</span>
              </p>
            </div>

            {job.notes && (
              <div className="p-4 bg-[#F0EFE9] rounded-2xl space-y-1">
                <span className="text-[10px] uppercase font-black tracking-wider text-gray-500">
                  Partner Assignment Notes
                </span>
                <p className="text-xs text-gray-800 leading-relaxed font-medium">
                  {job.notes}
                </p>
              </div>
            )}

            <div className="pt-2 border-t border-gray-100 flex items-center justify-between text-xs text-gray-500">
              <span>Customer: <strong className="text-gray-800">Farmer #{job.farmerId || 'N/A'}</strong></span>
              <span>Total Booking Amount: <strong className="text-[#142E1C] font-black">₹{Number(job.totalCost || 0).toLocaleString('en-IN')}</strong></span>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation & Input Modals */}
      {modalType && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-gray-100 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3">
              <span className="p-3 bg-emerald-50 text-emerald-700 rounded-2xl">
                <HardHat className="w-6 h-6 text-[#3E7B27]" />
              </span>
              <div>
                <h3 className="text-lg font-black text-gray-900">
                  {modalType === 'ACCEPT' && 'Accept Job Assignment'}
                  {modalType === 'REJECT' && 'Decline Job Assignment'}
                  {modalType === 'TRAVEL' && 'Start Traveling'}
                  {modalType === 'REACHED' && 'Confirm Destination Arrival'}
                  {modalType === 'START_WORK' && 'Start Field Operations'}
                  {modalType === 'PAUSE' && 'Pause Field Operations'}
                  {modalType === 'RESUME' && 'Resume Field Operations'}
                  {modalType === 'COMPLETE' && 'Complete Fieldwork'}
                </h3>
                <p className="text-xs text-gray-500">Task #{job.assignmentId} • Booking #{job.bookingId}</p>
              </div>
            </div>

            {/* Modal Body per Type */}
            {modalType === 'ACCEPT' && (
              <p className="text-xs text-gray-700 leading-relaxed">
                Are you sure you want to accept this assignment for <strong>{job.equipmentName}</strong>? You will be scheduled for the service dates.
              </p>
            )}

            {modalType === 'REJECT' && (
              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-700">
                  State Reason for Declining <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  placeholder="Explain why you cannot take this job (e.g. equipment clash, health, location distance)..."
                  className="w-full p-3 bg-[#F0EFE9] rounded-xl text-xs focus:ring-2 focus:ring-red-500 focus:bg-white focus:outline-none"
                />
              </div>
            )}

            {modalType === 'TRAVEL' && (
              <p className="text-xs text-gray-700 leading-relaxed">
                Confirm that you are departing and traveling to the fieldwork destination at <strong>{job.deliveryAddress}</strong>.
              </p>
            )}

            {modalType === 'REACHED' && (
              <p className="text-xs text-gray-700 leading-relaxed">
                Confirm that you have safely arrived at the field location and are ready to prepare the machinery.
              </p>
            )}

            {modalType === 'START_WORK' && (
              <p className="text-xs text-gray-700 leading-relaxed">
                Start tractor/machinery operations now. Work status will change to <strong>IN PROGRESS</strong>.
              </p>
            )}

            {modalType === 'PAUSE' && (
              <div className="space-y-2">
                <label className="text-xs font-bold text-gray-700">
                  Reason for Pause <span className="text-red-500">*</span>
                </label>
                <textarea
                  rows={3}
                  value={pauseReason}
                  onChange={(e) => setPauseReason(e.target.value)}
                  placeholder="Specify pause reason (e.g. Rain/Monsoon, Refueling, Maintenance, Break)..."
                  className="w-full p-3 bg-[#F0EFE9] rounded-xl text-xs focus:ring-2 focus:ring-amber-500 focus:bg-white focus:outline-none"
                />
              </div>
            )}

            {modalType === 'RESUME' && (
              <p className="text-xs text-gray-700 leading-relaxed">
                Ready to resume field operations? Status will return to <strong>IN PROGRESS</strong>.
              </p>
            )}

            {modalType === 'COMPLETE' && (
              <div className="space-y-2">
                <p className="text-xs text-gray-700 leading-relaxed">
                  Confirm that all requested machinery operations and acres have been finished.
                </p>
                <label className="text-xs font-bold text-gray-700">
                  Completion Notes (Optional)
                </label>
                <textarea
                  rows={3}
                  value={completionNotes}
                  onChange={(e) => setCompletionNotes(e.target.value)}
                  placeholder="Add notes (e.g. 5 acres ploughed smoothly, diesel level normal)..."
                  className="w-full p-3 bg-[#F0EFE9] rounded-xl text-xs focus:ring-2 focus:ring-[#3E7B27] focus:bg-white focus:outline-none"
                />
              </div>
            )}

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={actionLoading}
                onClick={() => setModalType(null)}
                className="px-4 py-2 text-xs font-bold text-gray-600 hover:text-gray-800 rounded-xl hover:bg-gray-100 transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={actionLoading}
                onClick={handleAction}
                className={`px-5 py-2.5 text-white text-xs font-black rounded-xl shadow-xs disabled:opacity-50 transition-all ${modalType === 'REJECT'
                    ? 'bg-red-600 hover:bg-red-700'
                    : modalType === 'PAUSE'
                      ? 'bg-amber-600 hover:bg-amber-700'
                      : 'bg-[#3E7B27] hover:bg-[#2E6F22]'
                  }`}
              >
                {actionLoading ? 'Updating...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

export default JobDetails;
