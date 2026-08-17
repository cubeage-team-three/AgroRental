import { useNavigate, Link } from "react-router-dom";
import { getAuthUser, clearAuth, isAuthenticated } from "../../utils/auth";

function OperatorDashboard() {
  const navigate = useNavigate();
  const user = getAuthUser();
  const authenticated = isAuthenticated();

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-emerald-800 to-green-700 rounded-3xl p-8 text-white shadow-lg">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 backdrop-blur-md rounded-full text-xs font-semibold mb-3 border border-white/20">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              Operator Status: {user?.status || "AUTHENTICATED"}
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight">
              Welcome back, {user?.fullName || "Operator"}!
            </h1>
            <p className="text-green-100 text-sm mt-2 max-w-xl">
              You are logged in to the Agro Rental Operator portal. Access assignments, manage duty status, and monitor your earnings.
            </p>
          </div>

          <div className="flex gap-3">
            {authenticated ? (
              <button
                type="button"
                onClick={handleLogout}
                className="bg-white/10 hover:bg-white/20 text-white text-xs font-bold px-5 py-3 rounded-xl border border-white/30 backdrop-blur-sm transition"
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

      {/* Operator Details Card */}
      {user && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Contact Details
            </span>
            <p className="text-lg font-bold text-gray-800 mt-2">
              +91 {user.mobileNumber}
            </p>
            <p className="text-xs text-gray-500">{user.email}</p>
            <div className="mt-3">
              <span className="px-2.5 py-1 bg-green-100 text-green-800 text-[11px] font-bold rounded-full">
                OTP Verified ✓
              </span>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Experience & Skills
            </span>
            <p className="text-lg font-bold text-gray-800 mt-2">
              {user.experience} Years Experience
            </p>
            <p className="text-xs text-gray-500 mt-1">Skills: {user.skills}</p>
            <div className="mt-3">
              <span className="px-2.5 py-1 bg-blue-100 text-blue-800 text-[11px] font-bold rounded-full">
                Documents Approved ✓
              </span>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">
              Duty Status
            </span>
            <p className="text-lg font-bold text-emerald-800 mt-2">
              AVAILABLE FOR JOBS
            </p>
            <p className="text-xs text-gray-500">Ready for Module 5 Profile & Job Dispatch</p>
            <div className="mt-3">
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
