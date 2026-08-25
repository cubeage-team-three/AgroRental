import { useState, useEffect, useRef } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  CheckCircle2,
  HardHat,
  Loader2,
  Phone,
  RefreshCw,
  ShieldCheck,
  Smartphone,
  X,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';

function OperatorOtpVerification() {
  const location = useLocation();
  const navigate = useNavigate();

  // Retrieve pending registration context from router state or session storage
  const [regData, setRegData] = useState(() => {
    if (location.state?.mobileNumber) {
      return location.state;
    }
    const saved = sessionStorage.getItem('agro_pending_operator_reg');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        return null;
      }
    }
    return null;
  });

  const mobileNumber = regData?.mobileNumber || '';
  const operatorId = regData?.operatorId || null;
  const fullName = regData?.fullName || 'Operator';

  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', '']);
  const inputRefs = useRef([]);

  const [resendCooldown, setResendCooldown] = useState(30);
  const [isCooldownActive, setIsCooldownActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [devMockOtp, setDevMockOtp] = useState('');

  // 30-second cooldown timer
  useEffect(() => {
    let timer = null;
    if (isCooldownActive && resendCooldown > 0) {
      timer = setInterval(() => {
        setResendCooldown((prev) => prev - 1);
      }, 1000);
    } else if (resendCooldown === 0) {
      setIsCooldownActive(false);
    }
    return () => clearInterval(timer);
  }, [isCooldownActive, resendCooldown]);

  // Focus first input on mount
  useEffect(() => {
    if (inputRefs.current[0]) {
      inputRefs.current[0].focus();
    }
  }, []);

  const maskedMobile = mobileNumber
    ? `+91 ${'•'.repeat(Math.max(0, mobileNumber.length - 4))}${mobileNumber.slice(-4)}`
    : '+91 ••••••••••';

  const handleDigitChange = (index, value) => {
    const clean = value.replace(/\D/g, '');
    if (!clean) {
      const updated = [...otpDigits];
      updated[index] = '';
      setOtpDigits(updated);
      return;
    }

    // Single digit input
    const char = clean.slice(-1);
    const updated = [...otpDigits];
    updated[index] = char;
    setOtpDigits(updated);
    if (errorMessage) setErrorMessage('');

    // Auto advance
    if (index < 5 && char) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    if (pasted) {
      const updated = [...otpDigits];
      for (let i = 0; i < 6; i++) {
        updated[i] = pasted[i] || '';
      }
      setOtpDigits(updated);
      const focusIndex = Math.min(pasted.length, 5);
      inputRefs.current[focusIndex]?.focus();
    }
  };

  const handleResendOtp = async () => {
    if (isCooldownActive || resending || !mobileNumber) return;
    setResending(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const res = await operatorService.sendOperatorOtp(mobileNumber, 'MOBILE_VERIFICATION');
      const responseData = res?.data || res;
      setSuccessMessage('✓ A fresh 6-digit verification code has been dispatched to your mobile.');
      if (responseData?.devMockOtp) {
        setDevMockOtp(responseData.devMockOtp);
      }
      setResendCooldown(30);
      setIsCooldownActive(true);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to resend OTP. Please try again.');
    } finally {
      setResending(false);
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    const otpCode = otpDigits.join('');
    if (otpCode.length !== 6) {
      setErrorMessage('Please enter the complete 6-digit verification code.');
      return;
    }

    if (!mobileNumber) {
      setErrorMessage('Mobile number missing. Please restart registration.');
      return;
    }

    setLoading(true);

    try {
      const res = await operatorService.verifyOperatorOtp(mobileNumber, otpCode, 'MOBILE_VERIFICATION');
      const responseData = res?.data || res;

      if (responseData?.verified || res?.success) {
        setSuccessMessage('✓ Mobile number verified successfully! Proceeding to KYC upload...');

        // Update local session state
        const updatedSession = {
          ...regData,
          mobileVerified: true,
        };
        sessionStorage.setItem('agro_pending_operator_reg', JSON.stringify(updatedSession));

        setTimeout(() => {
          navigate('/register/operator/kyc', {
            state: updatedSession,
          });
        }, 1200);
      } else {
        setErrorMessage('Verification failed. Please check the OTP entered.');
      }
    } catch (err) {
      console.error('Operator OTP Verify Error:', err);
      setErrorMessage(err.message || 'Invalid verification code. Please check and try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <div className="flex items-center gap-2 text-emerald-800 text-xs font-bold uppercase tracking-wider mb-2">
          <ShieldCheck className="h-4 w-4 text-emerald-600" />
          Step 2 of 4 • Phone Verification
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Verify Your Mobile Number
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          We have sent a 6-digit authentication code to{' '}
          <strong className="text-slate-800 font-semibold">{maskedMobile}</strong>.
        </p>
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
          <div className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
            <span>{errorMessage}</span>
            <button
              type="button"
              onClick={() => setErrorMessage('')}
              className="shrink-0 text-red-400 hover:text-red-600 transition-colors"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </RevealItem>
      )}

      {successMessage && (
        <RevealItem className="mt-5">
          <div className="flex items-center gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
            <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
        </RevealItem>
      )}

      {devMockOtp && (
        <RevealItem className="mt-4">
          <div className="flex items-center justify-between gap-2 rounded-2xl border border-emerald-200 bg-emerald-50/80 px-4 py-2.5 text-xs text-emerald-900">
            <span>
              Dev Auto-Fill: <strong>{devMockOtp}</strong>
            </span>
            <button
              type="button"
              onClick={() => {
                const digits = devMockOtp.split('').slice(0, 6);
                setOtpDigits(digits);
              }}
              className="rounded-lg bg-emerald-700 px-2.5 py-1 text-[11px] font-bold text-white hover:bg-emerald-800 transition-colors"
            >
              Paste Code
            </button>
          </div>
        </RevealItem>
      )}

      <RevealItem className="mt-7">
        <form onSubmit={handleVerifyOtp} className="space-y-6">
          <div>
            <label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-400">
              Enter 6-Digit Security Code
            </label>
            <div className="flex justify-between gap-2 sm:gap-3" onPaste={handlePaste}>
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
                  className={`h-14 w-12 sm:w-14 rounded-2xl border text-center text-xl font-bold transition-all duration-200 outline-none ${
                    digit
                      ? 'border-emerald-600 bg-emerald-50/30 text-emerald-900 shadow-sm'
                      : 'border-slate-200 bg-[#F7F6F0] text-slate-900 focus:border-emerald-500 focus:bg-white focus:shadow-[0_0_0_4px_rgba(16,185,129,0.12)]'
                  }`}
                />
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Didn&apos;t receive the code?</span>
            <button
              type="button"
              disabled={isCooldownActive || resending}
              onClick={handleResendOtp}
              className="flex items-center gap-1.5 font-bold text-emerald-700 hover:text-emerald-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {resending ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  Sending...
                </>
              ) : isCooldownActive ? (
                <>
                  <RefreshCw className="h-3.5 w-3.5" />
                  Resend in {resendCooldown}s
                </>
              ) : (
                <>
                  <RefreshCw className="h-3.5 w-3.5" />
                  Resend Code
                </>
              )}
            </button>
          </div>

          <MagneticButton className="block w-full pt-2">
            <motion.button
              type="submit"
              disabled={loading || otpDigits.join('').length !== 6}
              animate={
                loading
                  ? {}
                  : {
                      boxShadow: [
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                        '0 0 38px 6px rgba(163,230,53,0.6)',
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                      ],
                    }
              }
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
              className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-60"
            >
              {loading ? (
                <>
                  <Loader2 className="h-[18px] w-[18px] animate-spin" />
                  Verifying Security Code...
                </>
              ) : (
                <>
                  Verify & Continue to KYC
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500 space-y-2">
        <div>
          Incorrect mobile number?{' '}
          <Link to="/register/operator" className="font-bold text-emerald-700 hover:underline">
            ← Change Mobile / Re-enter Details
          </Link>
        </div>
      </RevealItem>
    </RevealGroup>
  );
}

export default OperatorOtpVerification;
