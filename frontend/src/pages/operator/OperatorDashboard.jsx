import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { getAuthUser, clearAuth, isAuthenticated } from "../../utils/auth";
import { getJobsSummary } from "../../services/jobService";

function OperatorDashboard() {
  const navigate = useNavigate();
  const user = getAuthUser();
  const authenticated = isAuthenticated();

  const [summary, setSummary] = useState(null);
  const [loadingSummary, setLoadingSummary] = useState(false);

  useEffect(() => {
    if (authenticated) {
      loadSummary();
    }
  }, [authenticated]);

  const loadSummary = async () => {
    setLoadingSummary(true);
    try {
      const data = await getJobsSummary();
      setSummary(data);
    } catch (err) {
      console.log("Could not load jobs summary on dashboard:", err);
    } finally {
      setLoadingSummary(false);
    }
  };

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto pb-12">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-emerald-800 to-green-700 rounded-3xl p-6 md:p-8 text-white shadow-lg">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur-md rounded-full text-xs font-semibold mb-3 border border-white/20">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Operator Status: {user?.status || "APPROVED"}
            </div>
            <h1 className="text-2xl md:text-3xl font-black tracking-tight">
              Welcome back, {user?.fullName || "Operator"}!
            </h1>
            <p className="text-green-100 text-xs md:text-sm mt-2 max-w-xl">
              You are logged in to the Agro Rental Operator portal. Access your assigned jobs, manage machinery duty, and review payouts.
            </p>
          </div>

          <div className="flex flex-wrap gap-2.5">
            <Link
              to="/operator/jobs"
              className="bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold px-5 py-3 rounded-xl shadow-md transition flex items-center gap-1.5"
            >
              <span>🚜</span>
              <span>Assigned Jobs</span>
            </Link>
            <Link
              to="/operator/profile"
              className="bg-lime-400 hover:bg-lime-300 text-emerald-950 text-xs font-bold px-5 py-3 rounded-xl shadow-md transition flex items-center gap-1.5"
            >
              <span>👤</span>
              <span>My Profile</span>
            </Link>
            <Link
              to="/operator/documents"
              className="bg-white/10 hover:bg-white/20 text-white text-xs font-bold px-4 py-3 rounded-xl border border-white/20 backdrop-blur-sm transition flex items-center gap-1.5"
            >
              <span>🪪</span>
              <span>Documents</span>
            </Link>
            {authenticated ? (
              <button
                type="button"
                onClick={handleLogout}
                className="bg-white/10 hover:bg-red-500/30 text-white text-xs font-bold px-4 py-3 rounded-xl border border-white/20 backdrop-blur-sm transition"
              >
                Log Out
              </button>
            ) : (
              <Link
                to="/login"
                className="bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold px-5 py-3 rounded-xl transition shadow-md"
              >
                Log In
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* Module 6: Assigned Jobs Summary Section */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xl">🚜</span>
            <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
              Assigned Work Overview (Module 6)
            </h2>
          </div>
          <Link
            to="/operator/jobs"
            className="text-xs font-bold text-emerald-800 hover:text-emerald-950 underline"
          >
            View All Jobs →
          </Link>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <Link
            to="/operator/jobs"
            className="bg-white p-5 rounded-2xl border border-amber-100/70 shadow-sm hover:border-emerald-500 hover:shadow-md transition group"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                Total Assigned
              </span>
              <span className="text-base">📋</span>
            </div>
            <p className="text-2xl font-black text-emerald-950 mt-1 group-hover:text-emerald-700 transition">
              {summary?.totalAssigned !== undefined ? summary.totalAssigned : "—"}
            </p>
            <p className="text-[10px] text-gray-400 mt-1">All assigned work orders</p>
          </Link>

          <Link
            to="/operator/jobs?status=PENDING_RESPONSE"
            className="bg-white p-5 rounded-2xl border border-amber-100/70 shadow-sm hover:border-amber-500 hover:shadow-md transition group"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-amber-700 uppercase tracking-wider">
                Pending Response
              </span>
              <span className="text-base">⏳</span>
            </div>
            <p className="text-2xl font-black text-amber-700 mt-1 group-hover:scale-105 transition origin-left">
              {summary?.pendingResponse !== undefined ? summary.pendingResponse : "—"}
            </p>
            <p className="text-[10px] text-gray-400 mt-1">Awaiting your response</p>
          </Link>

          <Link
            to="/operator/jobs?status=ACCEPTED"
            className="bg-white p-5 rounded-2xl border border-amber-100/70 shadow-sm hover:border-emerald-500 hover:shadow-md transition group"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-emerald-700 uppercase tracking-wider">
                Accepted
              </span>
              <span className="text-base">✓</span>
            </div>
            <p className="text-2xl font-black text-emerald-800 mt-1 group-hover:scale-105 transition origin-left">
              {summary?.accepted !== undefined ? summary.accepted : "—"}
            </p>
            <p className="text-[10px] text-gray-400 mt-1">Confirmed deployments</p>
          </Link>

          <Link
            to="/operator/jobs?status=COMPLETED"
            className="bg-white p-5 rounded-2xl border border-amber-100/70 shadow-sm hover:border-blue-500 hover:shadow-md transition group"
          >
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-blue-700 uppercase tracking-wider">
                Completed
              </span>
              <span className="text-base">🎉</span>
            </div>
            <p className="text-2xl font-black text-blue-800 mt-1 group-hover:scale-105 transition origin-left">
              {summary?.completed !== undefined ? summary.completed : "—"}
            </p>
            <p className="text-[10px] text-gray-400 mt-1">Finished field operations</p>
          </Link>
        </div>
      </div>

      {/* Operator Details Cards */}
      {user && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Contact Details
            </span>
            <p className="text-lg font-bold text-gray-800">
              +91 {user.mobileNumber}
            </p>
            <p className="text-xs text-gray-500">{user.email}</p>
            <div className="pt-2">
              <span className="px-2.5 py-1 bg-green-100 text-green-800 text-[11px] font-bold rounded-full">
                OTP Verified ✓
              </span>
            </div>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Experience & Skills
            </span>
            <p className="text-lg font-bold text-gray-800">
              {user.experience} Years Experience
            </p>
            <p className="text-xs text-gray-500 truncate">Skills: {user.skills}</p>
            <div className="pt-2">
              <span className="px-2.5 py-1 bg-blue-100 text-blue-800 text-[11px] font-bold rounded-full">
                Documents Approved ✓
              </span>
            </div>
          </div>

          <div className="bg-white p-6 rounded-3xl border border-amber-100/70 shadow-sm space-y-2">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Duty Status
            </span>
            <p className="text-lg font-bold text-emerald-800">
              AVAILABLE FOR JOBS
            </p>
            <p className="text-xs text-gray-500">Ready for Module 6 Job Dispatch</p>
            <div className="pt-2">
              <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 text-[11px] font-bold rounded-full">
                Active Ready
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default OperatorDashboard;
