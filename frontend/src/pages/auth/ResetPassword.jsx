import { useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, ArrowLeft, X, ShieldCheck } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthPasswordField from '../../components/auth/AuthPasswordField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';
import { PASSWORD_POLICY_HINT, getPasswordConfirmError, getPasswordPolicyError } from './passwordPolicy';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: '',
  });

  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleInputChange = (e) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!token) {
      setErrorMessage('This reset link is incomplete. Please request a new one.');
      return;
    }

    const passwordError =
      getPasswordPolicyError(formData.newPassword) ||
      getPasswordConfirmError(formData.newPassword, formData.confirmPassword);
    if (passwordError) {
      setErrorMessage(passwordError);
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const res = await apiClient.post('/auth/reset-password', {
        token: token.trim(),
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword,
      });

      setSubmitted(true);
      setFormData({ newPassword: '', confirmPassword: '' });
      setMessage(typeof res === 'string' && res ? res : 'Password reset successfully. You can now log in.');
      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: { message: 'Your password has been reset. Log in with your new password.' },
        });
      }, 2500);
    } catch (err) {
      console.error('Reset password error:', err);
      setErrorMessage(err.message || 'Failed to reset password. The link may be invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        <div className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
          <ShieldCheck className="h-3.5 w-3.5" />
          <span>Password Reset</span>
        </div>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Reset Your Password
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Choose a new password for your account. The reset link works once and expires after 30 minutes.
        </p>
      </RevealItem>

      {!token && !submitted && (
        <RevealItem className="mt-5">
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-center">
            <p className="text-sm font-semibold text-red-700">This reset link is incomplete or missing its token.</p>
            <Link to="/forgot-password" className="mt-3 inline-block text-xs font-bold text-emerald-700 underline">
              Request a new reset link
            </Link>
          </div>
        </RevealItem>
      )}

      {errorMessage && (
        <RevealItem className="mt-5">
          <div
            role="alert"
            className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700"
          >
            <span>
              {errorMessage}{' '}
              {errorMessage.toLowerCase().includes('token') && (
                <Link to="/forgot-password" className="font-bold underline">
                  Request a new link
                </Link>
              )}
            </span>
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

      {submitted ? (
        <RevealItem className="mt-6 text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
            <CheckCircle2 className="h-8 w-8" />
          </div>
          <p role="status" className="mb-6 text-[15px] font-medium text-slate-700">
            {message}
          </p>
          <Link
            to="/login"
            className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 px-6 py-3.5 text-[15px] font-semibold text-white shadow-md transition-all hover:bg-emerald-900"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Go to Log In</span>
          </Link>
        </RevealItem>
      ) : (
        <RevealItem className="mt-6">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <AuthPasswordField
              id="newPassword"
              name="newPassword"
              label="New Password"
              value={formData.newPassword}
              onChange={handleInputChange}
              autoComplete="new-password"
              aria-describedby="reset-password-hint"
              required
            />

            <AuthPasswordField
              id="confirmPassword"
              name="confirmPassword"
              label="Confirm New Password"
              value={formData.confirmPassword}
              onChange={handleInputChange}
              autoComplete="new-password"
              required
            />
            <p id="reset-password-hint" className="-mt-2 pl-1 text-xs text-slate-500">
              {PASSWORD_POLICY_HINT}
            </p>

            <AuthPrimaryButton loading={loading} loadingText="Updating Password..." disabled={!token}>
              Reset Password
            </AuthPrimaryButton>
          </form>

          <div className="mt-8 text-center text-sm text-slate-500">
            Return to{' '}
            <Link to="/login" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
              Log In
            </Link>
          </div>
        </RevealItem>
      )}
    </RevealGroup>
  );
}
