import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, ArrowRight, Loader2, CheckCircle2, ArrowLeft } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email.trim()) {
      setErrorMessage('Please enter your email address.');
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const res = await apiClient.post('/auth/forgot-password', { email: email.trim() });
      setSubmitted(true);
      setMessage(res.data || res.message || 'If an account with that email exists, password reset instructions have been sent.');
    } catch (err) {
      console.error('Forgot password error:', err);
      setErrorMessage(err.message || 'Failed to process request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-xl rounded-2xl border border-slate-200/80 sm:px-10">
          
          <div className="mb-6 text-center">
            <h2 className="text-2xl font-extrabold text-slate-900">Forgot Password?</h2>
            <p className="text-sm text-slate-600 mt-1">Enter your registered email address to receive password reset instructions.</p>
          </div>

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
                <ArrowLeft className="w-4 h-4" />
                <span>Return to Log In</span>
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              {errorMessage && (
                <div className="p-3 rounded-xl bg-red-50 border border-red-200 text-xs font-semibold text-red-700">
                  {errorMessage}
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Email Address</label>
                <div className="relative">
                  <Mail className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="farmer@example.com"
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
                    <span>Sending Link...</span>
                  </>
                ) : (
                  <>
                    <span>Send Reset Instructions</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>

              <div className="text-center pt-2">
                <Link to="/login" className="text-xs font-semibold text-emerald-700 hover:underline inline-flex items-center gap-1">
                  <ArrowLeft className="w-3 h-3" /> Back to Log In
                </Link>
              </div>
            </form>
          )}

        </div>
      </div>
    </div>
  );
}
