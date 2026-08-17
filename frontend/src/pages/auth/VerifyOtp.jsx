import { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate, Link } from "react-router-dom";

function VerifyOtp() {
  const location = useLocation();
  const navigate = useNavigate();

  const initialMobile =
    location.state?.mobileNumber ||
    new URLSearchParams(location.search).get("mobileNumber") ||
    "";

  const [mobileNumber, setMobileNumber] = useState(initialMobile);
  const [isEditingMobile, setIsEditingMobile] = useState(!initialMobile);
  const [otpDigits, setOtpDigits] = useState(["", "", "", "", "", ""]);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [cooldown, setCooldown] = useState(60);
  const [isVerified, setIsVerified] = useState(false);

  const inputRefs = useRef([]);
  const hasAutoSentRef = useRef(false);

  // Countdown timer for resend cooldown
  useEffect(() => {
    let timer;
    if (cooldown > 0 && !isVerified) {
      timer = setInterval(() => {
        setCooldown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [cooldown, isVerified]);

  // Auto-send OTP when arriving from registration
  useEffect(() => {
    if (initialMobile && !hasAutoSentRef.current) {
      hasAutoSentRef.current = true;
      handleSendOtp(initialMobile);
    }
  }, [initialMobile]);

  const handleSendOtp = async (mobile) => {
    const targetMobile = mobile || mobileNumber;
    if (!targetMobile || !/^[6-9][0-9]{9}$/.test(targetMobile)) {
      setError("Please enter a valid 10-digit Indian mobile number");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/operators/otp/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mobileNumber: targetMobile }),
      });

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "Failed to send OTP");
      }

      setMessage("OTP sent successfully to " + targetMobile);
      setCooldown(60);
      setIsEditingMobile(false);
      // Focus first OTP input
      setTimeout(() => inputRefs.current[0]?.focus(), 100);
    } catch (err) {
      setError(err.message || "Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (cooldown > 0 || resending) return;

    setResending(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/operators/otp/resend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ mobileNumber }),
      });

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "Failed to resend OTP");
      }

      setMessage("OTP resent successfully!");
      setCooldown(60);
      setOtpDigits(["", "", "", "", "", ""]);
      inputRefs.current[0]?.focus();
    } catch (err) {
      setError(err.message || "Failed to resend OTP");
    } finally {
      setResending(false);
    }
  };

  const handleOtpChange = (index, value) => {
    // Only allow numeric input
    if (value && !/^[0-9]$/.test(value)) return;

    const newDigits = [...otpDigits];
    newDigits[index] = value;
    setOtpDigits(newDigits);

    // Auto focus next input
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    // Backspace: clear and focus previous
    if (e.key === "Backspace" && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pastedData = e.clipboardData.getData("text").trim();
    if (/^[0-9]{6}$/.test(pastedData)) {
      const digits = pastedData.split("");
      setOtpDigits(digits);
      inputRefs.current[5]?.focus();
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    const fullOtp = otpDigits.join("");

    if (fullOtp.length !== 6) {
      setError("Please enter the complete 6-digit OTP");
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch("http://localhost:8080/api/operators/otp/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          mobileNumber,
          otp: fullOtp,
        }),
      });

      const result = await response.json();

      if (!response.ok || !result.success) {
        throw new Error(result.message || "OTP verification failed");
      }

      setIsVerified(true);
      setMessage("Mobile number verified successfully!");
    } catch (err) {
      setError(err.message || "Invalid or expired OTP");
    } finally {
      setLoading(false);
    }
  };

  const isOtpComplete = otpDigits.every((digit) => digit !== "");

  return (
    <div className="min-h-screen bg-gray-100 py-12 px-4 flex items-center justify-center">
      <div className="max-w-md w-full bg-white shadow-xl rounded-2xl p-8 border border-gray-100">
        {/* Header */}
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-green-100 text-green-700 rounded-full flex items-center justify-center mx-auto mb-4 text-2xl font-bold">
            {isVerified ? "✓" : "📱"}
          </div>
          <h1 className="text-2xl font-bold text-gray-800">
            {isVerified ? "Mobile Number Verified" : "OTP Verification"}
          </h1>
          <p className="text-sm text-gray-500 mt-2">
            {isVerified
              ? "Your mobile number has been successfully verified."
              : "We have dispatched a 6-digit verification code to your mobile number."}
          </p>
        </div>

        {/* Success Banner */}
        {message && (
          <div className="mb-6 p-4 rounded-xl bg-green-50 border border-green-200 text-green-800 text-sm flex items-start gap-3">
            <span className="text-green-600 font-bold">✓</span>
            <div>{message}</div>
          </div>
        )}

        {/* Error Banner */}
        {error && (
          <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-sm flex items-start gap-3">
            <span className="text-red-600 font-bold">⚠️</span>
            <div>{error}</div>
          </div>
        )}

        {/* Verified State View */}
        {isVerified ? (
          <div className="space-y-6 text-center">
            <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
              <p className="text-xs text-gray-500 uppercase tracking-wider font-semibold">
                Verified Mobile
              </p>
              <p className="text-lg font-bold text-gray-800 mt-1">
                +91 {mobileNumber}
              </p>
              <span className="inline-block mt-2 px-3 py-1 bg-green-100 text-green-800 text-xs font-semibold rounded-full">
                Status: Mobile Verified (Pending Admin Approval)
              </span>
            </div>

            <button
              onClick={() => navigate("/operator/documents", { state: { mobileNumber } })}
              className="w-full bg-green-700 hover:bg-green-800 text-white font-bold py-3.5 px-6 rounded-xl shadow-lg hover:shadow-xl transition-all duration-200"
            >
              Proceed to Document Verification (Module 3) →
            </button>

            <Link
              to="/login"
              className="block text-sm text-gray-500 hover:text-green-700 transition"
            >
              Back to Home / Login
            </Link>
          </div>
        ) : (
          /* OTP Entry Form */
          <div>
            {/* Mobile Number Display / Change */}
            <div className="mb-6 p-4 bg-gray-50 rounded-xl border border-gray-200">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-xs text-gray-500 uppercase font-semibold">
                    Mobile Number
                  </span>
                  <p className="text-base font-bold text-gray-800">
                    {mobileNumber ? `+91 ${mobileNumber}` : "Not specified"}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setIsEditingMobile(!isEditingMobile)}
                  className="text-xs text-green-700 hover:text-green-900 font-semibold underline"
                >
                  {isEditingMobile ? "Cancel" : "Change"}
                </button>
              </div>

              {isEditingMobile && (
                <div className="mt-3 pt-3 border-t border-gray-200">
                  <div className="flex gap-2">
                    <input
                      type="tel"
                      value={mobileNumber}
                      onChange={(e) => setMobileNumber(e.target.value)}
                      placeholder="Enter 10-digit mobile"
                      maxLength="10"
                      className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-500"
                    />
                    <button
                      type="button"
                      onClick={() => handleSendOtp(mobileNumber)}
                      disabled={loading}
                      className="bg-green-700 hover:bg-green-800 text-white px-4 py-2 text-xs font-bold rounded-lg transition disabled:bg-gray-400"
                    >
                      {loading ? "Sending..." : "Send OTP"}
                    </button>
                  </div>
                </div>
              )}
            </div>

            <form onSubmit={handleVerify} className="space-y-6">
              {/* 6-Digit Boxes */}
              <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase tracking-wider text-center mb-3">
                  Enter 6-Digit Verification Code
                </label>
                <div className="flex justify-between gap-2" onPaste={handlePaste}>
                  {otpDigits.map((digit, index) => (
                    <input
                      key={index}
                      ref={(el) => (inputRefs.current[index] = el)}
                      type="text"
                      inputMode="numeric"
                      maxLength="1"
                      value={digit}
                      onChange={(e) => handleOtpChange(index, e.target.value)}
                      onKeyDown={(e) => handleKeyDown(index, e)}
                      className="w-12 h-14 text-center text-xl font-bold border-2 border-gray-200 rounded-xl focus:border-green-600 focus:ring-2 focus:ring-green-100 outline-none transition-all"
                    />
                  ))}
                </div>
              </div>

              {/* Verify Button */}
              <button
                type="submit"
                disabled={!isOtpComplete || loading}
                className="w-full bg-green-700 hover:bg-green-800 text-white font-bold py-3.5 px-6 rounded-xl shadow-md hover:shadow-lg transition-all duration-200 disabled:bg-gray-300 disabled:cursor-not-allowed"
              >
                {loading ? "Verifying..." : "Verify OTP"}
              </button>

              {/* Resend Cooldown Section */}
              <div className="text-center pt-2">
                <p className="text-xs text-gray-500">
                  Didn't receive the code?
                </p>
                {cooldown > 0 ? (
                  <p className="text-xs text-gray-400 font-medium mt-1">
                    Resend OTP available in{" "}
                    <span className="text-green-700 font-bold">{cooldown}s</span>
                  </p>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={resending}
                    className="mt-1 text-sm text-green-700 hover:text-green-900 font-bold underline transition disabled:opacity-50"
                  >
                    {resending ? "Resending OTP..." : "Resend OTP Now"}
                  </button>
                )}
              </div>

              {/* Back to registration link */}
              <div className="text-center border-t border-gray-100 pt-4">
                <Link
                  to="/register/operator"
                  className="text-xs text-gray-500 hover:text-gray-700 transition"
                >
                  ← Back to Operator Registration
                </Link>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}

export default VerifyOtp;
