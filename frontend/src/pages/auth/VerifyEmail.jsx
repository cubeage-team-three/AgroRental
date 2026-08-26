import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2, ArrowRight } from 'lucide-react';
import { apiClient } from '../../services/apiClient';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setLoading(false);
      setSuccess(false);
      setMessage('Invalid verification request: Missing verification token.');
      return;
    }

    const verifyToken = async () => {
      try {
        const res = await apiClient.get(`/auth/verify-email?token=${encodeURIComponent(token)}`);
        setSuccess(true);
        setMessage(res.data || res.message || 'Email verified successfully! You can now log in.');
      } catch (err) {
        console.error('Email verification error:', err);
        setSuccess(false);
        setMessage(err.message || 'Email verification failed. The token may be invalid or expired.');
      } finally {
        setLoading(false);
      }
    };

    verifyToken();
  }, [token]);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-6 shadow-xl rounded-2xl border border-slate-200/80 text-center sm:px-10">
          
          {loading && (
            <div className="py-6 flex flex-col items-center">
              <Loader2 className="w-12 h-12 text-emerald-600 animate-spin mb-4" />
              <h2 className="text-xl font-bold text-slate-800">Verifying Your Email...</h2>
              <p className="text-sm text-slate-500 mt-2">Please wait while we confirm your verification link.</p>
            </div>
          )}

          {!loading && success && (
            <div className="py-6 flex flex-col items-center">
              <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 mb-4 shadow-sm">
                <CheckCircle2 className="w-10 h-10" />
              </div>
              <h2 className="text-2xl font-extrabold text-slate-900">Email Verified!</h2>
              <p className="text-sm text-slate-600 mt-2 mb-6">{message}</p>
              <Link
                to="/login"
                className="w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm transition-all shadow-md"
              >
                <span>Proceed to Log In</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          )}

          {!loading && !success && (
            <div className="py-6 flex flex-col items-center">
              <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center text-red-600 mb-4 shadow-sm">
                <XCircle className="w-10 h-10" />
              </div>
              <h2 className="text-2xl font-extrabold text-slate-900">Verification Failed</h2>
              <p className="text-sm text-slate-600 mt-2 mb-6">{message}</p>
              <Link
                to="/login"
                className="w-full inline-flex items-center justify-center gap-2 px-6 py-3 bg-slate-800 hover:bg-slate-900 text-white font-bold rounded-xl text-sm transition-all shadow-md"
              >
                <span>Return to Log In</span>
              </Link>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
