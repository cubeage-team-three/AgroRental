import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Lock, ArrowRight, Loader2, CheckCircle2, Eye, EyeOff } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: '',
  });

  const [showPassword, setShowPassword] = useState(false);
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
      setErrorMessage('Invalid request: Missing password reset token.');
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
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-xl rounded-2xl border border-slate-200/80 sm:px-10">
          
          <div className="mb-6 text-center">
            <h2 className="text-2xl font-extrabold text-slate-900">Reset Your Password</h2>
            <p className="text-sm text-slate-600 mt-1">Please enter your new password below.</p>
          </div>

          {!token && !submitted && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-center">
              <p className="text-sm font-semibold text-red-700">Invalid or missing reset token from URL link.</p>
              <Link to="/forgot-password" className="inline-block mt-3 text-xs font-bold text-emerald-700 underline">
                Request new reset link
              </Link>
            </div>
          )}

          {submitted ? (
            <div className="py-4 text-center">
              <div className="w-12 h-12 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 mx-auto mb-3">
                <CheckCircle2 className="w-8 h-8" />
              </div>
              <p className="text-sm text-slate-700 font-medium mb-6">{message}</p>
              <Link
                to="/login"
                className="w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm transition-all shadow-md"
              >
                <span>Proceed to Log In</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          ) : token && (
            <form onSubmit={handleSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs font-semibold text-red-700">
                  {errorMessage}
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">New Password</label>
                <div className="relative">
                  <Lock className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="newPassword"
                    value={formData.newPassword}
                    onChange={handleInputChange}
                    placeholder="••••••••"
                    required
                    className="w-full pl-10 pr-10 py-2.5 rounded-xl border border-slate-300 focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Confirm New Password</label>
                <div className="relative">
                  <Lock className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleInputChange}
                    placeholder="••••••••"
                    required
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 text-sm"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center gap-2 py-3 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm transition-all shadow-md disabled:opacity-50"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Resetting Password...</span>
                  </>
                ) : (
                  <>
                    <span>Reset Password</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          )}

        </div>
      </div>
    </div>
  );
}
