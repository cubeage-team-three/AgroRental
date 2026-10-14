import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Smartphone, MessageCircle, X } from 'lucide-react';
import { sendOtp } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthTextField from '../../components/auth/AuthTextField';
import AuthPasswordField from '../../components/auth/AuthPasswordField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';
import AuthModeToggle from '../../components/auth/AuthModeToggle';

const ROLES = [
  { id: 'FARMER', label: 'Farmer' },
  { id: 'PARTNER', label: 'Partner' },
  { id: 'OPERATOR', label: 'Operator' },
];

function GoogleIcon() {
  return (
    <svg className="h-[18px] w-[18px]" viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="#4285F4"
        d="M23.52 12.27c0-.85-.08-1.67-.22-2.45H12v4.64h6.47c-.28 1.5-1.13 2.77-2.4 3.62v3h3.88c2.27-2.09 3.57-5.17 3.57-8.81z"
      />
      <path
        fill="#34A853"
        d="M12 24c3.24 0 5.96-1.07 7.95-2.92l-3.88-3c-1.08.72-2.45 1.15-4.07 1.15-3.13 0-5.78-2.11-6.73-4.96H1.27v3.1C3.25 21.3 7.31 24 12 24z"
      />
      <path
        fill="#FBBC05"
        d="M5.27 14.27a7.2 7.2 0 010-4.54v-3.1H1.27a12 12 0 000 10.74l4-3.1z"
      />
      <path
        fill="#EA4335"
        d="M12 4.77c1.76 0 3.34.6 4.59 1.79l3.44-3.44C17.95 1.19 15.24 0 12 0 7.31 0 3.25 2.7 1.27 6.63l4 3.1C6.22 6.88 8.87 4.77 12 4.77z"
      />
    </svg>
  );
}

