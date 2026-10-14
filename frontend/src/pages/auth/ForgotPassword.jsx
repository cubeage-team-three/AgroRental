import { useState } from 'react';
import { Link, useSearchParams, useLocation } from 'react-router-dom';
import { Mail, CheckCircle2, ArrowLeft, X, ShieldCheck } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthTextField from '../../components/auth/AuthTextField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';

export default function ForgotPassword() {
  const [searchParams] = useSearchParams();
  const location = useLocation();

  const roleParam = searchParams.get('role')?.toUpperCase();
  const stateRole = location.state?.role?.toUpperCase();
  const role = ['FARMER', 'PARTNER', 'OPERATOR'].includes(roleParam)
    ? roleParam
    : ['FARMER', 'PARTNER', 'OPERATOR'].includes(stateRole)
    ? stateRole
    : 'FARMER';

  const initialIdentifier =
    location.state?.mobileOrEmail || location.state?.email || location.state?.identifier || '';

  const [identifier, setIdentifier] = useState(initialIdentifier);
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const getRoleMeta = () => {
    switch (role) {
      case 'PARTNER':
        return {
          title: 'Partner Password Recovery',
          subtitle:
            'Enter your registered email address or mobile number to receive partner password reset instructions.',
          fieldLabel: 'Email Address or Mobile Number',
          badgeText: 'Partner (Equipment Owner)',
        };
      case 'OPERATOR':
        return {
          title: 'Operator Password Recovery',
          subtitle:
            'Enter your registered mobile number or email address to receive operator password reset instructions.',
          fieldLabel: 'Mobile Number or Email Address',
          badgeText: 'Machinery Operator',
        };
      case 'FARMER':
      default:
        return {
          title: 'Farmer Password Recovery',
          subtitle:
            'Enter your registered email address or mobile number to receive password reset instructions.',
          fieldLabel: 'Email Address or Mobile Number',
          badgeText: 'Farmer Account',
        };
    }
  };

  const meta = getRoleMeta();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!identifier.trim()) {
      setErrorMessage(`Please enter your ${meta.fieldLabel.toLowerCase()}.`);
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const res = await apiClient.post('/auth/forgot-password', {
        role,
        email: identifier.trim(),
        mobileOrEmail: identifier.trim(),
        identifier: identifier.trim(),
      });
      setSubmitted(true);
      setMessage(
        res.data ||
          res.message ||
          'If an account with that identifier exists, password reset instructions have been sent.'
      );
    } catch (err) {
      console.error('Forgot password error:', err);
      setErrorMessage(err.message || 'Failed to process request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        <div className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
          <ShieldCheck className="h-3.5 w-3.5" />
          <span>{meta.badgeText}</span>
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          {meta.title}
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          {meta.subtitle}
        </p>
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
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

      {submitted ? (
        <RevealItem className="mt-6 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
            <CheckCircle2 className="h-8 w-8" />
          </div>
          <p className="mb-6 text-[15px] font-medium text-slate-700">{message}</p>
          <Link
            to={`/login?role=${role}`}
            className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 px-6 py-3.5 text-[15px] font-semibold text-white shadow-md transition-all hover:bg-emerald-900"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Return to {meta.badgeText} Log In</span>
          </Link>
        </RevealItem>
      ) : (
        <RevealItem className="mt-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <AuthTextField
              id="identifier"
              name="identifier"
              label={meta.fieldLabel}
              icon={Mail}
              type="text"
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              autoComplete="username"
              required
            />

            <AuthPrimaryButton loading={loading} loadingText="Sending Link...">
              Send Reset Instructions
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
