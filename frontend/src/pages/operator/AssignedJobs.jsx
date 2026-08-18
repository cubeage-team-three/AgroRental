import { useState, useEffect } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { isAuthenticated } from "../../utils/auth";
import { getAssignedJobs, getJobsSummary, acceptJob, rejectJob } from "../../services/jobService";

function AssignedJobs() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialStatus = searchParams.get("status") || "ALL";

  const [selectedStatus, setSelectedStatus] = useState(initialStatus);
  const [jobs, setJobs] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Modal State for Accept / Reject
  const [modalType, setModalType] = useState(null); // 'ACCEPT' | 'REJECT' | null
  const [selectedJob, setSelectedJob] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const filterTabs = [
    { key: "ALL", label: "All Jobs" },
    { key: "PENDING_RESPONSE", label: "Pending Response" },
    { key: "ACCEPTED", label: "Accepted" },
    { key: "COMPLETED", label: "Completed" },
    { key: "REJECTED", label: "Rejected" },
    { key: "CANCELLED", label: "Cancelled" },
  ];

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
      return;
    }
    loadJobsData(selectedStatus);
  }, [selectedStatus]);

  useEffect(() => {
    const statusFromUrl = searchParams.get("status") || "ALL";
    if (statusFromUrl !== selectedStatus) {
      setSelectedStatus(statusFromUrl);
    }
  }, [searchParams]);

  const loadJobsData = async (statusFilter) => {
    setLoading(true);
    setError("");

    try {
      const filter = statusFilter === "ALL" ? null : statusFilter;
      const [jobsData, summaryData] = await Promise.all([
        getAssignedJobs(filter),
        getJobsSummary().catch(() => null),
      ]);

      setJobs(Array.isArray(jobsData) ? jobsData : []);
      if (summaryData) setSummary(summaryData);
    } catch (err) {
      console.error("Failed to load assigned jobs:", err);
      setError(err.message || "Failed to retrieve assigned jobs from server");
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (statusKey) => {
    setSelectedStatus(statusKey);
    if (statusKey === "ALL") {
      setSearchParams({});
    } else {
      setSearchParams({ status: statusKey });
    }
  };

  const openAcceptModal = (job) => {
    setSelectedJob(job);
    setModalType("ACCEPT");
  };

  const openRejectModal = (job) => {
    setSelectedJob(job);
    setRejectReason("");
    setModalType("REJECT");
  };

  const closeModal = () => {
    setModalType(null);
    setSelectedJob(null);
    setRejectReason("");
  };

  const handleConfirmAccept = async () => {
    if (!selectedJob) return;
    setActionLoading(true);
    try {
      await acceptJob(selectedJob.id);
      setActionSuccess(`Job #${selectedJob.id} accepted successfully! Added to your active deployments.`);
      closeModal();
      loadJobsData(selectedStatus);
    } catch (err) {
      alert(err.message || "Failed to accept job.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmReject = async () => {
    if (!selectedJob) return;
    setActionLoading(true);
    try {
      await rejectJob(selectedJob.id, rejectReason);
      setActionSuccess(`Job #${selectedJob.id} has been declined.`);
      closeModal();
      loadJobsData(selectedStatus);
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
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-100 text-emerald-900 border border-emerald-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
            ✓ Accepted
          </span>
        );
      case "TRAVELING":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-100 text-amber-900 border border-amber-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse"></span>
            🚜 Traveling
          </span>
        );
      case "REACHED_LOCATION":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-lime-100 text-lime-950 border border-lime-400 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-lime-600"></span>
            📍 Reached Location
          </span>
        );
      case "WORK_STARTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-900 text-lime-300 border border-emerald-800 text-xs font-bold rounded-full shadow-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-lime-400 animate-pulse"></span>
            ⚡ Work In Progress
          </span>
        );
      case "WORK_PAUSED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-200 text-amber-950 border border-amber-400 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-800"></span>
            ⏸️ Work Paused
          </span>
        );
      case "WORK_RESUMED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-900 text-lime-300 border border-emerald-800 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-lime-400 animate-pulse"></span>
            🔄 Work Resumed
          </span>
        );
      case "WORK_COMPLETED":
      case "COMPLETED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-100 text-blue-900 border border-blue-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
            🎉 Completed
          </span>
        );
      case "PENDING_RESPONSE":
      case "ASSIGNED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-100 text-amber-900 border border-amber-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse"></span>
            ⏳ Pending Response
          </span>
        );
      case "REJECTED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-100 text-red-900 border border-red-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
            ✕ Rejected
          </span>
        );
      case "CANCELLED":
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-gray-100 text-gray-800 border border-gray-300 text-xs font-bold rounded-full">
            <span className="w-1.5 h-1.5 rounded-full bg-gray-600"></span>
            🚫 Cancelled
          </span>
        );
      default:
        return (
          <span className="px-3 py-1 bg-gray-100 text-gray-800 text-xs font-semibold rounded-full">
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
        day: "numeric",
        month: "short",
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

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">🚜</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 6 & 7 — Work Dispatch & Response
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Assigned Work & Deployments
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Review equipment assignments, accept pending field deployments, and manage your active schedule.
          </p>
        </div>

        <div className="flex gap-2">
          <Link
            to="/operator/dashboard"
            className="inline-flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs px-4 py-2.5 rounded-xl transition"
          >
            <span>📊</span>
            <span>Dashboard</span>
          </Link>
          <Link
            to="/operator/work"
            className="inline-flex items-center gap-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-900 font-bold text-xs px-4 py-2.5 rounded-xl border border-emerald-200 transition"
          >
            <span>⚡</span>
            <span>Active Duty</span>
          </Link>
        </div>
      </div>

      {/* Action Success Toast */}
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

      {/* Summary Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div
          onClick={() => handleTabChange("ALL")}
          className={`cursor-pointer bg-white p-5 rounded-2xl border transition-all duration-200 ${
            selectedStatus === "ALL"
              ? "border-emerald-600 shadow-md ring-2 ring-emerald-500/20"
              : "border-amber-100/70 shadow-sm hover:border-emerald-300"
          }`}
        >
          <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
            Total Assigned
          </span>
          <p className="text-2xl md:text-3xl font-black text-emerald-950 mt-1">
            {summary?.totalAssigned !== undefined ? summary.totalAssigned : jobs.length}
          </p>
          <p className="text-[11px] text-gray-500 mt-0.5">All Work Orders</p>
        </div>

        <div
          onClick={() => handleTabChange("PENDING_RESPONSE")}
          className={`cursor-pointer bg-white p-5 rounded-2xl border transition-all duration-200 ${
            selectedStatus === "PENDING_RESPONSE"
              ? "border-amber-500 shadow-md ring-2 ring-amber-500/20"
              : "border-amber-100/70 shadow-sm hover:border-amber-300"
          }`}
        >
          <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider">
            Pending Response
          </span>
          <p className="text-2xl md:text-3xl font-black text-amber-700 mt-1">
            {summary?.pendingResponse || 0}
          </p>
          <p className="text-[11px] text-gray-500 mt-0.5">Awaiting Your Action</p>
        </div>

        <div
          onClick={() => handleTabChange("ACCEPTED")}
          className={`cursor-pointer bg-white p-5 rounded-2xl border transition-all duration-200 ${
            selectedStatus === "ACCEPTED"
              ? "border-emerald-600 shadow-md ring-2 ring-emerald-500/20"
              : "border-amber-100/70 shadow-sm hover:border-emerald-300"
          }`}
        >
          <span className="text-[11px] font-bold text-emerald-700 uppercase tracking-wider">
            Accepted
          </span>
          <p className="text-2xl md:text-3xl font-black text-emerald-800 mt-1">
            {summary?.accepted || 0}
          </p>
          <p className="text-[11px] text-gray-500 mt-0.5">Confirmed Jobs</p>
        </div>

        <div
          onClick={() => handleTabChange("COMPLETED")}
          className={`cursor-pointer bg-white p-5 rounded-2xl border transition-all duration-200 ${
            selectedStatus === "COMPLETED"
              ? "border-blue-500 shadow-md ring-2 ring-blue-500/20"
              : "border-amber-100/70 shadow-sm hover:border-blue-300"
          }`}
        >
          <span className="text-[11px] font-bold text-blue-700 uppercase tracking-wider">
            Completed
          </span>
          <p className="text-2xl md:text-3xl font-black text-blue-800 mt-1">
            {summary?.completed || 0}
          </p>
          <p className="text-[11px] text-gray-500 mt-0.5">Finished Work</p>
        </div>
      </div>

      {/* Filter Tabs Bar */}
      <div className="bg-white rounded-2xl p-2 border border-amber-100/70 shadow-sm flex flex-wrap gap-1.5">
        {filterTabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => handleTabChange(tab.key)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all duration-150 ${
              selectedStatus === tab.key
                ? "bg-emerald-800 text-white shadow-sm"
                : "text-gray-600 hover:bg-emerald-50 hover:text-emerald-900"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content Area */}
      {loading ? (
        <div className="bg-white rounded-3xl p-12 border border-amber-100/70 text-center space-y-3 shadow-sm">
          <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-emerald-600 border-t-transparent"></div>
          <p className="text-sm font-semibold text-emerald-950">
            Loading assigned jobs...
          </p>
        </div>
      ) : jobs.length === 0 ? (
        /* Empty State */
        <div className="bg-white rounded-3xl p-12 border border-amber-100/70 text-center space-y-4 shadow-sm">
          <div className="w-16 h-16 bg-emerald-50 rounded-2xl text-3xl flex items-center justify-center mx-auto text-emerald-800 shadow-inner">
            🌾
          </div>
          <div>
            <h3 className="text-lg font-bold text-emerald-950">
              No Jobs Found
            </h3>
            <p className="text-xs md:text-sm text-gray-500 max-w-md mx-auto mt-1">
              {selectedStatus === "ALL"
                ? "You have no active job assignments at this moment. New field deployments assigned by partners will appear here."
                : `There are currently no assigned jobs with status "${selectedStatus}".`}
            </p>
          </div>
          {selectedStatus !== "ALL" && (
            <button
              type="button"
              onClick={() => handleTabChange("ALL")}
              className="inline-block bg-emerald-800 hover:bg-emerald-900 text-white font-bold text-xs px-5 py-2.5 rounded-xl shadow transition"
            >
              Show All Assigned Jobs
            </button>
          )}
        </div>
      ) : (
        /* Jobs List Cards */
        <div className="space-y-4">
          {jobs.map((job) => (
            <div
              key={job.id}
              className="bg-white rounded-3xl p-6 border border-amber-100/70 shadow-sm hover:shadow-md transition-all duration-200 space-y-4"
            >
              {/* Card Top */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-gray-100 pb-3">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-2xl shrink-0">
                    🚜
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-[11px] font-mono font-bold text-gray-400 bg-gray-100 px-2 py-0.5 rounded">
                        #JOB-{job.id}
                      </span>
                      <h2 className="text-base md:text-lg font-black text-emerald-950">
                        {job.jobTitle}
                      </h2>
                    </div>
                    <p className="text-xs text-emerald-800 font-semibold mt-0.5">
                      {job.equipmentName
                        ? `${job.equipmentName} (${job.equipmentCategory || "Machinery"})`
                        : job.jobType || "Field Operation"}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 self-end sm:self-center">
                  {getStatusBadge(job.status)}
                </div>
              </div>

              {/* Card Body: Details Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
                <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/50 space-y-1">
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                    Work Location
                  </span>
                  <p className="font-semibold text-gray-800 line-clamp-2">
                    📍 {job.workLocation}
                  </p>
                </div>

                <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/50 space-y-1">
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                    Schedule & Time
                  </span>
                  <p className="font-semibold text-gray-800">
                    📅 {formatDate(job.scheduledDate)}
                  </p>
                  {job.scheduledStartTime && (
                    <p className="text-[11px] text-gray-500 font-medium">
                      ⏰ {formatTime(job.scheduledStartTime)} - {formatTime(job.scheduledEndTime)}
                      {job.estimatedDurationHours ? ` (${job.estimatedDurationHours} hrs)` : ""}
                    </p>
                  )}
                </div>

                <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/50 space-y-1">
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                    Customer / Farmer
                  </span>
                  <p className="font-semibold text-gray-800">
                    👤 {job.customerName}
                  </p>
                  <p className="text-[11px] text-gray-500">
                    📱 +91 {job.customerMobile}
                  </p>
                </div>

                <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/50 space-y-1">
                  <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider block">
                    Operator Payout
                  </span>
                  <p className="text-base font-black text-emerald-950">
                    ₹{job.operatorPayout ? Number(job.operatorPayout).toLocaleString("en-IN") : "0"}
                  </p>
                  <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full inline-block">
                    Guaranteed Rate
                  </span>
                </div>
              </div>

              {/* Card Footer: Module 7 Accept/Reject & View Details */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-2">
                <div className="text-[11px] text-gray-400">
                  Assigned by: <span className="font-semibold text-gray-600">{job.assignedBy || "Partner Dispatch"}</span>
                </div>

                <div className="flex items-center gap-2 self-stretch sm:self-auto">
                  {/* Module 7 Action Buttons if Pending Response */}
                  {(job.status === "PENDING_RESPONSE" || job.status === "ASSIGNED") && (
                    <>
                      <button
                        type="button"
                        onClick={() => openAcceptModal(job)}
                        className="bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs px-4 py-2.5 rounded-xl shadow-sm transition flex items-center gap-1"
                      >
                        <span>✓</span>
                        <span>Accept Job</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => openRejectModal(job)}
                        className="bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs px-3.5 py-2.5 rounded-xl border border-red-200 transition"
                      >
                        ✕ Decline
                      </button>
                    </>
                  )}

                  {/* Module 8 Track Duty Button for Active Jobs */}
                  {["ACCEPTED", "TRAVELING", "REACHED_LOCATION", "WORK_STARTED", "WORK_PAUSED", "WORK_RESUMED"].includes(job.status) && (
                    <Link
                      to={`/operator/work?jobId=${job.id}`}
                      className="bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs px-4 py-2.5 rounded-xl shadow-sm transition flex items-center gap-1.5"
                    >
                      <span>⚡</span>
                      <span>Duty Tracker</span>
                    </Link>
                  )}

                  <Link
                    to={`/operator/jobs/${job.id}`}
                    className="bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs px-4 py-2.5 rounded-xl transition flex items-center gap-1.5"
                  >
                    <span>View Details</span>
                    <span>→</span>
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Module 7 Confirmation Modals */}
      {modalType === "ACCEPT" && selectedJob && (
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
                You are accepting <span className="font-bold text-gray-800">#{selectedJob.id} — {selectedJob.jobTitle}</span>. This will be added to your confirmed active deployments.
              </p>
            </div>

            <div className="bg-[#FAF8F5] p-4 rounded-2xl border border-amber-100/60 text-xs space-y-1.5">
              <div className="flex justify-between">
                <span className="text-gray-500">Scheduled Date:</span>
                <span className="font-bold text-gray-800">{formatDate(selectedJob.scheduledDate)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Guaranteed Payout:</span>
                <span className="font-bold text-emerald-800">₹{selectedJob.operatorPayout}</span>
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={closeModal}
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

      {modalType === "REJECT" && selectedJob && (
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
                Are you sure you want to decline <span className="font-bold text-gray-800">#{selectedJob.id}</span>? The partner dispatcher will be notified.
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
                onClick={closeModal}
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

export default AssignedJobs;
