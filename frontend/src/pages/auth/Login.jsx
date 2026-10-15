import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Smartphone, X } from 'lucide-react';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthTextField from '../../components/auth/AuthTextField';
import AuthPasswordField from '../../components/auth/AuthPasswordField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';

const ROLES = [
  { id: 'FARMER', label: 'Farmer' },
  { id: 'PARTNER', label: 'Partner' },
  { id: 'OPERATOR', label: 'Operator' },
];

const ROLE_IDS = ROLES.map((r) => r.id);

const HOME_BY_ROLE = {
  FARMER: '/farmer/dashboard',
  PARTNER: '/partner/dashboard',
  OPERATOR: '/operator/dashboard',
  ADMIN: '/admin/overview',
};

const SESSION_EXPIRED_MESSAGE = 'Your session has expired. Please sign in again.';

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

function resolveRole(propRole, paramRole, stateRole) {
  const candidate = [propRole, paramRole, stateRole]
    .map((r) => r?.toUpperCase())
    .find((r) => r && ROLE_IDS.includes(r));
  return candidate || 'FARMER';
}

/** Where to go after login: back to the protected page that sent us here, but only within the same role's area. */
function destinationFor(role, from) {
  const home = HOME_BY_ROLE[role] || HOME_BY_ROLE.FARMER;
  const fromPath = from?.pathname;
  if (fromPath && fromPath.startsWith(home.substring(0, home.indexOf('/', 1) + 1))) {
    return `${fromPath}${from.search || ''}`;
  }
  return home;
}

function Login({ initialRole: propRole }) {
  const { t } = useLanguage();
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Initial state from a registration redirect, a protected-route redirect, or query parameters
  const initialIdentifier = location.state?.mobileNumber || location.state?.mobileOrEmail || '';
  const initialMessage = location.state?.successMessage || location.state?.message || '';

  const searchParams = new URLSearchParams(location.search);
  const paramRole = searchParams.get('role');
  const stateRole = location.state?.role;

  const [selectedRole, setSelectedRole] = useState(() => resolveRole(propRole, paramRole, stateRole));

  useEffect(() => {
    setSelectedRole(resolveRole(propRole, paramRole, stateRole));
  }, [propRole, paramRole, stateRole]);

  const [formData, setFormData] = useState({
    mobileOrEmail: initialIdentifier,
    password: '',
  });

  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState(initialMessage);
  const [infoMessage, setInfoMessage] = useState(
    location.state?.sessionExpired ? SESSION_EXPIRED_MESSAGE : ''
  );

  const handleRoleChange = (roleId) => {
    setSelectedRole(roleId);
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    setInfoMessage('');

    if (!formData.mobileOrEmail.trim()) {
      setErrorMessage('Please enter your mobile number or email address.');
      return;
    }

    if (!formData.password) {
      setErrorMessage('Password is required.');
      return;
    }

    setLoading(true);

    try {
      const response = await auth.login({
        mobileOrEmail: formData.mobileOrEmail.trim(),
        password: formData.password,
        role: selectedRole,
        loginType: 'PASSWORD',
      });

      const userData = response.data || response;
      const role = userData?.role || selectedRole;
      const labels = {
        PARTNER: 'Partner login successful! Redirecting to Partner Portal...',
        ADMIN: 'Admin login successful! Redirecting to Admin Portal...',
        OPERATOR: 'Operator login successful! Redirecting to Operator Portal...',
      };
      setSuccessMessage(labels[role] || 'Login successful! Redirecting to Farmer Dashboard...');
      const destination = destinationFor(role, location.state?.from);
      setTimeout(() => {
        navigate(destination, { replace: true });
      }, 1200);
    } catch (err) {
      console.error('Login error:', err);

      const isNetworkError =
        !err.status ||
        err.status === 0 ||
        (err.message && err.message.toLowerCase().includes('network error'));

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

  const inputLabel = selectedRole === 'PARTNER' ? 'Partner Mobile or Email' : 'Mobile Number or Email';

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Welcome back
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Sign in with your mobile number or email and your password.
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

      {errorMessage && (
        <RevealItem className="mt-4">
          <div
            role="alert"
            className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700"
          >
            <span>{errorMessage}</span>
            <button
              type="button"
              onClick={() => setErrorMessage('')}
              aria-label="Dismiss error"
              className="shrink-0 text-red-400 transition-colors hover:text-red-600"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </RevealItem>
      )}

      {successMessage && (
        <RevealItem className="mt-4">
          <div
            role="status"
            className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800"
          >
            {successMessage}
          </div>
        </RevealItem>
      )}

      {infoMessage && (
        <RevealItem className="mt-4">
          <div
            role="status"
            className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-600"
          >
            {infoMessage}
          </div>
        </RevealItem>
      )}

      <RevealItem className="mt-6">
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
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
            <div className="mt-2 text-right">
              <Link
                to={{
                  pathname: '/forgot-password',
                  search: `?role=${selectedRole}`,
                }}
                state={{ identifier: formData.mobileOrEmail, role: selectedRole }}
                className="text-xs font-semibold text-emerald-700 underline-offset-2 transition-colors duration-200 hover:text-emerald-800 hover:underline"
              >
                Forgot password?
              </Link>
            </div>
          </div>

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

          <RevealItem className="mt-4">
            <button
              type="button"
              onClick={() =>
                setInfoMessage(
                  'Google sign-in is coming soon. Please sign in with your mobile number or email and password.'
                )
              }
              className="flex min-h-[50px] w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-slate-300 hover:bg-slate-50 active:scale-[0.98]"
            >
              <GoogleIcon />
              Google
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
              onClick={() => handleRoleChange('OPERATOR')}
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
