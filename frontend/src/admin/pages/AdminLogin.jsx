import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertCircle, Eye, EyeOff, Loader2, Lock, Mail, ShieldCheck } from 'lucide-react';
import { adminAuthService } from '../services/adminAuthService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';

function AdminLogin() {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!email.trim() || !password) {
      setErrorMessage('Enter both your admin email and password.');
      return;
    }

    setLoading(true);
    try {
      await adminAuthService.login(email.trim(), password);
      navigate('/admin/overview');
    } catch (err) {
      setErrorMessage(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#03050a] px-4 py-12">
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(16,185,129,0.18),transparent_55%)]" />
      <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_85%_90%,rgba(132,204,22,0.10),transparent_50%)]" />
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.04]"
        style={{
          backgroundImage:
            'linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px)',
          backgroundSize: '48px 48px',
        }}
      />

      <RevealGroup stagger={0.08} delayChildren={0.05} className="relative w-full max-w-[420px]">
        <RevealItem className="flex flex-col items-center text-center">
          <span className="flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-400/20 bg-emerald-400/10 text-emerald-300 shadow-[0_0_30px_-6px_rgba(16,185,129,0.6)]">
            <ShieldCheck className="h-7 w-7" />
          </span>
          <h1 className="mt-5 font-display text-2xl font-bold text-white sm:text-[28px]">
            Admin Control Panel
          </h1>
          <p className="mt-2 text-sm text-white/50">Restricted access. Authorized personnel only.</p>
        </RevealItem>

        <RevealItem className="mt-8 rounded-3xl border border-white/10 bg-white/[0.03] p-6 shadow-[0_30px_80px_-20px_rgba(0,0,0,0.7)] backdrop-blur-2xl sm:p-8">
          {errorMessage && (
            <div className="mb-5 flex items-start gap-2 rounded-xl border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-300">
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="admin-email" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-white/40">
                Admin Email
              </label>
              <div className="relative rounded-2xl border border-white/10 bg-white/5 backdrop-blur-md transition-all duration-300 ease-out focus-within:border-emerald-400/60 focus-within:bg-white/10 focus-within:shadow-[0_0_0_4px_rgba(16,185,129,0.15),0_0_28px_-6px_rgba(16,185,129,0.55)]">
                <Mail className="pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-white/30" />
                <input
                  id="admin-email"
                  type="email"
                  autoComplete="username"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="agrorent@admin.in"
                  className="w-full rounded-2xl bg-transparent py-3.5 pl-11 pr-4 text-[15px] text-white placeholder-white/25 outline-none"
                />
              </div>
            </div>

            <div>
              <label htmlFor="admin-password" className="mb-1.5 block text-xs font-semibold uppercase tracking-wide text-white/40">
                Password
              </label>
              <div className="relative rounded-2xl border border-white/10 bg-white/5 backdrop-blur-md transition-all duration-300 ease-out focus-within:border-emerald-400/60 focus-within:bg-white/10 focus-within:shadow-[0_0_0_4px_rgba(16,185,129,0.15),0_0_28px_-6px_rgba(16,185,129,0.55)]">
                <Lock className="pointer-events-none absolute left-4 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-white/30" />
                <input
                  id="admin-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••"
                  className="w-full rounded-2xl bg-transparent py-3.5 pl-11 pr-11 text-[15px] text-white placeholder-white/25 outline-none"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-white/30 transition-colors hover:text-white/60"
                >
                  {showPassword ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
                </button>
              </div>
            </div>

            <MagneticButton className="block w-full pt-1">
              <motion.button
                type="submit"
                disabled={loading}
                animate={
                  loading
                    ? {}
                    : {
                        boxShadow: [
                          '0 0 18px 0px rgba(16,185,129,0.35)',
                          '0 0 34px 4px rgba(16,185,129,0.55)',
                          '0 0 18px 0px rgba(16,185,129,0.35)',
                        ],
                      }
                }
                transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
                className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-500 text-[15px] font-semibold text-emerald-950 transition-all duration-200 ease-out hover:bg-emerald-400 active:scale-[0.98] disabled:opacity-70"
              >
                {loading ? (
                  <>
                    <Loader2 className="h-[18px] w-[18px] animate-spin" />
                    Verifying…
                  </>
                ) : (
                  'Sign In to Admin Panel'
                )}
              </motion.button>
            </MagneticButton>
          </form>
        </RevealItem>

        <RevealItem className="mt-6 flex items-center justify-center gap-2 text-[11px] font-medium text-white/30">
          <Lock className="h-3.5 w-3.5" />
          256-bit encrypted session · Every access attempt is logged
        </RevealItem>
      </RevealGroup>
    </div>
  );
}

export default AdminLogin;
