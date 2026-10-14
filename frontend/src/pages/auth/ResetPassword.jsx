import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle2, ArrowLeft, X, ShieldCheck } from 'lucide-react';
import { apiClient } from '../../services/apiClient';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import AuthPasswordField from '../../components/auth/AuthPasswordField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const roleParam = searchParams.get('role')?.toUpperCase();
  const role = ['FARMER', 'PARTNER', 'OPERATOR'].includes(roleParam) ? roleParam : null;

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

  const getRoleLabel = () => {
    if (role === 'PARTNER') return 'Partner';
    if (role === 'OPERATOR') return 'Operator';
    if (role === 'FARMER') return 'Farmer';
    return null;
  };

  const roleLabel = getRoleLabel();

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!token) {
      setErrorMessage('Invalid request: Missing password reset token from URL link.');
      return;
    }

    if (formData.newPassword.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }

    if (formData.newPassword !== formData.confirmPassword) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const res = await apiClient.post('/auth/reset-password', {
        token: token.trim(),
        role: role || undefined,
        newPassword: formData.newPassword,
        confirmPassword: formData.confirmPassword,
      });

      setSubmitted(true);
      setMessage(res.data || res.message || 'Password reset successfully. You can now log in.');
    } catch (err) {
      console.error('Reset password error:', err);
      setErrorMessage(err.message || 'Failed to reset password. The token may be invalid or expired.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.07} delayChildren={0.04}>
      <RevealItem>
        {roleLabel && (
          <div className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-800">
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>{roleLabel} Password Reset</span>
          </div>
        )}
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Reset Your Password
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Please enter and confirm your new account password.
        </p>
      </RevealItem>

      {!token && !submitted && (
        <RevealItem className="mt-5">
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-center">
            <p className="text-sm font-semibold text-red-700">
              Invalid or missing reset token from URL link.
            </p>
            <Link
              to={role ? `/forgot-password?role=${role}` : '/forgot-password'}
              className="mt-3 inline-block text-xs font-bold text-emerald-700 underline"
            >
              Request new reset link
            </Link>
          </div>
        </RevealItem>
      )}

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
            to={role ? `/login?role=${role}` : '/login'}
            className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 px-6 py-3.5 text-[15px] font-semibold text-white shadow-md transition-all hover:bg-emerald-900"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>Go to {roleLabel ? `${roleLabel} ` : ''}Log In</span>
          </Link>
        </RevealItem>
      ) : (
        <RevealItem className="mt-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <AuthPasswordField
              id="newPassword"
              name="newPassword"
              label="New Password (Min 6 Characters)"
              value={formData.newPassword}
              onChange={handleInputChange}
              autoComplete="new-password"
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

            <AuthPrimaryButton loading={loading} loadingText="Updating Password...">
              Reset Password
            </AuthPrimaryButton>
          </form>

          <div className="mt-8 text-center text-sm text-slate-500">
            Return to{' '}
            <Link
              to={role ? `/login?role=${role}` : '/login'}
              className="font-bold text-emerald-700 underline-offset-2 hover:underline"
            >
              Log In
            </Link>
          </div>
        </RevealItem>
      )}
    </RevealGroup>
  );
}
