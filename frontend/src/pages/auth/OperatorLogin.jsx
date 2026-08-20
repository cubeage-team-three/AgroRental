import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HardHat,
  Phone,
  Lock,
  Eye,
  EyeOff,
  Loader2,
  AlertCircle,
  CheckCircle2,
  ArrowRight,
  ShieldCheck,
  Wrench,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';

function OperatorLogin() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    mobileNumber: '',
    password: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) setErrorMessage('');
  };

  const handleMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setFormData((prev) => ({ ...prev, mobileNumber: val }));
    if (errorMessage) setErrorMessage('');
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
        `Welcome back, ${response?.operator?.fullName || 'Operator'}! Redirecting to Operator Portal...`
      );

      setTimeout(() => {
        navigate('/operator/dashboard');
      }, 1000);
    } catch (err) {
      console.error('Operator login error:', err);
      setErrorMessage(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FDFBF7] flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="sm:mx-auto sm:w-full sm:max-w-md space-y-4">
        {/* Brand Icon & Heading */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-3xl bg-[#142E1C] text-[#C1FF72] shadow-md">
            <HardHat className="w-8 h-8" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Machinery Operator Login
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 font-medium">
            Sign in to access your deployed field jobs, machinery schedules, and earnings.
          </p>
        </div>

        {/* Login Card */}
        <motion.div
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6"
        >
          {/* Error / Success Notifications */}
          <AnimatePresence>
            {errorMessage && (
              <motion.div
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center justify-between shadow-xs"
              >
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                  <span>{errorMessage}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setErrorMessage('')}
                  className="text-red-500 hover:text-red-700 font-black text-sm"
                >
                  ✕
                </button>
              </motion.div>
            )}

            {successMessage && (
              <motion.div
                initial={{ opacity: 0, y: -6 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -6 }}
                className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-950 text-xs font-bold flex items-center justify-between shadow-xs"
              >
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{successMessage}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setSuccessMessage('')}
                  className="text-emerald-700 hover:text-emerald-950 font-black text-sm"
                >
                  ✕
                </button>
              </motion.div>
            )}
          </AnimatePresence>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Registered Mobile Number *
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-3 text-xs font-bold text-gray-400">
                  +91
                </span>
                <input
                  type="tel"
                  name="mobileNumber"
                  value={formData.mobileNumber}
                  onChange={handleMobileChange}
                  placeholder="10-digit mobile number"
                  maxLength={10}
                  className="w-full pl-12 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none tracking-wide"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-gray-700 mb-1">
                Account Password *
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  placeholder="Enter your password"
                  className="w-full pl-10 pr-10 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3.5 top-3 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-60"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Verifying Credentials & Token...</span>
                </>
              ) : (
                <>
                  <span>Sign In as Operator</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Registration & Alternative Role Links */}
          <div className="border-t border-gray-100 pt-5 space-y-3 text-center text-xs">
            <p className="text-gray-500 font-medium">
              New machinery operator?{' '}
              <Link
                to="/register/operator"
                className="font-extrabold text-[#3E7B27] hover:underline"
              >
                Register & Verify Credentials →
              </Link>
            </p>

            <p className="text-gray-400 text-[11px]">
              Farmer or Fleet Owner?{' '}
              <Link
                to="/login"
                className="font-bold text-gray-700 hover:underline"
              >
                Standard Portal Login
              </Link>
            </p>
          </div>
        </motion.div>

        {/* Security Assurance Badge */}
        <div className="flex items-center justify-center gap-2 text-[11px] font-semibold text-gray-400">
          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
          <span>AgroRental Safe Authentication • 256-Bit Encrypted Sessions</span>
        </div>
      </div>
    </div>
  );
}

export default OperatorLogin;
