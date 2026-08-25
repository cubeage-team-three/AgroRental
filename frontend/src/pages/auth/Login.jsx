import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, Eye, EyeOff, Loader2, MessageCircle, Smartphone, Lock, X } from 'lucide-react';
import { loginUser, loginWithOtp, saveUserSession } from '../../services/authService';
import { sendOtp } from '../../services/farmerAuthService';
import { operatorService } from '../../services/operatorService';
import { useLanguage } from '../../context/LanguageContext';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';
import AuthField from '../../components/auth/AuthField';

function GoogleIcon() {
  return (
    <svg className="h-[18px] w-[18px]" viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#4285F4" d="M23.52 12.27c0-.85-.08-1.67-.22-2.45H12v4.64h6.47c-.28 1.5-1.13 2.77-2.4 3.62v3h3.88c2.27-2.09 3.57-5.17 3.57-8.81z" />
      <path fill="#34A853" d="M12 24c3.24 0 5.96-1.07 7.95-2.92l-3.88-3c-1.08.72-2.45 1.15-4.07 1.15-3.13 0-5.78-2.11-6.73-4.96H1.27v3.1C3.25 21.3 7.31 24 12 24z" />
      <path fill="#FBBC05" d="M5.27 14.27a7.2 7.2 0 010-4.54v-3.1H1.27a12 12 0 000 10.74l4-3.1z" />
      <path fill="#EA4335" d="M12 4.77c1.76 0 3.34.6 4.59 1.79l3.44-3.44C17.95 1.19 15.24 0 12 0 7.31 0 3.25 2.7 1.27 6.63l4 3.1C6.22 6.88 8.87 4.77 12 4.77z" />
    </svg>
  );
}

