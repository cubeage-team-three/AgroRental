import { useState } from 'react';
import { Link, useSearchParams, useLocation } from 'react-router-dom';
import { Mail, CheckCircle2, ArrowLeft, X, ShieldCheck, Info, FlaskConical } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthTextField from '../../components/auth/AuthTextField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';

const ROLES = [
  { id: 'FARMER', label: 'Farmer' },
  { id: 'PARTNER', label: 'Partner' },
  { id: 'OPERATOR', label: 'Operator' },
];

const ROLE_IDS = ROLES.map((r) => r.id);

function resolveRole(...candidates) {
  const role = candidates.map((r) => r?.toUpperCase()).find((r) => r && ROLE_IDS.includes(r));
  return role || 'FARMER';
}

export default function ForgotPassword() {
  const [searchParams] = useSearchParams();
  const location = useLocation();

  const [role, setRole] = useState(() => resolveRole(searchParams.get('role'), location.state?.role));
  const [identifier, setIdentifier] = useState(
    location.state?.identifier || location.state?.mobileOrEmail || location.state?.email || ''
  );
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!identifier.trim()) {
      setErrorMessage('Please enter your registered mobile number or email address.');
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const data = await apiClient.post('/auth/forgot-password', {
        role,
        identifier: identifier.trim(),
      });
      setResult({
        message: data?.message || 'Your request has been received.',
        deliveryConfigured: Boolean(data?.deliveryConfigured),
        devResetToken: data?.devResetToken || null,
        devNotice: data?.devNotice || null,
      });
    } catch (err) {
      console.error('Forgot password error:', err);
      setErrorMessage(err.message || 'Failed to process request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const roleLabel = ROLES.find((r) => r.id === role)?.label || 'Farmer';

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        <div className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
          <ShieldCheck className="h-3.5 w-3.5" />
          <span>Password Recovery</span>
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Forgot your password?
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Choose your account type and enter the mobile number or email address you registered with.
        </p>
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
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

      {result ? (
        <RevealItem className="mt-6">
          <div
            role="status"
            className={`flex items-start gap-3 rounded-2xl border p-4 text-sm font-medium ${
              result.deliveryConfigured
                ? 'border-emerald-200 bg-emerald-50 text-emerald-900'
                : 'border-slate-200 bg-slate-50 text-slate-700'
            }`}
          >
            {result.deliveryConfigured ? (
              <CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" />
            ) : (
              <Info className="mt-0.5 h-5 w-5 shrink-0 text-slate-500" />
            )}
            <p>{result.message}</p>
          </div>

          {result.devNotice && (
            <div className="mt-4 rounded-2xl border-2 border-dashed border-amber-400 bg-amber-50 p-4 text-sm text-amber-900">
              <div className="flex items-center gap-2 font-bold uppercase tracking-wide text-amber-800">
                <FlaskConical className="h-4 w-4" />
                Development only
              </div>
              <p className="mt-1.5 text-xs leading-relaxed">{result.devNotice}</p>
              {result.devResetToken ? (
                <Link
                  to={`/reset-password?token=${encodeURIComponent(result.devResetToken)}`}
                  className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-amber-600 px-3.5 py-2 text-xs font-bold text-white hover:bg-amber-700"
                >
                  DEV-ONLY: open the reset link
                </Link>
              ) : (
                <p className="mt-2 text-xs font-semibold">No reset token was issued for these details.</p>
              )}
            </div>
          )}

          <Link
            to={`/login?role=${role}`}
            className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 px-6 py-3.5 text-[15px] font-semibold text-white shadow-md transition-all hover:bg-emerald-900"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Return to {roleLabel} Log In</span>
          </Link>
          <button
            type="button"
            onClick={() => setResult(null)}
            className="mt-3 w-full text-center text-xs font-semibold text-emerald-700 underline-offset-2 hover:underline"
          >
            Try different details
          </button>
        </RevealItem>
      ) : (
        <RevealItem className="mt-6">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div>
              <span className="mb-2 block text-xs font-bold uppercase tracking-wider text-slate-500">
                Account Type
              </span>
              <div
                role="tablist"
                aria-label="Select the account type to recover"
                className="grid grid-cols-3 gap-1 rounded-2xl bg-[#F0EFE9] p-1"
              >
                {ROLES.map((r) => {
                  const isActive = role === r.id;
                  return (
                    <button
                      key={r.id}
                      id={`forgot-role-${r.id.toLowerCase()}`}
                      role="tab"
                      aria-selected={isActive}
                      type="button"
                      onClick={() => setRole(r.id)}
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
            </div>

            <AuthTextField
              id="identifier"
              name="identifier"
              label="Mobile Number or Email Address"
              icon={Mail}
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              autoComplete="username"
              required
            />

            <AuthPrimaryButton loading={loading} loadingText="Submitting...">
              Reset Password
            </AuthPrimaryButton>
          </form>

          <div className="mt-8 text-center text-sm text-slate-500">
            Remember your password?{' '}
            <Link
              to={`/login?role=${role}`}
              className="font-bold text-emerald-700 underline-offset-2 hover:underline"
            >
              Log In Here
            </Link>
          </div>
        </RevealItem>
      )}
    </RevealGroup>
  );
}
