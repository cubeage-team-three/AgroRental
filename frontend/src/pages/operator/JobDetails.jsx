import { useState, useEffect } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { isAuthenticated } from "../../utils/auth";
import { getJobDetails, acceptJob, rejectJob } from "../../services/jobService";

function JobDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Modal states
  const [modalType, setModalType] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
      return;
    }
    loadJobDetails();
  }, [id]);

  const loadJobDetails = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getJobDetails(id);
      setJob(data);
    } catch (err) {
      console.error("Failed to load job details:", err);
      setError(err.message || "Failed to retrieve job details.");
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmAccept = async () => {
    setActionLoading(true);
    try {
      await acceptJob(job.id);
      setActionSuccess("Job accepted successfully! Added to your active deployments.");
      setModalType(null);
      loadJobDetails();
    } catch (err) {
      alert(err.message || "Failed to accept job.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmReject = async () => {
    setActionLoading(true);
    try {
      await rejectJob(job.id, rejectReason);
      setActionSuccess("Job assignment has been declined.");
      setModalType(null);
      loadJobDetails();
    } catch (err) {
      alert(err.message || "Failed to reject job.");
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "ACCEPTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1 bg-emerald-100 text-emerald-900 border border-emerald-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
            ✓ Accepted Assignment
          </span>
        );
      case "PENDING_RESPONSE":
      case "ASSIGNED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1 bg-amber-100 text-amber-900 border border-amber-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse"></span>
            ⏳ Pending Response
          </span>
        );
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1 bg-blue-100 text-blue-900 border border-blue-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
            🎉 Completed Duty
          </span>
        );
      case "REJECTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1 bg-red-100 text-red-900 border border-red-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
            ✕ Rejected
          </span>
        );
      case "CANCELLED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3.5 py-1 bg-gray-100 text-gray-800 border border-gray-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-gray-600"></span>
            🚫 Cancelled
          </span>
        );
      default:
        return (
          <span className="px-3.5 py-1 bg-gray-100 text-gray-800 text-xs font-semibold rounded-full">
            {status}
          </span>
        );
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "Scheduled";
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString("en-IN", {
        weekday: "long",
        day: "numeric",
        month: "long",
        year: "numeric",
      });
    } catch {
      return dateStr;
    }
  };

  const formatTime = (timeStr) => {
    if (!timeStr) return "";
    return timeStr.slice(0, 5);
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto py-16 text-center space-y-3">
        <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-emerald-600 border-t-transparent"></div>
        <p className="text-sm font-semibold text-emerald-950">
          Loading Job Assignment Details...
        </p>
      </div>
    );
  }

  if (error || !job) {
    return (
      <div className="max-w-3xl mx-auto py-12 space-y-6">
        <div className="bg-white rounded-3xl p-8 border border-red-200 shadow-sm text-center space-y-4">
          <div className="w-16 h-16 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto text-2xl font-bold">
            ⚠️
          </div>
          <h2 className="text-xl font-bold text-gray-800">
            Unable to Load Job Details
          </h2>
          <p className="text-sm text-gray-500 max-w-md mx-auto">
            {error || "Job assignment not found or you do not have permission to view it."}
          </p>
          <div className="pt-2">
            <Link
              to="/operator/jobs"
              className="inline-block bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-xs px-6 py-3 rounded-xl transition shadow"
            >
              ← Back to All Assigned Jobs
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Top Breadcrumb & Header */}
      <div className="space-y-3">
        <Link
          to="/operator/jobs"
          className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-800 hover:text-emerald-950 transition"
        >
          <span>←</span>
          <span>Back to Assigned Jobs</span>
        </Link>

        <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-2.5">
              <span className="text-xs font-mono font-bold text-emerald-900 bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 rounded-lg">
                #JOB-{job.id}
              </span>
              <h1 className="text-2xl md:text-3xl font-black text-emerald-950">
                {job.jobTitle}
              </h1>
            </div>
            <p className="text-xs font-semibold text-emerald-800 uppercase tracking-wider">
              {job.jobType ? `Operation Type: ${job.jobType}` : "Agricultural Machinery Operation"}
            </p>
          </div>

          <div className="self-end md:self-center">
            {getStatusBadge(job.status)}
          </div>
        </div>
      </div>

      {/* Toast Alert */}
      {actionSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-950 text-xs font-bold flex items-center justify-between shadow-sm animate-fade-in">
          <div className="flex items-center gap-2">
            <span className="text-base">✅</span>
            <span>{actionSuccess}</span>
          </div>
          <button onClick={() => setActionSuccess("")} className="text-emerald-800 hover:text-emerald-950">
            ✕
          </button>
        </div>
      )}

      {/* Main Grid: Details Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols): Work, Machinery & Schedule */}
        <div className="lg:col-span-2 space-y-6">
          {/* 1. Job Description & Instructions */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-5">
            <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
              <span className="text-xl">📋</span>
              <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                Work Scope & Instructions
              </h2>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <span className="font-bold text-gray-400 uppercase tracking-wider block mb-1">
                  Job Overview
                </span>
                <p className="text-sm font-medium text-gray-800 leading-relaxed bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/40">
                  {job.jobDescription || "Standard field machinery deployment and agricultural operation."}
                </p>
              </div>

              {job.workInstructions && (
                <div>
                  <span className="font-bold text-gray-400 uppercase tracking-wider block mb-1">
                    Special Field Instructions
                  </span>
                  <div className="text-sm font-medium text-amber-950 bg-amber-50/60 p-4 rounded-2xl border border-amber-200/50 leading-relaxed">
                    ⚠️ {job.workInstructions}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 2. Equipment / Machinery Specifications */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-5">
            <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
              <span className="text-xl">🚜</span>
              <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                Assigned Machinery Specifications
              </h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/40 space-y-1">
                <span className="font-bold text-gray-400 uppercase tracking-wider block">
                  Equipment Model
                </span>
                <p className="text-sm font-bold text-gray-900">
                  {job.equipmentName || "Designated Tractor / Harvester"}
                </p>
                {job.equipmentBrand && (
                  <p className="text-gray-500 font-medium">
                    Brand: {job.equipmentBrand} {job.equipmentModel ? `• ${job.equipmentModel}` : ""}
                  </p>
                )}
              </div>

              <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/40 space-y-1">
                <span className="font-bold text-gray-400 uppercase tracking-wider block">
                  Machinery Category
                </span>
                <p className="text-sm font-bold text-emerald-900">
                  {job.equipmentCategory || "Agricultural Equipment"}
                </p>
                <p className="text-gray-500 font-medium">
                  ID: #{job.equipmentId || "N/A"}
                </p>
              </div>
            </div>
          </div>

          {/* 3. Schedule & Location Details */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-5">
            <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
              <span className="text-xl">📍</span>
              <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                Deployment Schedule & Work Location
              </h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/40 space-y-1">
                <span className="font-bold text-gray-400 uppercase tracking-wider block">
                  Scheduled Date & Time
                </span>
                <p className="text-sm font-bold text-gray-900">
                  📅 {formatDate(job.scheduledDate)}
                </p>
                {job.scheduledStartTime && (
                  <p className="text-gray-600 font-medium mt-0.5">
                    ⏰ {formatTime(job.scheduledStartTime)} to {formatTime(job.scheduledEndTime)}
                  </p>
                )}
                {job.estimatedDurationHours && (
                  <p className="text-emerald-700 font-bold">
                    Est. Duration: {job.estimatedDurationHours} Hours
                  </p>
                )}
              </div>

              <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/40 space-y-1">
                <span className="font-bold text-gray-400 uppercase tracking-wider block">
                  Farm Work Location
                </span>
                <p className="text-sm font-bold text-gray-900">
                  📍 {job.workLocation}
                </p>
                {job.latitude && job.longitude && (
                  <p className="text-gray-500 font-mono text-[11px] pt-1">
                    GPS: {job.latitude.toFixed(4)}, {job.longitude.toFixed(4)}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Right Column (1 Col): Customer, Partner, Payout & Module 7 Action Box */}
        <div className="space-y-6">
          {/* Module 7 Response Box if Pending */}
          {(job.status === "PENDING_RESPONSE" || job.status === "ASSIGNED") && (
            <div className="bg-white rounded-3xl p-6 border-2 border-emerald-500 shadow-lg space-y-4">
              <div className="flex items-center gap-2">
                <span className="text-xl">⚡</span>
                <h3 className="font-black text-emerald-950 text-sm uppercase tracking-wider">
                  Respond to Job Assignment
                </h3>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed">
                Please confirm whether you accept this deployment schedule or decline to release it to other operators.
              </p>
              <div className="flex flex-col gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setModalType("ACCEPT")}
                  className="w-full bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs py-3.5 rounded-xl shadow transition text-center"
                >
                  ✓ Accept Job Assignment
                </button>
                <button
                  type="button"
                  onClick={() => setModalType("REJECT")}
                  className="w-full bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs py-3 rounded-xl border border-red-200 transition text-center"
                >
                  ✕ Decline Assignment
                </button>
              </div>
            </div>
          )}

          {/* If already accepted, shortcut to Active Duty Tracker */}
          {job.status === "ACCEPTED" && (
            <div className="bg-emerald-900 text-white rounded-3xl p-6 shadow-md space-y-3">
              <div className="flex items-center gap-2">
                <span className="text-xl">✅</span>
                <h3 className="font-bold text-sm uppercase tracking-wider">
                  Deployment Confirmed
                </h3>
              </div>
              <p className="text-xs text-emerald-100/90 leading-relaxed">
                You have accepted this job. Proceed to the active duty tracker when ready to start work on-site.
              </p>
              <Link
                to="/operator/work"
                className="inline-block w-full bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs py-3 rounded-xl shadow transition text-center"
              >
                Go to Active Duty Tracker →
              </Link>
            </div>
          )}

          {/* Payout Card */}
          <div className="bg-gradient-to-br from-emerald-900 to-green-950 text-white rounded-3xl p-6 shadow-md space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-xs uppercase font-bold text-emerald-200 tracking-wider">
                Operator Compensation
              </span>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-white/10 backdrop-blur-md border border-white/20">
                Guaranteed Payout
              </span>
            </div>

            <div>
              <p className="text-3xl font-black text-lime-300">
                ₹{job.operatorPayout ? Number(job.operatorPayout).toLocaleString("en-IN") : "0"}
              </p>
              <p className="text-xs text-emerald-200 mt-1">
                Direct settlement on completion of work order.
              </p>
            </div>

            <div className="pt-3 border-t border-white/10 text-xs text-emerald-100/80 space-y-1.5">
              <div className="flex justify-between">
                <span>Assignment Status:</span>
                <span className="font-bold text-white">{job.status}</span>
              </div>
              <div className="flex justify-between">
                <span>Assigned By:</span>
                <span className="font-semibold">{job.assignedBy || "Partner Dispatch"}</span>
              </div>
            </div>
          </div>

          {/* Customer Card */}
          <div className="bg-white rounded-3xl p-6 border border-amber-100/70 shadow-sm space-y-4">
            <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
              <span className="text-xl">👤</span>
              <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                Farmer / Customer
              </h2>
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex justify-between py-1 border-b border-gray-100">
                <span className="text-gray-500">Name:</span>
                <span className="font-bold text-gray-800">{job.customerName}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-gray-100">
                <span className="text-gray-500">Mobile:</span>
                <span className="font-bold text-emerald-800">+91 {job.customerMobile}</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-gray-500">Field Site:</span>
                <span className="font-semibold text-gray-700 truncate max-w-[170px]">{job.workLocation}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modals */}
      {modalType === "ACCEPT" && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 space-y-5 border border-amber-100 shadow-2xl animate-scale-in">
            <div className="w-12 h-12 rounded-2xl bg-emerald-100 text-emerald-800 text-2xl flex items-center justify-center mx-auto shadow-inner">
              🚜
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-xl font-black text-emerald-950">
                Accept This Job Assignment?
              </h3>
              <p className="text-xs text-gray-500">
                You are accepting <span className="font-bold text-gray-800">#{job.id} — {job.jobTitle}</span>.
              </p>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={() => setModalType(null)}
                disabled={actionLoading}
                className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs rounded-xl transition"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmAccept}
                disabled={actionLoading}
                className="flex-1 py-3 bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs rounded-xl shadow-md transition"
              >
                {actionLoading ? "Accepting..." : "Confirm Accept ✓"}
              </button>
            </div>
          </div>
        </div>
      )}

      {modalType === "REJECT" && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 space-y-5 border border-red-100 shadow-2xl animate-scale-in">
            <div className="w-12 h-12 rounded-2xl bg-red-100 text-red-700 text-2xl flex items-center justify-center mx-auto shadow-inner">
              ⚠️
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-xl font-black text-gray-900">
                Decline Job Assignment?
              </h3>
              <p className="text-xs text-gray-500">
                Are you sure you want to decline <span className="font-bold text-gray-800">#{job.id}</span>?
              </p>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-600 mb-1">
                Reason for Declining (Optional):
              </label>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="e.g. Schedule conflict, distance too far, personal leave..."
                className="w-full text-xs p-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-red-400 focus:outline-hidden"
                rows="3"
              />
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={() => setModalType(null)}
                disabled={actionLoading}
                className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs rounded-xl transition"
              >
                Go Back
              </button>
              <button
                type="button"
                onClick={handleConfirmReject}
                disabled={actionLoading}
                className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-md transition"
              >
                {actionLoading ? "Declining..." : "Confirm Decline ✕"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default JobDetails;
