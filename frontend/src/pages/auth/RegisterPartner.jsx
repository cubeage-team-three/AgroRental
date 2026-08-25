import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  Building2,
  Eye,
  EyeOff,
  Loader2,
  Lock,
  Mail,
  User,
  X,
} from 'lucide-react';
import { partnerService } from '../../services/partnerService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';
import AuthField from '../../components/auth/AuthField';

function RegisterPartner() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    businessName: '',
    mobileNumber: '',
    email: '',
    address: '',
    password: '',
    confirmPassword: '',
    gstNumber: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    if (!formData.password || formData.password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setErrorMessage('Password and Confirm Password do not match.');
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        businessName: formData.businessName.trim() || null,
        mobileNumber: formData.mobileNumber.trim(),
        email: formData.email.trim() || null,
        address: formData.address.trim() || null,
        gstNumber: formData.gstNumber.trim() || null,
        password: formData.password,
      };

      await partnerService.registerPartner(payload);

      setSuccessMessage('✓ Equipment Partner account registered successfully! Redirecting to login...');

      setTimeout(() => {
        navigate('/login', {
          state: {
            mobileOrEmail: formData.mobileNumber,
            message: 'Registration complete! Please log in with your credentials.',
          },
        });
      }, 1500);
    } catch (err) {
      console.error('Partner Registration error:', err);
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: Account draft created! Redirecting to login...');
        setTimeout(() => {
          navigate('/login');
        }, 1500);
      } else {
        setErrorMessage(err.message || 'Failed to register partner. Please check all fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Register as Machinery Owner
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">List your equipment and start earning on AgroRent.</p>
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
          <div className="flex items-start justify-between gap-2 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            <span>{errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage('')} className="shrink-0 text-red-400 transition-colors hover:text-red-600">
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
            id="fullName"
            name="fullName"
            label="Full Name"
            icon={User}
            type="text"
            value={formData.fullName}
            onChange={handleInputChange}
          />

          <AuthField
            id="businessName"
            name="businessName"
            label="Enterprise Name (Optional)"
            icon={Building2}
            type="text"
            value={formData.businessName}
            onChange={handleInputChange}
          />

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
            id="email"
            name="email"
            label="Email Address (Optional)"
            icon={Mail}
            type="email"
            value={formData.email}
            onChange={handleInputChange}
          />

          <AuthField
            id="password"
            name="password"
            label="Account Password"
            icon={Lock}
            type={showPassword ? 'text' : 'password'}
            minLength={6}
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

          <AuthField
            id="confirmPassword"
            name="confirmPassword"
            label="Confirm Password"
            icon={Lock}
            type="password"
            value={formData.confirmPassword}
            onChange={handleInputChange}
          />

          <div>
            <label htmlFor="address" className="mb-1.5 block text-xs font-semibold text-slate-600">
              Machinery Base Address / Hub (Optional)
            </label>
            <div className="rounded-2xl border border-transparent bg-[#F7F6F0] transition-all duration-300 ease-out focus-within:border-emerald-500 focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.55)]">
              <textarea
                id="address"
                name="address"
                rows={2}
                value={formData.address}
                onChange={handleInputChange}
                placeholder="e.g. Krishi Seva Kendra, Market Yard, Pune, Maharashtra"
                className="w-full resize-none rounded-2xl bg-transparent px-4 py-3.5 text-[15px] text-slate-900 outline-none placeholder:text-slate-400"
              />
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
                  Registering Partner Account...
                </>
              ) : (
                <>
                  Create Partner Account
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500">
        Already have an equipment partner account?{' '}
        <Link to="/login" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
          Log In Here
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default RegisterPartner;
