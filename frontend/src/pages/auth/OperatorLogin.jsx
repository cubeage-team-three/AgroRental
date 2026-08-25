import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  Eye,
  EyeOff,
  HardHat,
  LayoutDashboard,
  Loader2,
  Lock,
  Phone,
  Sprout,
  Tractor,
  X,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';
import AuthField from '../../components/auth/AuthField';

const ROLES = [
  { id: 'farmer', label: 'Farmer', icon: Sprout },
  { id: 'owner', label: 'Equipment Owner', icon: Tractor },
  { id: 'operator', label: 'Operator', icon: HardHat },
  { id: 'admin', label: 'Admin', icon: LayoutDashboard },
];

function OperatorLogin() {
  const navigate = useNavigate();
  const location = useLocation();

  const [formData, setFormData] = useState({
    mobileNumber: location.state?.mobileNumber || '',
    password: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState(
    location.state?.message || ''
  );

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setFormData((prev) => ({
      ...prev,
      mobileNumber: val,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleRoleSelect = (roleId) => {
    if (roleId === 'farmer') {
      navigate('/login');
      return;
    }
    if (roleId === 'owner') {
      navigate('/login');
      return;
    }
    if (roleId === 'admin') {
      navigate('/admin/login');
      return;
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Please enter your 10-digit registered mobile number.');
      return;
    }

    if (!formData.password) {
      setErrorMessage('Please enter your account password.');
      return;
    }

    setLoading(true);

    try {
      const response = await operatorService.loginOperator({
        mobileNumber: formData.mobileNumber.trim(),
        password: formData.password,
      });

      setSuccessMessage(
        `✓ Welcome back, ${response?.operator?.fullName || 'Operator'}! Redirecting to dashboard...`
      );

      setTimeout(() => {
        navigate('/operator/dashboard');
      }, 1000);
    } catch (err) {
      console.error('Operator Login error:', err);
      const msg = err.message || '';
      if (msg.toLowerCase().includes('suspended') || msg.toLowerCase().includes('inactive')) {
        setErrorMessage('Your operator account has been suspended. Please contact support.');
      } else if (msg.toLowerCase().includes('pending')) {
        setErrorMessage('Your operator account is currently pending verification. You can log in once approved by an administrator.');
      } else if (msg.toLowerCase().includes('rejected')) {
        setErrorMessage(msg);
      } else if (msg.toLowerCase().includes('not found')) {
        setErrorMessage('No operator account found with this mobile number. Please register first.');
      } else {
        setErrorMessage(msg || 'Login failed. Please check your mobile number and password.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Sign in as Machinery Operator
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Enter your registered mobile number and password to access your field jobs.
        </p>
      </RevealItem>

      <RevealItem className="mt-7">
        <span className="mb-3 block text-xs font-semibold uppercase tracking-wide text-slate-400">
          I am a
        </span>
        <div className="grid grid-cols-4 gap-2">
          {ROLES.map((r) => {
            const active = r.id === 'operator';
            return (
              <button
                key={r.id}
                type="button"
                onClick={() => handleRoleSelect(r.id)}
                className="relative flex flex-col items-center justify-center gap-1.5 rounded-2xl border-2 border-transparent p-3 transition-colors duration-200"
              >
                {active && (
                  <motion.span
                    layoutId="operator-login-role-highlight"
                    transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                    className="absolute inset-0 rounded-2xl border-2 border-emerald-600 bg-emerald-50"
                  />
                )}
                <r.icon className={`relative h-5 w-5 ${active ? 'text-emerald-700' : 'text-slate-400'}`} />
                <span className={`relative text-[11px] font-semibold ${active ? 'text-emerald-800' : 'text-slate-500'}`}>
                  {r.label}
                </span>
              </button>
            );
          })}
        </div>
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

      {successMessage && (
        <RevealItem className="mt-5">
          <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
            {successMessage}
          </div>
        </RevealItem>
      )}

      <RevealItem className="mt-6">
        <form onSubmit={handleSubmit} className="space-y-4">
          <AuthField
            id="mobileNumber"
            name="mobileNumber"
            label="Mobile Number"
            type="tel"
            maxLength={10}
            prefix="+91"
            value={formData.mobileNumber}
            onChange={handleMobileChange}
          />

          <AuthField
            id="password"
            name="password"
            label="Password"
            icon={Lock}
            type={showPassword ? 'text' : 'password'}
            value={formData.password}
            onChange={handleInputChange}
            rightSlot={
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                className="text-slate-400 transition-colors hover:text-slate-600"
              >
                {showPassword ? <EyeOff className="h-[18px] w-[18px]" /> : <Eye className="h-[18px] w-[18px]" />}
              </button>
            }
          />

          <MagneticButton className="block w-full pt-2">
            <motion.button
              type="submit"
              disabled={loading}
              animate={
                loading
                  ? {}
                  : {
                      boxShadow: [
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                        '0 0 38px 6px rgba(163,230,53,0.6)',
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                      ],
                    }
              }
              transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
              className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-70"
            >
              {loading ? (
                <>
                  <Loader2 className="h-[18px] w-[18px] animate-spin" />
                  Signing in to Operator Console...
                </>
              ) : (
                <>
                  Login to Operator Account
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500 space-y-2">
        <div>
          Don&apos;t have an Operator account?{' '}
          <Link
            to="/register/operator"
            className="font-bold text-emerald-700 underline-offset-2 hover:underline"
          >
            Create Operator Account
          </Link>
        </div>
        <div>
          <Link to="/" className="text-xs font-semibold text-slate-400 hover:text-slate-600 transition-colors">
            ← Back to Home
          </Link>
        </div>
      </RevealItem>
    </RevealGroup>
  );
}

export default OperatorLogin;
