import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { saveAuth } from "../../utils/auth";

function Login() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    identifier: "",
    password: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!formData.identifier.trim()) {
      setError("Please enter your registered mobile number or email");
      return;
    }

    if (!formData.password) {
      setError("Please enter your password");
      return;
    }

    setLoading(true);
    setError("");
    setSuccessMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/operators/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          identifier: formData.identifier.trim(),
          password: formData.password,
        }),
      });

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "Login failed. Please check your credentials.");
      }

      const { token, operator } = result.data;

      // Save auth token and user profile
      saveAuth(token, operator);

      setSuccessMessage(`Welcome back, ${operator.fullName}! Logging in...`);

      setTimeout(() => {
        navigate("/operator/dashboard");
      }, 1000);
    } catch (err) {
      setError(err.message || "Invalid credentials or network error");
    } finally {
      setLoading(false);
    }
  };

  const isPendingDocError = error.toLowerCase().includes("document") || error.toLowerCase().includes("review");
  const isOtpError = error.toLowerCase().includes("otp") || error.toLowerCase().includes("mobile number is not verified");

  return (
    <div className="min-h-screen bg-[#FDFBF7] py-12 px-4 flex items-center justify-center">
      <div className="max-w-md w-full">
        {/* Brand Card */}
        <div className="bg-white rounded-3xl shadow-xl p-8 border border-amber-100/60">
          {/* Top Logo & Title */}
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-800 font-extrabold text-3xl mb-3 shadow-inner">
              🚜
            </div>
            <h1 className="text-2xl font-black text-emerald-950 tracking-tight">
              Agro Rental Platform
            </h1>
            <p className="text-xs font-semibold text-emerald-800 uppercase tracking-widest mt-1">
              Operator Portal
            </p>
          </div>

          {/* Role Pill Navigation */}
          <div className="flex bg-amber-50/70 p-1 rounded-xl mb-6 border border-amber-200/50">
            <button
              type="button"
              className="flex-1 py-2 text-xs font-bold text-emerald-900 bg-white shadow-sm rounded-lg border border-amber-200/60 transition"
            >
              Operator Login
            </button>
            <Link
              to="/register"
              className="flex-1 text-center py-2 text-xs font-semibold text-emerald-900/70 hover:text-emerald-950 transition"
            >
              Partner / Farmer
            </Link>
          </div>

          {/* Success Banner */}
          {successMessage && (
            <div className="mb-6 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-900 text-sm flex items-center gap-3">
              <span className="text-lg">✓</span>
              <span className="font-semibold">{successMessage}</span>
            </div>
          )}

          {/* Error Banner */}
          {error && (
            <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-900 text-sm space-y-2">
              <div className="flex items-start gap-2">
                <span className="text-base">⚠️</span>
                <span className="font-semibold">{error}</span>
              </div>

              {/* Actionable shortcuts for onboarding gates */}
              {isOtpError && (
                <div className="pt-2 border-t border-red-200/60">
                  <Link
                    to="/verify-otp"
                    state={{ mobileNumber: formData.identifier }}
                    className="text-xs font-bold text-red-800 underline hover:text-red-950"
                  >
                    Go to OTP Verification Page →
                  </Link>
                </div>
              )}

              {isPendingDocError && (
                <div className="pt-2 border-t border-red-200/60">
                  <Link
                    to="/operator/documents"
                    state={{ mobileNumber: formData.identifier }}
                    className="text-xs font-bold text-red-800 underline hover:text-red-950"
                  >
                    Check / Upload Documents (Module 3) →
                  </Link>
                </div>
              )}
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Mobile / Email Identifier */}
            <div>
              <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                Mobile Number or Email
              </label>
              <input
                type="text"
                name="identifier"
                value={formData.identifier}
                onChange={handleChange}
                placeholder="e.g. 9822334455 or name@agro.com"
                required
                className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-3 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
              />
            </div>

            {/* Password */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider">
                  Password
                </label>
              </div>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="Enter your password"
                  required
                  className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-3 pr-12 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-emerald-800 text-xs font-semibold"
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>

            {/* Primary Orange Login Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white font-bold py-3.5 px-6 rounded-xl shadow-md hover:shadow-lg transition-all duration-200 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <svg
                    className="animate-spin h-5 w-5 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    ></circle>
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    ></path>
                  </svg>
                  <span>Authenticating...</span>
                </>
              ) : (
                <span>Log In to Operator Portal →</span>
              )}
            </button>
          </form>

          {/* Registration Redirect */}
          <div className="mt-8 pt-6 border-t border-amber-100 text-center">
            <p className="text-xs text-gray-500">
              New to Agro Rental Platform?{" "}
              <Link
                to="/register/operator"
                className="font-bold text-emerald-800 hover:text-emerald-950 underline"
              >
                Register as an Operator
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