function Login({ initialRole: propRole }) {
  const { t } = useLanguage();
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Retrieve initial state from registration redirect or query parameters
  const initialMobile = location.state?.mobileNumber || location.state?.mobileOrEmail || '';
  const initialMessage = location.state?.successMessage || location.state?.message || '';

  const searchParams = new URLSearchParams(location.search);
  const paramRole = searchParams.get('role')?.toUpperCase();
  const stateRole = location.state?.role?.toUpperCase();
  const rawRole = propRole?.toUpperCase() || paramRole || stateRole || 'FARMER';
  const initialRole = ['FARMER', 'PARTNER', 'OPERATOR'].includes(rawRole)
    ? rawRole
    : 'FARMER';

  const [selectedRole, setSelectedRole] = useState(initialRole);

  useEffect(() => {
    let nextRole = 'FARMER';
    if (propRole && ['FARMER', 'PARTNER', 'OPERATOR'].includes(propRole.toUpperCase())) {
      nextRole = propRole.toUpperCase();
    } else if (paramRole) {
      if (['FARMER', 'PARTNER', 'OPERATOR'].includes(paramRole)) {
        nextRole = paramRole;
      } else {
        nextRole = 'FARMER';
      }
    } else if (stateRole && ['FARMER', 'PARTNER', 'OPERATOR'].includes(stateRole)) {
      nextRole = stateRole;
    }
    setSelectedRole(nextRole);
    if (nextRole !== 'FARMER') {
      setLoginMode('PASSWORD');
    }
  }, [propRole, paramRole, stateRole]);

  // Mode: "PASSWORD" or "OTP" (OTP available for Farmer accounts)
  const [loginMode, setLoginMode] = useState('PASSWORD');

  const [formData, setFormData] = useState({
    mobileOrEmail: initialMobile,
    password: '',
    otp: '',
  });

  const [loading, setLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [devMockOtp, setDevMockOtp] = useState('');

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState(initialMessage);
  const [infoMessage, setInfoMessage] = useState('');

  const handleRoleChange = (roleId) => {
    setSelectedRole(roleId);
    if (roleId !== 'FARMER' && loginMode === 'OTP') {
      setLoginMode('PASSWORD');
    }
    setErrorMessage('');
  };

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
        setErrorMessage('Network error: Failed to send OTP.');
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
      if (loginMode === 'OTP' && selectedRole === 'FARMER') {
        response = await auth.login({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          otp: formData.otp.trim(),
          role: selectedRole,
          loginType: 'OTP',
        });
      } else {
        response = await auth.login({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          password: formData.password,
          role: selectedRole,
          loginType: 'PASSWORD',
        });
      }

      const userData = response.data || response;
      if (userData && (userData.role === 'PARTNER' || userData.partnerId)) {
        setSuccessMessage('Partner login successful! Redirecting to Partner Portal...');
        setTimeout(() => {
          navigate('/partner/dashboard');
        }, 1200);
      } else if (userData && userData.role === 'ADMIN') {
        setSuccessMessage('Admin login successful! Redirecting to Admin Portal...');
        setTimeout(() => {
          navigate('/admin/overview');
        }, 1200);
      } else if (userData && (userData.role === 'OPERATOR' || userData.operatorId)) {
        setSuccessMessage('Operator login successful! Redirecting to Operator Portal...');
        setTimeout(() => {
          navigate('/operator/dashboard');
        }, 1200);
      } else {
        setSuccessMessage('Login successful! Redirecting to Farmer Dashboard...');
        setTimeout(() => {
          navigate('/farmer/dashboard');
        }, 1200);
      }
    } catch (err) {
      console.error('Login error:', err);

      const isNetworkError =
        !err.status ||
        err.status === 0 ||
        (err.message && err.message.toLowerCase().includes('network error'));

      const cleanMobile = formData.mobileOrEmail.replace(/\D/g, '');

      // Special rule: PENDING_OTP redirection
      if (err.message && err.message.includes('PENDING_OTP')) {
        setErrorMessage('Your account is pending OTP verification. Redirecting...');
        setTimeout(() => {
          navigate('/verify-otp', {
            state: { mobileNumber: cleanMobile },
          });
        }, 1500);
        return;
      }

      if (isNetworkError) {
        setErrorMessage(
          'Could not reach the server. Please check that the backend is running and try again.'
        );
      } else {
        setErrorMessage(err.message || 'Login failed. Please check your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  const inputLabel =
    selectedRole === 'OPERATOR'
      ? 'Mobile Number or Email'
      : selectedRole === 'PARTNER'
      ? 'Partner Mobile or Email'
      : 'Mobile Number or Email';

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Welcome back
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Sign in to manage your farms, bookings, and machinery.
        </p>
      </RevealItem>

      <RevealItem className="mt-6">
        <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
          Log In As
        </label>
        <div
          role="tablist"
          aria-label="Select role to log in as"
          className="grid grid-cols-3 gap-1 rounded-2xl bg-[#F0EFE9] p-1"
        >
          {ROLES.map((r) => {
            const isActive = selectedRole === r.id;
            return (
              <button
                key={r.id}
                id={`role-tab-${r.id.toLowerCase()}`}
                role="tab"
                aria-selected={isActive}
                type="button"
                onClick={() => handleRoleChange(r.id)}
                className={`rounded-xl py-2.5 text-xs sm:text-sm font-semibold transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-600 ${
                  isActive
                    ? 'bg-white text-emerald-800 shadow-sm font-bold'
                    : 'text-slate-500 hover:text-slate-800 font-medium'
                }`}
              >
                {r.label}
              </button>
            );
          })}
        </div>
      </RevealItem>

      {selectedRole === 'FARMER' && (
        <RevealItem className="mt-4">
          <AuthModeToggle
            activeMode={loginMode}
            onChange={(mode) => {
              setLoginMode(mode);
              setErrorMessage('');
            }}
          />
        </RevealItem>
      )}

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
            <button
              type="button"
              onClick={() => setErrorMessage('')}
              className="shrink-0 text-red-400 transition-colors hover:text-red-600"
            >
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
          <AuthTextField
            id="mobileOrEmail"
            name="mobileOrEmail"
            label={inputLabel}
            icon={Smartphone}
            type="text"
            value={formData.mobileOrEmail}
            onChange={handleInputChange}
            autoComplete="username"
            required
          />

          {loginMode === 'PASSWORD' && (
            <div>
              <AuthPasswordField
                id="password"
                name="password"
                label="Password"
                value={formData.password}
                onChange={handleInputChange}
                autoComplete="current-password"
                required
              />
              {selectedRole === 'FARMER' && (
                <div className="mt-2 flex items-center justify-between text-xs">
                  <Link
                    to={{
                      pathname: '/forgot-password',
                      search: '?role=FARMER',
                    }}
                    state={{ mobileOrEmail: formData.mobileOrEmail, role: 'FARMER' }}
                    className="font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
                  >
                    Forgot password?
                  </Link>
                  <button
                    type="button"
                    onClick={() => {
                      setLoginMode('OTP');
                      handleSendOtp();
                    }}
                    className="font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
                  >
                    Forgot password? Use OTP instead
                  </button>
                </div>
              )}
              {selectedRole === 'PARTNER' && (
                <div className="mt-2 text-right">
                  <Link
                    to={{
                      pathname: '/forgot-password',
                      search: '?role=PARTNER',
                    }}
                    state={{ mobileOrEmail: formData.mobileOrEmail, role: 'PARTNER' }}
                    className="text-xs font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
                  >
                    Forgot password?
                  </Link>
                </div>
              )}
              {selectedRole === 'OPERATOR' && (
                <div className="mt-2 text-right">
                  <Link
                    to={{
                      pathname: '/forgot-password',
                      search: '?role=OPERATOR',
                    }}
                    state={{ mobileOrEmail: formData.mobileOrEmail, role: 'OPERATOR' }}
                    className="text-xs font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
                  >
                    Forgot password?
                  </Link>
                </div>
              )}
            </div>
          )}

          {loginMode === 'OTP' && selectedRole === 'FARMER' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-600">
                  6-digit code sent to your phone
                </span>
                <button
                  type="button"
                  onClick={handleSendOtp}
                  disabled={sendingOtp}
                  className="text-xs font-bold text-emerald-700 transition-colors hover:underline disabled:opacity-50"
                >
                  {sendingOtp ? 'Sending...' : otpSent ? 'Resend OTP' : 'Send OTP'}
                </button>
              </div>
              <AuthTextField
                id="otp"
                name="otp"
                label="Enter 6-digit OTP"
                type="text"
                maxLength={6}
                value={formData.otp}
                onChange={handleInputChange}
                inputMode="numeric"
                autoComplete="one-time-code"
                className="[&_input]:text-center [&_input]:tracking-[0.4em] [&_input]:font-bold"
              />
            </div>
          )}

          <AuthPrimaryButton loading={loading} loadingText="Signing in...">
            {t('login')}
          </AuthPrimaryButton>
        </form>
      </RevealItem>

      {selectedRole === 'FARMER' && (
        <>
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
              onClick={() =>
                setInfoMessage('Google sign-in is coming soon — please use phone or OTP for now.')
              }
              className="flex min-h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-slate-300 hover:bg-slate-50 active:scale-[0.98]"
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
              className="flex min-h-[50px] items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-emerald-300 hover:bg-emerald-50 active:scale-[0.98]"
            >
              <MessageCircle className="h-[18px] w-[18px] text-[#25D366]" />
              WhatsApp
            </button>
          </RevealItem>
        </>
      )}

      <RevealItem className="mt-8 text-center text-sm text-slate-500 space-y-2">
        <div>
          Don&apos;t have an account?{' '}
          <Link
            to="/register?role=farmer"
            className="font-bold text-emerald-700 underline-offset-2 hover:underline"
          >
            Farmer
          </Link>
          {' • '}
          <Link
            to="/register?role=partner"
            className="font-bold text-emerald-700 underline-offset-2 hover:underline"
          >
            Equipment Owner
          </Link>
          {' • '}
          <Link
            to="/register?role=operator"
            className="font-bold text-emerald-700 underline-offset-2 hover:underline"
          >
            Machinery Operator
          </Link>
        </div>
        {selectedRole !== 'OPERATOR' && (
          <div>
            Machinery Operator?{' '}
            <button
              type="button"
              onClick={() => {
                setSelectedRole('OPERATOR');
                setErrorMessage('');
              }}
              className="font-bold text-emerald-800 underline-offset-2 hover:underline cursor-pointer"
            >
              Sign In to Operator Portal →
            </button>
          </div>
        )}
      </RevealItem>
    </RevealGroup>
  );
}

export default Login;
