import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  Award,
  Clock,
  Eye,
  EyeOff,
  FileText,
  HardHat,
  LayoutDashboard,
  Loader2,
  Lock,
  Mail,
  MapPin,
  ShieldCheck,
  Sprout,
  Tractor,
  Upload,
  User,
  Wrench,
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

const SKILL_SUGGESTIONS = [
  'Tractor Operation',
  'Combine Harvester',
  'Rotavator & Cultivator',
  'Paddy Transplanter',
  'Laser Land Leveler',
  'Agricultural Drone',
  'Baler & Thresher',
];

function RegisterOperator() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    mobileNumber: '',
    email: '',
    address: '',
    aadhaarNumber: '',
    drivingLicenseNumber: '',
    experience: '',
    skills: '',
    password: '',
    confirmPassword: '',
    profilePhoto: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isDuplicateMobile, setIsDuplicateMobile] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errorMessage) {
      setErrorMessage('');
      setIsDuplicateMobile(false);
    }
  };

  const handleMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setFormData((prev) => ({
      ...prev,
      mobileNumber: val,
    }));
    if (errorMessage) {
      setErrorMessage('');
      setIsDuplicateMobile(false);
    }
  };

  const handleAadhaarChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 12);
    setFormData((prev) => ({
      ...prev,
      aadhaarNumber: val,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleExperienceChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 2);
    setFormData((prev) => ({
      ...prev,
      experience: val,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handleAddSkill = (skill) => {
    const current = formData.skills
      ? formData.skills.split(',').map((s) => s.trim()).filter(Boolean)
      : [];
    if (!current.includes(skill)) {
      const updated = [...current, skill].join(', ');
      setFormData((prev) => ({ ...prev, skills: updated }));
    }
  };

  const handleRoleSelect = (roleId) => {
    if (roleId === 'farmer') {
      navigate('/register');
      return;
    }
    if (roleId === 'owner') {
      navigate('/register/partner');
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
    setIsDuplicateMobile(false);
    setSuccessMessage('');

    // Validation checks matching backend OperatorRegistrationRequest
    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    if (!/^[6-9][0-9]{9}$/.test(formData.mobileNumber)) {
      setErrorMessage('Please enter a valid 10-digit mobile number starting with 6, 7, 8, or 9.');
      return;
    }

    if (!formData.email.trim()) {
      setErrorMessage('Email address is required.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    if (!formData.address.trim()) {
      setErrorMessage('Address is required.');
      return;
    }

    if (!formData.aadhaarNumber || formData.aadhaarNumber.length !== 12) {
      setErrorMessage('Aadhaar number must be exactly 12 digits.');
      return;
    }

    if (!formData.drivingLicenseNumber.trim()) {
      setErrorMessage('Driving license number is required.');
      return;
    }

    if (formData.experience === '' || isNaN(Number(formData.experience)) || Number(formData.experience) < 0) {
      setErrorMessage('Please specify your experience in years (0 or more).');
      return;
    }

    if (!formData.skills.trim()) {
      setErrorMessage('Machinery skills are required (e.g. Tractor Operation, Combine Harvester).');
      return;
    }

    if (!formData.password || formData.password.length < 8) {
      setErrorMessage('Password must contain at least 8 characters.');
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
        mobileNumber: formData.mobileNumber.trim(),
        email: formData.email.trim(),
        address: formData.address.trim(),
        aadhaarNumber: formData.aadhaarNumber.trim(),
        drivingLicenseNumber: formData.drivingLicenseNumber.trim().toUpperCase(),
        experience: parseInt(formData.experience, 10),
        skills: formData.skills.trim(),
        password: formData.password,
        profilePhoto: formData.profilePhoto.trim() || null,
      };

      const regResponse = await operatorService.registerOperator(payload);
      const registeredOperator = regResponse?.data || regResponse;
      const operatorId = registeredOperator?.id;

      // Automatically dispatch initial mobile verification OTP
      try {
        await operatorService.sendOperatorOtp(formData.mobileNumber.trim(), 'MOBILE_VERIFICATION');
      } catch (otpErr) {
        console.warn('Initial OTP trigger:', otpErr.message);
      }

      // Store in temporary session storage for resilience on refresh
      const regSession = {
        operatorId,
        mobileNumber: formData.mobileNumber.trim(),
        fullName: formData.fullName.trim(),
        aadhaarNumber: formData.aadhaarNumber.trim(),
        drivingLicenseNumber: formData.drivingLicenseNumber.trim().toUpperCase(),
      };
      sessionStorage.setItem('agro_pending_operator_reg', JSON.stringify(regSession));

      setSuccessMessage('✓ Account created! Redirecting to mobile verification...');

      setTimeout(() => {
        navigate('/verify-otp/operator', {
          state: regSession,
        });
      }, 1000);
    } catch (err) {
      console.error('Operator Registration error:', err);
      const errMsg = err.message || '';

      if (
        errMsg.toLowerCase().includes('already exists') ||
        errMsg.toLowerCase().includes('duplicate') ||
        err.status === 409
      ) {
        setIsDuplicateMobile(true);
        setErrorMessage('An Operator account with this mobile number already exists. Please login instead.');
      } else {
        setErrorMessage(errMsg || 'Failed to register operator. Please check all fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.06} delayChildren={0.05}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          Register as Machinery Operator
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Get deployed on verified farm tasks, track fieldwork hours, and receive guaranteed payouts.
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
                    layoutId="operator-register-role-highlight"
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
          <div className="space-y-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
            <div className="flex items-start justify-between gap-2">
              <span>{errorMessage}</span>
              <button
                type="button"
                onClick={() => {
                  setErrorMessage('');
                  setIsDuplicateMobile(false);
                }}
                className="shrink-0 text-red-400 transition-colors hover:text-red-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            {isDuplicateMobile && (
              <div className="pt-1">
                <Link
                  to="/login/operator"
                  state={{ mobileNumber: formData.mobileNumber }}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white shadow-xs hover:bg-red-700 transition-colors"
                >
                  <span>Go to Operator Login</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </div>
            )}
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
            label="Email Address"
            icon={Mail}
            type="email"
            value={formData.email}
            onChange={handleInputChange}
          />

          <div>
            <label htmlFor="address" className="mb-1.5 block text-xs font-semibold text-slate-600">
              Residential / Base Address
            </label>
            <div className="rounded-2xl border border-transparent bg-[#F7F6F0] transition-all duration-300 ease-out focus-within:border-emerald-500 focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.55)]">
              <textarea
                id="address"
                name="address"
                rows={2}
                value={formData.address}
                onChange={handleInputChange}
                placeholder="e.g. Near Bus Stand, Shirur, Pune, Maharashtra 412210"
                className="w-full resize-none rounded-2xl bg-transparent px-4 py-3.5 text-[15px] text-slate-900 outline-none placeholder:text-slate-400"
              />
            </div>
          </div>

          <AuthField
            id="aadhaarNumber"
            name="aadhaarNumber"
            label="Aadhaar Number (12 Digits)"
            icon={ShieldCheck}
            type="text"
            maxLength={12}
            value={formData.aadhaarNumber}
            onChange={handleAadhaarChange}
          />

          <AuthField
            id="drivingLicenseNumber"
            name="drivingLicenseNumber"
            label="Driving License Number"
            icon={Award}
            type="text"
            value={formData.drivingLicenseNumber}
            onChange={handleInputChange}
          />

          <AuthField
            id="experience"
            name="experience"
            label="Experience (in Years)"
            icon={Clock}
            type="number"
            min="0"
            max="50"
            value={formData.experience}
            onChange={handleExperienceChange}
          />

          <div>
            <AuthField
              id="skills"
              name="skills"
              label="Machinery Skills (e.g. Tractor, Harvester, Drone)"
              icon={HardHat}
              type="text"
              value={formData.skills}
              onChange={handleInputChange}
            />
            {/* Quick Skill Tags */}
            <div className="mt-2 flex flex-wrap items-center gap-1.5 px-1">
              <span className="text-[11px] font-semibold text-slate-400">Quick add:</span>
              {SKILL_SUGGESTIONS.map((skill) => (
                <button
                  key={skill}
                  type="button"
                  onClick={() => handleAddSkill(skill)}
                  className="rounded-lg bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-800 border border-emerald-200/60 hover:bg-emerald-100 transition-colors"
                >
                  + {skill}
                </button>
              ))}
            </div>
          </div>

          <AuthField
            id="password"
            name="password"
            label="Account Password (Min 8 Characters)"
            icon={Lock}
            type={showPassword ? 'text' : 'password'}
            minLength={8}
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
            minLength={8}
            value={formData.confirmPassword}
            onChange={handleInputChange}
          />

          <AuthField
            id="profilePhoto"
            name="profilePhoto"
            label="Profile Photo URL (Optional)"
            icon={Upload}
            type="url"
            value={formData.profilePhoto}
            onChange={handleInputChange}
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
                  Creating Operator Account...
                </>
              ) : (
                <>
                  Create Operator Account
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </motion.button>
          </MagneticButton>
        </form>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500 space-y-2">
        <div>
          Already have an Operator account?{' '}
          <Link
            to="/login/operator"
            className="font-bold text-emerald-700 underline-offset-2 hover:underline"
          >
            Log In Here
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

export default RegisterOperator;