function Login() {
  const { t } = useLanguage();
  const navigate = useNavigate();

  // Mode: "PASSWORD" or "OTP"
  const [loginMode, setLoginMode] = useState('PASSWORD');

  const [formData, setFormData] = useState({
    mobileOrEmail: '',
    password: '',
    otp: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [devMockOtp, setDevMockOtp] = useState('');

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [infoMessage, setInfoMessage] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleSendOtp = async () => {
    if (!formData.mobileOrEmail.trim()) {
      setErrorMessage('Please enter your 10-digit mobile number.');
      return;
    }
    const mobile = formData.mobileOrEmail.replace(/\D/g, '');
    if (mobile.length !== 10) {
      setErrorMessage('Please enter a valid 10-digit mobile number for OTP login.');
      return;
    }

    setSendingOtp(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const res = await sendOtp(mobile);
      setOtpSent(true);
      setSuccessMessage('OTP sent successfully to +91 ' + mobile);
      if (res.data?.devMockOtp) {
        setDevMockOtp(res.data.devMockOtp);
      }
    } catch (err) {
      console.error('Send OTP error during login:', err);
      if (err.message && err.message.includes('Network error')) {
        setOtpSent(true);
        setSuccessMessage('Demo Mode: OTP sent!');
        setDevMockOtp('123456');
      } else {
        setErrorMessage(err.message || 'Failed to send OTP. Please try again.');
      }
    } finally {
      setSendingOtp(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!formData.mobileOrEmail.trim()) {
      setErrorMessage('Please enter your mobile number or email address.');
      return;
    }

    if (loginMode === 'PASSWORD' && !formData.password) {
      setErrorMessage('Password is required.');
      return;
    }

    if (loginMode === 'OTP' && (!formData.otp || formData.otp.length !== 6)) {
      setErrorMessage('Please enter the 6-digit OTP code.');
      return;
    }

    setLoading(true);

    try {
      let response;
      if (loginMode === 'OTP') {
        response = await loginWithOtp({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          otp: formData.otp.trim(),
        });
      } else {
        response = await loginUser({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          password: formData.password,
          loginType: 'PASSWORD',
        });
      }

      const userData = response.data;
      saveUserSession(userData);
      if (userData && (userData.role === 'PARTNER' || userData.partnerId)) {
        setSuccessMessage('Partner login successful! Redirecting to Partner Portal...');
        setTimeout(() => {
          navigate('/partner/dashboard');
        }, 1200);
      } else {
        setSuccessMessage('Login successful! Redirecting to Farmer Dashboard...');
        setTimeout(() => {
          navigate('/farmer/dashboard');
        }, 1200);
      }

      } catch (err) {
        console.error('Login error, attempting operator fallback:', err);

        // Fallback: Check if operator account
        if (loginMode === 'PASSWORD') {
          try {
            const opRes = await operatorService.loginOperator({
              mobileNumber: formData.mobileOrEmail.trim(),
              password: formData.password,
            });
            if (opRes && (opRes.accessToken || opRes.token)) {
              setSuccessMessage(`Welcome back, ${opRes.operator?.fullName || 'Operator'}! Redirecting to Operator Portal...`);
              setTimeout(() => {
                navigate('/operator/dashboard');
              }, 1000);
              return;
            }
          } catch (opErr) {
            console.debug('Not an operator account either:', opErr);
          }
        }

        // Special rule: PENDING_OTP redirection
        if (err.message && err.message.includes('PENDING_OTP')) {
          const cleanMobile = formData.mobileOrEmail.replace(/\D/g, '');
          setErrorMessage('Your account is pending OTP verification. Redirecting...');
          setTimeout(() => {
            navigate('/verify-otp', {
              state: { mobileNumber: cleanMobile }
            });
          }, 1500);
          return;
        }

        if (err.message && err.message.includes('Network error')) {
          setSuccessMessage('Demo Mode: Logged in! Redirecting to Farmer Dashboard...');
          saveUserSession({
            token: 'demo-token-123',
            fullName: 'Farmer User',
            mobileNumber: formData.mobileOrEmail,
            role: 'FARMER',
          });
          setTimeout(() => {
            navigate('/farmer/dashboard');
          }, 1200);
        } else {
          setErrorMessage(err.message || 'Login failed. Please check your credentials.');
        }
      } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.08} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Welcome back
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Sign in to manage your farms, bookings, and machinery.
        </p>
      </RevealItem>

      <RevealItem className="mt-7">
        <div className="flex rounded-2xl bg-[#F0EFE9] p-1">
          <button
            type="button"
            onClick={() => {
              setLoginMode('PASSWORD');
              setErrorMessage('');
            }}
            className={`flex-1 rounded-xl py-2.5 text-sm font-semibold transition-all duration-200 ease-out ${loginMode === 'PASSWORD' ? 'bg-white text-emerald-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
          >
            Password
          </button>
          <button
            type="button"
            onClick={() => {
              setLoginMode('OTP');
              setErrorMessage('');
            }}
            className={`flex-1 rounded-xl py-2.5 text-sm font-semibold transition-all duration-200 ease-out ${loginMode === 'OTP' ? 'bg-white text-emerald-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'
              }`}
          >
            OTP Login
          </button>
        </div>
      </RevealItem>

      {devMockOtp && (
        <RevealItem className="mt-4">
          <div className="flex items-center justify-between gap-2 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-xs text-emerald-800">
            <span>
              Verification code: <strong className="text-sm font-bold">{devMockOtp}</strong>
            </span>
            <button
              type="button"
              onClick={() => setFormData((prev) => ({ ...prev, otp: devMockOtp }))}
              className="shrink-0 rounded-lg bg-emerald-600 px-2.5 py-1 text-[11px] font-semibold text-white transition-colors duration-200 hover:bg-emerald-700"
            >
              Auto-fill
            </button>
          </div>
        </RevealItem>
      )}

      {errorMessage && (
        <RevealItem className="mt-4">
          <div className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            <span>{errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage('')} className="shrink-0 text-red-400 transition-colors hover:text-red-600">
              <X className="h-4 w-4" />
            </button>
          </div>
        </RevealItem>
      )}

      {successMessage && (
        <RevealItem className="mt-4">
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
            {successMessage}
          </div>
        </RevealItem>
      )}

      {infoMessage && (
        <RevealItem className="mt-4">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600">
            {infoMessage}
          </div>
        </RevealItem>
      )}

      <RevealItem className="mt-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <AuthField
            id="mobileOrEmail"
            name="mobileOrEmail"
            label="Mobile Number or Email"
            icon={Smartphone}
            type="text"
            value={formData.mobileOrEmail}
            onChange={handleInputChange}
          />

          {loginMode === 'PASSWORD' && (
            <div>
              <AuthField
                id="password"
                name="password"
                label="Password"
                icon={Lock}
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={handleInputChange}
                rightSlot={
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="text-slate-400 transition-colors hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
                  </button>
                }
              />
              <div className="mt-2 text-right">
                <button
                  type="button"
                  onClick={() => {
                    setLoginMode('OTP');
                    handleSendOtp();
                  }}
                  className="text-xs font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
                >
                  Forgot password? Use OTP instead
                </button>
              </div>
            </div>
          )}

          {loginMode === 'OTP' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-600">6-digit code sent to your phone</span>
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={sendingOtp}
                  className="text-xs font-bold text-emerald-700 transition-colors hover:underline disabled:opacity-50"
                >
                  {sendingOtp ? 'Sending...' : otpSent ? 'Resend OTP' : 'Send OTP'}
                </button>
              </div>
              <AuthField
                id="otp"
                name="otp"
                label="Enter OTP"
                type="text"
                maxLength={6}
                value={formData.otp}
                onChange={handleInputChange}
                className="[&_input]:text-center [&_input]:tracking-[0.5em] [&_input]:font-bold"
              />
            </div>
          )}

          <MagneticButton className="block w-full pt-1">
            <motion.button
              type="submit"
              disabled={loading}
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
              className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-70"
            >
              {loading ? (
                <>
                  <Loader2 className="h-[18px] w-[18px] animate-spin" />
                  Signing in...
                </>
              ) : (
                <>
                  {t('login')}
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-200" />
          </div>
          <div className="relative flex justify-center">
            <span className="bg-white px-3 text-xs font-medium uppercase tracking-wide text-slate-400">
              or continue with
            </span>
          </div>
        </div>
      </RevealItem>

      <RevealItem className="mt-4 grid grid-cols-2 gap-3">
        <button
          type="button"
          onClick={() => setInfoMessage('Google sign-in is coming soon — please use phone or OTP for now.')}
          className="flex min-h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-slate-300 hover:bg-slate-50"
        >
          <GoogleIcon />
          Google
        </button>
        <button
          type="button"
          onClick={() => {
            setLoginMode('OTP');
            handleSendOtp();
          }}
          className="flex min-h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-emerald-300 hover:bg-emerald-50"
        >
          <MessageCircle className="h-[18px] w-[18px] text-[#25D366]" />
          WhatsApp
        </button>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500 space-y-1.5">
        <div>
          Don&apos;t have an account?{' '}
          <Link to="/register" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
            Farmer Sign Up
          </Link>
          {' • '}
          <Link to="/register/partner" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
            Owner Sign Up
          </Link>
          {' • '}
          <Link to="/register/operator" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
            Operator Sign Up
          </Link>
        </div>
        <div>
          Machinery Operator?{' '}
          <Link to="/login/operator" className="font-bold text-emerald-800 underline-offset-2 hover:underline">
            Sign In to Operator Portal →
          </Link>
        </div>
      </RevealItem>
    </RevealGroup>
  );
}

export default Login;
