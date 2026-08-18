import { useState, useEffect, useCallback } from "react";
import { Link, useSearchParams, useNavigate } from "react-router-dom";
import { getAssignedJobs, getJobDetails, updateJobStatus } from "../../services/jobService";
import { isAuthenticated } from "../../utils/auth";

function ActiveJob() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const requestedJobId = searchParams.get("jobId");

  const [activeJobs, setActiveJobs] = useState([]);
  const [currentJob, setCurrentJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Confirmation Modal State
  const [confirmModal, setConfirmModal] = useState({
    isOpen: false,
    targetStatus: null,
    title: "",
    description: "",
    notes: "",
  });

  const fetchActiveJobs = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch all assigned jobs for the operator
      const response = await getAssignedJobs();
      const allJobs = response.data || [];

      // Filter jobs that are currently active in the work lifecycle
      const activeStatusList = [
        "ACCEPTED",
        "TRAVELING",
        "REACHED_LOCATION",
        "WORK_STARTED",
        "WORK_PAUSED",
        "WORK_RESUMED",
        "WORK_COMPLETED",
      ];

      const filteredActive = allJobs.filter((job) =>
        activeStatusList.includes(job.status)
      );

      setActiveJobs(filteredActive);

      if (filteredActive.length > 0) {
        let selectedJob = null;
        if (requestedJobId) {
          selectedJob = filteredActive.find(
            (j) => j.id.toString() === requestedJobId.toString()
          );
        }
        // If not found or no param, pick first in-progress or accepted job
        if (!selectedJob) {
          selectedJob =
            filteredActive.find((j) => j.status !== "WORK_COMPLETED") ||
            filteredActive[0];
        }

        // Fetch detailed job data for accurate milestones & timestamps
        if (selectedJob) {
          const detailRes = await getJobDetails(selectedJob.id);
          setCurrentJob(detailRes.data || selectedJob);
          if (!requestedJobId || requestedJobId !== selectedJob.id.toString()) {
            setSearchParams({ jobId: selectedJob.id.toString() });
          }
        }
      } else {
        setCurrentJob(null);
      }
    } catch (err) {
      console.error("Failed to load active jobs:", err);
      setError(err.message || "Failed to load active job details. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [requestedJobId, setSearchParams]);

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
      return;
    }
    fetchActiveJobs();
  }, [fetchActiveJobs, navigate]);

  const handleSelectJob = async (jobId) => {
    try {
      setLoading(true);
      setSearchParams({ jobId: jobId.toString() });
      const res = await getJobDetails(jobId);
      setCurrentJob(res.data);
      setSuccessMessage(null);
      setError(null);
    } catch (err) {
      setError(err.message || "Failed to switch job details.");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenConfirm = (targetStatus, title, description) => {
    setConfirmModal({
      isOpen: true,
      targetStatus,
      title,
      description,
      notes: "",
    });
  };

  const handleCloseConfirm = () => {
    setConfirmModal({
      isOpen: false,
      targetStatus: null,
      title: "",
      description: "",
      notes: "",
    });
  };

  const executeStatusTransition = async (targetStatus, notes = "") => {
    if (!currentJob || updating) return;

    try {
      setUpdating(true);
      setError(null);
      setSuccessMessage(null);

      const response = await updateJobStatus(currentJob.id, targetStatus, notes);
      const updatedJob = response.data;

      // Update current job in state directly from backend response
      setCurrentJob(updatedJob);
      setSuccessMessage(
        `Job status successfully updated to ${formatStatusLabel(targetStatus)}!`
      );

      // Refresh list to keep selector synchronized
      const listRes = await getAssignedJobs();
      const allJobs = listRes.data || [];
      const activeStatusList = [
        "ACCEPTED",
        "TRAVELING",
        "REACHED_LOCATION",
        "WORK_STARTED",
        "WORK_PAUSED",
        "WORK_RESUMED",
        "WORK_COMPLETED",
      ];
      setActiveJobs(allJobs.filter((j) => activeStatusList.includes(j.status)));

      handleCloseConfirm();
    } catch (err) {
      console.error("Status update error:", err);
      setError(err.message || "Failed to update job status. Please try again.");
    } finally {
      setUpdating(false);
    }
  };

  const formatStatusLabel = (status) => {
    if (!status) return "Unknown";
    switch (status) {
      case "ASSIGNED":
        return "Assigned";
      case "PENDING_RESPONSE":
        return "Pending Response";
      case "ACCEPTED":
        return "Accepted";
      case "TRAVELING":
        return "Traveling to Field";
      case "REACHED_LOCATION":
        return "Reached Location";
      case "WORK_STARTED":
        return "Work In Progress";
      case "WORK_PAUSED":
        return "Work Paused";
      case "WORK_RESUMED":
        return "Work Resumed";
      case "WORK_COMPLETED":
        return "Work Completed";
      case "COMPLETED":
        return "Completed";
      case "REJECTED":
        return "Declined";
      case "CANCELLED":
        return "Cancelled";
      default:
        return status.replace(/_/g, " ");
    }
  };

  const formatDateTime = (dateStr) => {
    if (!dateStr) return null;
    try {
      const d = new Date(dateStr);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) + ", " + d.toLocaleDateString([], { month: "short", day: "numeric" });
    } catch {
      return dateStr;
    }
  };

  // Milestone Stepper Definitions
  const workflowMilestones = [
    {
      key: "ACCEPTED",
      label: "Accepted",
      subLabel: "Duty Confirmed",
      icon: "🤝",
      timestampKey: "createdAt",
    },
    {
      key: "TRAVELING",
      label: "Traveling",
      subLabel: "En Route to Site",
      icon: "🚜",
      timestampKey: "travelingAt",
    },
    {
      key: "REACHED_LOCATION",
      label: "Reached",
      subLabel: "On-Site Check",
      icon: "📍",
      timestampKey: "reachedLocationAt",
    },
    {
      key: "WORK_STARTED",
      label: "Started",
      subLabel: "Machinery Active",
      icon: "⚡",
      timestampKey: "workStartedAt",
    },
    {
      key: "WORK_PAUSED_OR_RESUMED",
      label: currentJob?.status === "WORK_PAUSED" ? "Paused" : "In Duty",
      subLabel: currentJob?.status === "WORK_PAUSED" ? "Break / Refuel" : "Field Operation",
      icon: currentJob?.status === "WORK_PAUSED" ? "⏸️" : "🔄",
      timestampKey: currentJob?.status === "WORK_PAUSED" ? "workPausedAt" : "workResumedAt",
    },
    {
      key: "WORK_COMPLETED",
      label: "Completed",
      subLabel: "Duty Finished",
      icon: "🎉",
      timestampKey: "workCompletedAt",
    },
  ];

  const getMilestoneState = (milestoneKey) => {
    if (!currentJob) return "pending";
    const status = currentJob.status;

    const rankMap = {
      PENDING_RESPONSE: 0,
      ASSIGNED: 0,
      ACCEPTED: 1,
      TRAVELING: 2,
      REACHED_LOCATION: 3,
      WORK_STARTED: 4,
      WORK_PAUSED: 5,
      WORK_RESUMED: 5,
      WORK_COMPLETED: 6,
      COMPLETED: 6,
    };

    let stepRank = 0;
    if (milestoneKey === "ACCEPTED") stepRank = 1;
    else if (milestoneKey === "TRAVELING") stepRank = 2;
    else if (milestoneKey === "REACHED_LOCATION") stepRank = 3;
    else if (milestoneKey === "WORK_STARTED") stepRank = 4;
    else if (milestoneKey === "WORK_PAUSED_OR_RESUMED") stepRank = 5;
    else if (milestoneKey === "WORK_COMPLETED") stepRank = 6;

    const currentRank = rankMap[status] || 0;

    if (currentRank > stepRank) return "completed";
    if (currentRank === stepRank) return "current";
    return "pending";
  };

  const getMilestoneTimestamp = (milestone) => {
    if (!currentJob) return null;
    if (milestone.timestampKey && currentJob[milestone.timestampKey]) {
      return formatDateTime(currentJob[milestone.timestampKey]);
    }
    // Check milestones audit list
    if (currentJob.milestones && currentJob.milestones.length > 0) {
      const match = currentJob.milestones.find(
        (m) =>
          m.status === milestone.key ||
          (milestone.key === "WORK_PAUSED_OR_RESUMED" &&
            (m.status === "WORK_PAUSED" || m.status === "WORK_RESUMED"))
      );
      if (match && match.timestamp) {
        return formatDateTime(match.timestamp);
      }
    }
    return null;
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-16">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">⚡</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 8 — Work Status Management
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Active Job Status & Duty Tracker
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Live operational status dispatch. Update milestone progression from travel check-in to field completion.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {activeJobs.length > 1 && (
            <div className="relative">
              <select
                value={currentJob?.id || ""}
                onChange={(e) => handleSelectJob(Number(e.target.value))}
                className="bg-emerald-50 text-emerald-950 text-xs font-bold px-3.5 py-2.5 rounded-xl border border-emerald-300 focus:outline-none focus:ring-2 focus:ring-lime-400"
              >
                {activeJobs.map((job) => (
                  <option key={job.id} value={job.id}>
                    Job #{job.id}: {job.jobTitle} ({formatStatusLabel(job.status)})
                  </option>
                ))}
              </select>
            </div>
          )}

          <Link
            to="/operator/jobs"
            className="bg-emerald-50 hover:bg-emerald-100 text-emerald-900 text-xs font-bold px-4 py-2.5 rounded-xl border border-emerald-200 transition"
          >
            ← Assigned Jobs
          </Link>
        </div>
      </div>

      {/* Alert Messages */}
      {error && (
        <div className="bg-red-50 border-l-4 border-red-500 p-4 rounded-2xl flex items-start justify-between gap-3 text-red-800 text-xs">
          <div className="flex items-center gap-2 font-medium">
            <span>⚠️</span>
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="font-bold text-red-500 hover:text-red-700"
          >
            ✕
          </button>
        </div>
      )}

      {successMessage && (
        <div className="bg-emerald-50 border-l-4 border-emerald-500 p-4 rounded-2xl flex items-start justify-between gap-3 text-emerald-900 text-xs">
          <div className="flex items-center gap-2 font-medium">
            <span>✅</span>
            <span>{successMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessMessage(null)}
            className="font-bold text-emerald-600 hover:text-emerald-800"
          >
            ✕
          </button>
        </div>
      )}

      {/* Loading Spinner */}
      {loading && (
        <div className="bg-white rounded-3xl p-12 border border-amber-100/70 shadow-sm text-center">
          <div className="inline-block w-8 h-8 border-4 border-emerald-800 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-xs font-bold text-gray-500 mt-3">Loading active duty parameters...</p>
        </div>
      )}

      {/* Empty State: No Active Jobs */}
      {!loading && !currentJob && (
        <div className="bg-white rounded-3xl p-12 border border-amber-100/70 shadow-sm text-center space-y-4">
          <div className="w-16 h-16 bg-amber-50 text-amber-700 border border-amber-200 rounded-3xl flex items-center justify-center text-3xl mx-auto">
            🚜
          </div>
          <h2 className="text-xl font-black text-emerald-950">No Active Jobs Currently in Progress</h2>
          <p className="text-xs md:text-sm text-gray-500 max-w-md mx-auto">
            You do not currently have any accepted or active field jobs. Please review your assigned jobs and accept a pending assignment to start tracking duty progress.
          </p>
          <div className="pt-2">
            <Link
              to="/operator/jobs"
              className="inline-block bg-lime-400 hover:bg-lime-300 text-emerald-950 font-bold text-xs px-6 py-3 rounded-xl shadow transition"
            >
              View Assigned Jobs List →
            </Link>
          </div>
        </div>
      )}

      {/* Active Job Content */}
      {!loading && currentJob && (
        <div className="space-y-6">
          {/* Main Job Overview Card */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-amber-100/60 pb-5">
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-[11px] font-black uppercase tracking-wider bg-lime-400/20 text-emerald-900 border border-lime-400/40 px-2.5 py-0.5 rounded-lg">
                    Job #{currentJob.id}
                  </span>
                  <span className="text-xs font-bold text-gray-400">•</span>
                  <span className="text-xs font-bold text-emerald-800">
                    {currentJob.jobType || "Field Operation"}
                  </span>
                </div>
                <h2 className="text-xl md:text-2xl font-black text-emerald-950 mt-1">
                  {currentJob.jobTitle}
                </h2>
              </div>

              {/* Status Badge */}
              <div className="flex items-center gap-3">
                <span
                  className={`text-xs font-bold px-4 py-2 rounded-xl border flex items-center gap-2 ${
                    currentJob.status === "WORK_COMPLETED" || currentJob.status === "COMPLETED"
                      ? "bg-emerald-100 text-emerald-950 border-emerald-300"
                      : currentJob.status === "WORK_PAUSED"
                      ? "bg-amber-100 text-amber-900 border-amber-300"
                      : "bg-lime-400 text-emerald-950 border-lime-500 shadow-sm"
                  }`}
                >
                  <span className="w-2 h-2 rounded-full bg-emerald-950 animate-pulse"></span>
                  <span>{formatStatusLabel(currentJob.status)}</span>
                </span>
              </div>
            </div>

            {/* Job Metadata Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
              <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Customer</p>
                <p className="text-xs font-bold text-emerald-950 mt-0.5 truncate">{currentJob.customerName}</p>
                <p className="text-[11px] text-gray-500">{currentJob.customerMobile}</p>
              </div>

              <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Work Location</p>
                <p className="text-xs font-bold text-emerald-950 mt-0.5 truncate" title={currentJob.workLocation}>
                  {currentJob.workLocation}
                </p>
                <p className="text-[11px] text-gray-500">Field Site</p>
              </div>

              <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Machinery</p>
                <p className="text-xs font-bold text-emerald-950 mt-0.5 truncate">
                  {currentJob.equipmentBrand || "Assigned"} {currentJob.equipmentModel || "Equipment"}
                </p>
                <p className="text-[11px] text-emerald-700 font-semibold">{currentJob.equipmentName || "Tractor / Harvester"}</p>
              </div>

              <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Scheduled Date</p>
                <p className="text-xs font-bold text-emerald-950 mt-0.5">{currentJob.scheduledDate}</p>
                <p className="text-[11px] text-gray-500">
                  {currentJob.scheduledStartTime ? currentJob.scheduledStartTime.slice(0, 5) : "Full Day"}
                </p>
              </div>

              <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60 col-span-2 sm:col-span-1">
                <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Operator Payout</p>
                <p className="text-base font-black text-emerald-950 mt-0.5">
                  ₹{Number(currentJob.operatorPayout || 0).toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                </p>
                <p className="text-[10px] text-lime-700 font-bold">Guaranteed Rate</p>
              </div>
            </div>
          </div>

          {/* 7-Step Visual Stepper Card */}
          <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-6">
            <div className="flex items-center justify-between border-b border-amber-100/60 pb-4">
              <div>
                <h3 className="text-sm font-black text-emerald-950 uppercase tracking-wider">
                  Operational Milestone Stepper
                </h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  Live status transition track logged directly to server timestamps.
                </p>
              </div>

              <span className="text-xs font-bold text-emerald-900 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200">
                Backend Synced ✓
              </span>
            </div>

            {/* Stepper Grid */}
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
              {workflowMilestones.map((milestone, idx) => {
                const state = getMilestoneState(milestone.key);
                const timestamp = getMilestoneTimestamp(milestone);

                return (
                  <div
                    key={idx}
                    className={`p-4 rounded-2xl border transition-all flex flex-col justify-between space-y-3 ${
                      state === "current"
                        ? "bg-emerald-950 text-white border-emerald-950 shadow-lg ring-2 ring-lime-400"
                        : state === "completed"
                        ? "bg-emerald-50/80 border-emerald-200 text-emerald-950"
                        : "bg-[#FAF8F5] border-amber-100/40 text-gray-400"
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span
                        className={`w-7 h-7 rounded-xl font-bold text-xs flex items-center justify-center ${
                          state === "current"
                            ? "bg-lime-400 text-emerald-950"
                            : state === "completed"
                            ? "bg-emerald-700 text-white"
                            : "bg-gray-200 text-gray-500"
                        }`}
                      >
                        {state === "completed" ? "✓" : idx + 1}
                      </span>
                      <span className="text-sm">{milestone.icon}</span>
                    </div>

                    <div>
                      <p className={`font-bold text-xs ${state === "current" ? "text-white" : "text-emerald-950"}`}>
                        {milestone.label}
                      </p>
                      <p className={`text-[10px] mt-0.5 ${state === "current" ? "text-emerald-200" : "text-gray-500"}`}>
                        {milestone.subLabel}
                      </p>

                      {timestamp && (
                        <p className={`text-[10px] font-semibold mt-2 pt-1.5 border-t ${
                          state === "current" ? "border-emerald-800 text-lime-300" : "border-emerald-200 text-emerald-700"
                        }`}>
                          🕒 {timestamp}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>

            {/* Milestone Actions Panel */}
            <div className="bg-[#FAF8F5] p-6 rounded-2xl border border-amber-100/70 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h4 className="text-xs font-bold uppercase tracking-wider text-gray-500">
                  Next Operational Action
                </h4>
                <p className="text-sm font-black text-emerald-950 mt-0.5">
                  Current Status: <span className="text-emerald-800">{formatStatusLabel(currentJob.status)}</span>
                </p>
              </div>

              {/* Action Buttons based on Current Status */}
              <div className="flex items-center flex-wrap gap-3">
                {/* 1. ACCEPTED -> TRAVELING */}
                {currentJob.status === "ACCEPTED" && (
                  <button
                    type="button"
                    disabled={updating}
                    onClick={() => executeStatusTransition("TRAVELING", "Operator en route to work location")}
                    className="bg-emerald-900 hover:bg-emerald-950 text-white font-bold text-xs px-5 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                  >
                    {updating ? "Updating..." : "🚜 Start Traveling →"}
                  </button>
                )}

                {/* 2. TRAVELING -> REACHED_LOCATION */}
                {currentJob.status === "TRAVELING" && (
                  <button
                    type="button"
                    disabled={updating}
                    onClick={() => executeStatusTransition("REACHED_LOCATION", "Operator reached work location")}
                    className="bg-emerald-900 hover:bg-emerald-950 text-white font-bold text-xs px-5 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                  >
                    {updating ? "Updating..." : "📍 Mark Reached Location →"}
                  </button>
                )}

                {/* 3. REACHED_LOCATION -> WORK_STARTED */}
                {currentJob.status === "REACHED_LOCATION" && (
                  <button
                    type="button"
                    disabled={updating}
                    onClick={() =>
                      handleOpenConfirm(
                        "WORK_STARTED",
                        "Confirm Start Field Operation",
                        "Are you ready to initiate the machinery and start the field duty? This will timestamp your active operational start time."
                      )
                    }
                    className="bg-lime-400 hover:bg-lime-300 text-emerald-950 font-black text-xs px-6 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                  >
                    {updating ? "Updating..." : "⚡ Start Work Duty →"}
                  </button>
                )}

                {/* 4. WORK_STARTED -> WORK_PAUSED or WORK_COMPLETED */}
                {currentJob.status === "WORK_STARTED" && (
                  <>
                    <button
                      type="button"
                      disabled={updating}
                      onClick={() =>
                        handleOpenConfirm(
                          "WORK_PAUSED",
                          "Pause Work Duty",
                          "Please confirm pausing the work duty for machinery check, refueling, or meal break."
                        )
                      }
                      className="bg-amber-100 hover:bg-amber-200 text-amber-950 border border-amber-300 font-bold text-xs px-4 py-3 rounded-xl transition disabled:bg-gray-400 flex items-center gap-2"
                    >
                      {updating ? "Updating..." : "⏸️ Pause Work"}
                    </button>

                    <button
                      type="button"
                      disabled={updating}
                      onClick={() =>
                        handleOpenConfirm(
                          "WORK_COMPLETED",
                          "Complete Work Duty",
                          "Are you sure you have completed the assigned field operations for this job? Once completed, the payout will be submitted for settlement verification."
                        )
                      }
                      className="bg-emerald-900 hover:bg-emerald-950 text-white font-black text-xs px-6 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                    >
                      {updating ? "Updating..." : "🎉 Complete Work Duty →"}
                    </button>
                  </>
                )}

                {/* 5. WORK_PAUSED -> WORK_RESUMED */}
                {currentJob.status === "WORK_PAUSED" && (
                  <button
                    type="button"
                    disabled={updating}
                    onClick={() => executeStatusTransition("WORK_RESUMED", "Operator resumed active work duty")}
                    className="bg-emerald-900 hover:bg-emerald-950 text-white font-bold text-xs px-5 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                  >
                    {updating ? "Updating..." : "🔄 Resume Work Duty →"}
                  </button>
                )}

                {/* 6. WORK_RESUMED -> WORK_PAUSED or WORK_COMPLETED */}
                {currentJob.status === "WORK_RESUMED" && (
                  <>
                    <button
                      type="button"
                      disabled={updating}
                      onClick={() =>
                        handleOpenConfirm(
                          "WORK_PAUSED",
                          "Pause Work Duty",
                          "Please confirm pausing the work duty for machinery check or break."
                        )
                      }
                      className="bg-amber-100 hover:bg-amber-200 text-amber-950 border border-amber-300 font-bold text-xs px-4 py-3 rounded-xl transition disabled:bg-gray-400 flex items-center gap-2"
                    >
                      {updating ? "Updating..." : "⏸️ Pause Work"}
                    </button>

                    <button
                      type="button"
                      disabled={updating}
                      onClick={() =>
                        handleOpenConfirm(
                          "WORK_COMPLETED",
                          "Complete Work Duty",
                          "Are you sure you have completed the field operations for this job? Once completed, payout processing will initiate."
                        )
                      }
                      className="bg-emerald-900 hover:bg-emerald-950 text-white font-black text-xs px-6 py-3 rounded-xl shadow transition disabled:bg-gray-400 flex items-center gap-2"
                    >
                      {updating ? "Updating..." : "🎉 Complete Work Duty →"}
                    </button>
                  </>
                )}

                {/* 7. WORK_COMPLETED */}
                {(currentJob.status === "WORK_COMPLETED" || currentJob.status === "COMPLETED") && (
                  <div className="flex items-center gap-3">
                    <span className="bg-emerald-100 text-emerald-950 font-bold text-xs px-4 py-2.5 rounded-xl border border-emerald-300">
                      🎉 Work Completed & Submitted
                    </span>
                    <Link
                      to="/operator/earnings"
                      className="bg-lime-400 hover:bg-lime-300 text-emerald-950 font-bold text-xs px-4 py-2.5 rounded-xl transition shadow"
                    >
                      Check Earnings 💰
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Milestone History Audit Trail */}
          {currentJob.milestones && currentJob.milestones.length > 0 && (
            <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-4">
              <h3 className="text-sm font-black text-emerald-950 uppercase tracking-wider">
                Milestone Audit Trail
              </h3>
              <div className="divide-y divide-amber-100/60">
                {currentJob.milestones.map((m, idx) => (
                  <div key={m.id || idx} className="py-3 flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <span className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-900 text-xs font-bold flex items-center justify-center">
                        {idx + 1}
                      </span>
                      <div>
                        <p className="text-xs font-bold text-emerald-950">
                          {formatStatusLabel(m.status)}
                        </p>
                        {m.notes && <p className="text-[11px] text-gray-500">{m.notes}</p>}
                      </div>
                    </div>
                    <span className="text-[11px] font-semibold text-gray-400">
                      {formatDateTime(m.timestamp)}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Confirmation Modal */}
      {confirmModal.isOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 md:p-8 border border-amber-100 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xl">
                  {confirmModal.targetStatus === "WORK_COMPLETED" ? "🎉" : "⚡"}
                </span>
                <h3 className="text-base font-black text-emerald-950">
                  {confirmModal.title}
                </h3>
              </div>
              <button
                type="button"
                onClick={handleCloseConfirm}
                className="text-gray-400 hover:text-gray-600 font-bold"
              >
                ✕
              </button>
            </div>

            <p className="text-xs text-gray-600 leading-relaxed">
              {confirmModal.description}
            </p>

            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-gray-500 mb-1">
                Optional Status Notes / Log
              </label>
              <textarea
                value={confirmModal.notes}
                onChange={(e) =>
                  setConfirmModal((prev) => ({ ...prev, notes: e.target.value }))
                }
                rows={2}
                placeholder="e.g. Completed 10 acres plowing with uniform depth..."
                className="w-full text-xs p-3 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-lime-400"
              />
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={handleCloseConfirm}
                disabled={updating}
                className="px-4 py-2.5 rounded-xl text-xs font-bold text-gray-600 hover:bg-gray-100 transition"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={updating}
                onClick={() =>
                  executeStatusTransition(
                    confirmModal.targetStatus,
                    confirmModal.notes
                  )
                }
                className={`px-5 py-2.5 rounded-xl text-xs font-black text-white transition shadow ${
                  confirmModal.targetStatus === "WORK_COMPLETED"
                    ? "bg-emerald-900 hover:bg-emerald-950"
                    : "bg-emerald-800 hover:bg-emerald-900"
                }`}
              >
                {updating ? "Submitting..." : "Confirm & Update"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default ActiveJob;
