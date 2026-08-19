import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowRight, LayoutDashboard, Loader2, MessageCircle, Sprout, Tractor, User, Eye, EyeOff, X } from 'lucide-react';
import { registerFarmer } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';
import AuthField from '../../components/auth/AuthField';

const ROLES = [
  { id: 'farmer', label: 'Farmer', icon: Sprout },
  { id: 'owner', label: 'Equipment Owner', icon: Tractor },
  { id: 'admin', label: 'Admin', icon: LayoutDashboard },
];

function Register() {
  const { t, setLanguage } = useLanguage();
  const navigate = useNavigate();
  const [role, setRole] = useState('farmer'); // farmer, owner, admin

  const [formData, setFormData] = useState({
    fullName: '',
    mobileNumber: '',
    email: '',
    password: '',
    preferredLanguage: 'English',
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
    // Only allow numeric digits up to 10 chars
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setFormData((prev) => ({
      ...prev,
      mobileNumber: val,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleRoleSelect = (roleId) => {
    if (roleId === 'owner') {
      setRole('owner');
      navigate('/register/partner');
      return;
    }
    setRole(roleId);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    // Validation checks
    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    setLoading(true);

    try {
      // Call backend Spring Boot API POST /api/farmers/register
      const response = await registerFarmer({
        fullName: formData.fullName.trim(),
        mobileNumber: formData.mobileNumber.trim(),
        email: formData.email.trim() || null,
        password: formData.password || null,
        preferredLanguage: formData.preferredLanguage,
      });

      setSuccessMessage('Registration successful! Redirecting to OTP verification...');

      // Redirect to OTP verification after 1.5 seconds with state
      setTimeout(() => {
        navigate('/verify-otp', {
          state: {
            mobileNumber: formData.mobileNumber,
            farmerData: response.data,
          },
        });
      }, 1200);
    } catch (err) {
      console.error('Registration error:', err);
      // Fallback for offline frontend demo if backend server is not connected
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: Account draft created! Proceeding to OTP Verification...');
        setTimeout(() => {
          navigate('/verify-otp', {
            state: {
              mobileNumber: formData.mobileNumber,
              farmerData: { fullName: formData.fullName, mobileNumber: formData.mobileNumber },
            },
          });
        }, 1200);
      } else {
        setErrorMessage(err.message || 'Failed to register. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.07} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Create your account
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">Join 50,000+ farmers on AgroRent.</p>
      </RevealItem>

      <RevealItem className="mt-7">
        <span className="mb-3 block text-xs font-semibold uppercase tracking-wide text-slate-400">I am a</span>
        <div className="grid grid-cols-3 gap-2.5">
          {ROLES.map((r) => (
            <button
              key={r.id}
              type="button"
              onClick={() => handleRoleSelect(r.id)}
              className="relative flex flex-col items-center justify-center gap-1.5 rounded-2xl border-2 border-transparent p-3.5 transition-colors duration-200"
              style={{
                borderColor: role === r.id ? undefined : 'transparent',
              }}
            >
              {role === r.id && (
                <motion.span
                  layoutId="role-highlight"
                  transition={{ type: 'spring', stiffness: 420, damping: 32 }}
                  className="absolute inset-0 rounded-2xl border-2 border-emerald-600 bg-emerald-50"
                />
              )}
              <r.icon
                className={`relative h-5 w-5 ${role === r.id ? 'text-emerald-700' : 'text-slate-400'}`}
              />
              <span className={`relative text-xs font-semibold ${role === r.id ? 'text-emerald-800' : 'text-slate-500'}`}>
                {r.label}
              </span>
            </button>
          ))}
        </div>
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
            label={t('full_name')}
            icon={User}
            type="text"
            value={formData.fullName}
            onChange={handleInputChange}
          />

          <AuthField
            id="mobileNumber"
            name="mobileNumber"
            label={t('mobile_number')}
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
            type="email"
            value={formData.email}
            onChange={handleInputChange}
          />

          <AuthField
            id="password"
            name="password"
            label="Password (Optional for OTP auth)"
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

          <div>
            <label htmlFor="preferredLanguage" className="mb-1.5 block text-xs font-semibold text-slate-600">
              {t('preferred_language')}
            </label>
            <select
              id="preferredLanguage"
              name="preferredLanguage"
              value={formData.preferredLanguage}
              onChange={(e) => {
                handleInputChange(e);
                setLanguage(e.target.value);
              }}
              className="min-h-[54px] w-full cursor-pointer rounded-2xl border border-transparent bg-[#F7F6F0] px-4 text-[15px] text-slate-900 outline-none transition-all duration-300 ease-out focus:border-emerald-500 focus:bg-white focus:shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.55)]"
            >
              <option value="English">English</option>
              <option value="Hindi">Hindi (हिंदी)</option>
              <option value="Marathi">Marathi (मराठी)</option>
              <option value="Telugu">Telugu (తెలుగు)</option>
              <option value="Tamil">Tamil (தமிழ்)</option>
              <option value="Kannada">Kannada (ಕನ್ನಡ)</option>
              <option value="Gujarati">Gujarati (ગુજરાતી)</option>
              <option value="Punjabi">Punjabi (ਪੰਜਾਬੀ)</option>
              <option value="Bengali">Bengali (বাংলা)</option>
            </select>
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
                  Sending OTP...
                </>
              ) : (
                <>
                  {t('send_otp')}
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-6">
        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-slate-200" />
          </div>
          <div className="relative flex justify-center">
            <span className="bg-white px-3 text-xs font-medium uppercase tracking-wide text-slate-400">
              or continue with
            </span>
          </div>
        </div>
      </RevealItem>

      <RevealItem className="mt-4">
        <button
          type="button"
          onClick={() => {
            if (formData.mobileNumber.length === 10) {
              handleSubmit({ preventDefault: () => {} });
            } else {
              setErrorMessage('Please enter a 10-digit mobile number for WhatsApp OTP Login.');
            }
          }}
          className="flex min-h-[50px] w-full items-center justify-center gap-2 rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:border-emerald-300 hover:bg-emerald-50"
        >
          <MessageCircle className="h-[18px] w-[18px] text-[#25D366]" />
          OTP Login (WhatsApp)
        </button>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
          Log In
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default Register;
