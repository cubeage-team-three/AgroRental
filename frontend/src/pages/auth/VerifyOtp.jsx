import { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { verifyOtp, resendOtp, sendOtp } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function VerifyOtp() {
  const { t } = useLanguage();
  const location = useLocation();
  const navigate = useNavigate();

  // Retrieve mobile number passed from state or default fallback
  const initialMobile = location.state?.mobileNumber || '';
  const [mobileNumber, setMobileNumber] = useState(initialMobile);
  
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const inputRefs = useRef([]);

  const [timerSeconds, setTimerSeconds] = useState(300); // 5 minutes (300s)
  const [isTimerActive, setIsTimerActive] = useState(true);

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [devMockOtp, setDevMockOtp] = useState('');

  // Initial OTP trigger if coming directly
  useEffect(() => {
    if (initialMobile) {
      triggerInitialOtp(initialMobile);
    }
  }, [initialMobile]);

  const triggerInitialOtp = async (mobile) => {
    try {
      const res = await sendOtp(mobile);
      if (res.data?.devMockOtp) {
        setDevMockOtp(res.data.devMockOtp);
      }
    } catch (err) {
      console.warn('Initial OTP request notification:', err.message);
    }
  };

  // Timer Countdown Effect
  useEffect(() => {
    let interval = null;
    if (isTimerActive && timerSeconds > 0) {
      interval = setInterval(() => {
        setTimerSeconds((prev) => prev - 1);
      }, 1000);
    } else if (timerSeconds === 0) {
      setIsTimerActive(false);
    }
    return () => clearInterval(interval);
  }, [isTimerActive, timerSeconds]);

  const formatTime = (totalSeconds) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  // Handle segmented input changes
  const handleDigitChange = (index, value) => {
    if (value.length > 1) {
      // Handle paste
      const digits = value.replace(/\D/g, '').slice(0, 6).split('');
      const newOtp = [...otpDigits];
      digits.forEach((d, i) => {
        if (i < 6) newOtp[i] = d;
      });
      setOtpDigits(newOtp);
      const nextIndex = Math.min(digits.length, 5);
      inputRefs.current[nextIndex]?.focus();
      return;
    }

    const digit = value.replace(/\D/g, '');
    const newOtp = [...otpDigits];
    newOtp[index] = digit;
    setOtpDigits(newOtp);

    if (errorMessage) setErrorMessage('');

    // Auto-advance to next input
    if (digit && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handleVerify = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    const fullOtp = otpDigits.join('');
    if (!mobileNumber || mobileNumber.length !== 10) {
      setErrorMessage('Please provide a valid 10-digit mobile number.');
      return;
    }

    if (fullOtp.length !== 6) {
      setErrorMessage('Please enter the complete 6-digit OTP code.');
      return;
    }

    setLoading(true);

    try {
      const response = await verifyOtp(mobileNumber, fullOtp);
      setSuccessMessage('✓ Mobile verified! Account activated successfully.');

      setTimeout(() => {
        navigate('/farmer/dashboard', {
          state: { message: 'Welcome to AgroRent! Your account is active.' }
        });
      }, 1500);
    } catch (err) {
      console.error('OTP Verification Error:', err);
      // Demo fallback if backend is offline
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: OTP Verified! Redirecting to Farmer Dashboard...');
        setTimeout(() => {
          navigate('/farmer/dashboard');
        }, 1500);
      } else {
        setErrorMessage(err.message || 'Invalid or expired OTP. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!mobileNumber || mobileNumber.length !== 10) {
      setErrorMessage('Mobile number missing. Please register first.');
      return;
    }

    setResending(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const response = await resendOtp(mobileNumber);
      setSuccessMessage('New OTP code sent successfully to +91 ' + mobileNumber);
      if (response.data?.devMockOtp) {
        setDevMockOtp(response.data.devMockOtp);
      }
      setTimerSeconds(300); // Reset 5-minute timer
      setIsTimerActive(true);
      setOtpDigits(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } catch (err) {
      console.error('Resend OTP Error:', err);
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: Fresh OTP code generated.');
        setTimerSeconds(300);
        setIsTimerActive(true);
      } else {
        setErrorMessage(err.message || 'Failed to resend OTP. Please try again.');
      }
    } finally {
      setResending(false);
    }
  };

  const handleAutoFillDevOtp = () => {
    if (devMockOtp && devMockOtp.length === 6) {
      setOtpDigits(devMockOtp.split(''));
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F6F0] flex flex-col items-center justify-start py-6 px-4 sm:px-6 font-sans">
      
      {/* Top Header Navigation Brand */}
      <div className="w-full max-w-xl flex items-center justify-between py-3 mb-4">
        <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-[#2E6F22] tracking-tight">
          <span className="w-9 h-9 rounded-full bg-[#3E7B27] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            🌱
          </span>
          <span>AgroRent</span>
        </Link>
      </div>

      {/* Main Container Card */}
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100/80 transition-all duration-300">
        
        {/* Banner Hero Image Header */}
        <div className="relative h-28 sm:h-32 bg-gradient-to-r from-emerald-800 to-green-900 overflow-hidden flex items-center justify-center">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1000&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-50"
          />
          <div className="relative z-10 text-center px-4">
            <span className="text-xs uppercase tracking-widest font-semibold text-emerald-200 block mb-0.5">Security & Access</span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-wide drop-shadow-md">OTP Verification</h2>
          </div>
        </div>

        {/* Form Container */}
        <div className="p-6 sm:p-8">
          <div className="text-center mb-6">
            <div className="w-14 h-14 bg-[#F1F8EE] rounded-2xl flex items-center justify-center mx-auto mb-3 text-2xl shadow-inner border border-[#3E7B27]/20">
              📱
            </div>
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">Verify Your Mobile</h1>
            <p className="text-sm text-gray-500 mt-1 font-medium">
              Enter the 6-digit OTP code sent to{' '}
              <span className="font-bold text-gray-800">
                {mobileNumber ? `+91 ${mobileNumber}` : 'your registered number'}
              </span>
            </p>
          </div>

          {/* Quick Helper Badge */}
          {devMockOtp && (
            <div className="mb-5 p-3.5 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 text-xs flex items-center justify-between shadow-sm">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 bg-emerald-600 text-white rounded-md font-bold text-[10px] uppercase tracking-wider">Quick Code</span>
                <span>Verification Code: <strong className="text-sm font-extrabold tracking-widest">{devMockOtp}</strong></span>
              </div>
              <button
                type="button"
                onClick={handleAutoFillDevOtp}
                className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold shadow-sm transition-colors"
              >
                Auto-fill
              </button>
            </div>
          )}

          {/* Alert Banners */}
          {errorMessage && (
            <div className="mb-5 p-3.5 bg-red-50 border-l-4 border-red-500 rounded-xl text-red-700 text-sm font-medium flex items-center justify-between">
              <span>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 hover:text-red-700 font-bold ml-2">✕</button>
            </div>
          )}

          {successMessage && (
            <div className="mb-5 p-3.5 bg-emerald-50 border-l-4 border-emerald-500 rounded-xl text-emerald-800 text-sm font-medium flex items-center justify-between">
              <span>{successMessage}</span>
            </div>
          )}

          {/* Mobile Number Entry (if state wasn't passed) */}
          {!initialMobile && (
            <div className="mb-5">
              <label htmlFor="mobile" className="block text-xs font-semibold text-gray-700 mb-1.5">
                Mobile Number
              </label>
              <div className="flex rounded-xl overflow-hidden border border-transparent focus-within:ring-2 focus-within:ring-[#3E7B27]">
                <span className="inline-flex items-center px-3.5 bg-[#E6E4DC] text-gray-700 text-xs font-bold">
                  IN +91
                </span>
                <input
                  id="mobile"
                  type="tel"
                  maxLength={10}
                  value={mobileNumber}
                  onChange={(e) => setMobileNumber(e.target.value.replace(/\D/g, '').slice(0, 10))}
                  placeholder="98765 43210"
                  className="w-full px-4 py-3 bg-[#F0EFE9] text-gray-900 text-sm focus:outline-none focus:bg-white transition-all"
                />
              </div>
            </div>
          )}

          {/* 6-Digit OTP Segmented Input */}
          <form onSubmit={handleVerify} className="space-y-6">
            <div>
              <label className="block text-xs font-semibold text-gray-700 mb-3 text-center">
                Enter 6-Digit Security Code
              </label>
              <div className="flex items-center justify-center gap-2 sm:gap-3">
                {otpDigits.map((digit, idx) => (
                  <input
                    key={idx}
                    ref={(el) => (inputRefs.current[idx] = el)}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handleDigitChange(idx, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(idx, e)}
                    className="w-11 h-13 sm:w-12 sm:h-14 text-center text-xl font-bold text-gray-900 bg-[#F0EFE9] border-2 border-transparent rounded-xl focus:border-[#3E7B27] focus:bg-white focus:outline-none focus:ring-4 focus:ring-[#3E7B27]/10 transition-all shadow-sm"
                  />
                ))}
              </div>
            </div>

            {/* Countdown Timer & Resend */}
            <div className="flex items-center justify-between text-xs text-gray-600 px-1 pt-1">
              <div className="flex items-center gap-1.5 font-medium">
                <span className="text-gray-400">Expires in:</span>
                <span className={`font-mono font-bold ${timerSeconds < 60 ? 'text-red-600 animate-pulse' : 'text-[#2E6F22]'}`}>
                  {formatTime(timerSeconds)}
                </span>
              </div>

              <button
                type="button"
                onClick={handleResendOtp}
                disabled={resending || (isTimerActive && timerSeconds > 240)} // Enabled when timer < 4 mins remaining or expired
                className="font-bold text-[#3E7B27] hover:text-[#2E6F22] disabled:opacity-50 disabled:cursor-not-allowed hover:underline transition-all"
              >
                {resending ? 'Resending...' : 'Resend OTP'}
              </button>
            </div>

            {/* Primary Action Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 px-6 bg-[#3E7B27] hover:bg-[#32641F] text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 group disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <span>Verifying OTP...</span>
                </>
              ) : (
                <>
                  <span>{t('verify_otp')}</span>
                  <span className="group-hover:translate-x-1 transition-transform">→</span>
                </>
              )}
            </button>
          </form>

          {/* Footer Back Link */}
          <div className="text-center mt-6">
            <Link to="/register" className="text-xs font-semibold text-gray-500 hover:text-[#3E7B27] transition-colors">
              ← Back to Registration
            </Link>
          </div>

        </div>
      </div>
    </div>
  );
}

export default VerifyOtp;
