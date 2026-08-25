import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ArrowRight,
  Award,
  CheckCircle2,
  ChevronLeft,
  Clock,
  Eye,
  EyeOff,
  FileText,
  HardHat,
  Loader2,
  Lock,
  Mail,
  MapPin,
  Phone,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  User,
  X,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import MagneticButton from '../../components/ui/MagneticButton';
import AuthField from '../../components/auth/AuthField';

const POPULAR_SKILLS = [
  'Tractor Operation',
  'Combine Harvester',
  'Rotavator & Cultivator',
  'Paddy Transplanter',
  'Laser Land Leveler',
  'Agricultural Drone',
  'Baler & Thresher',
];

const STEPS = [
  { step: 1, label: 'Profile & Skills' },
  { step: 2, label: 'KYC & License' },
  { step: 3, label: 'OTP Verification' },
  { step: 4, label: 'Approval Status' },
];

const EASE = [0.22, 1, 0.36, 1];

function RegisterOperator() {
  // Wizard Steps: 1 = Basic Info & Skills, 2 = KYC & Docs, 3 = OTP Verification, 4 = Success Pending Status
  const [currentStep, setCurrentStep] = useState(1);

  // Form State
  const [formData, setFormData] = useState({
    fullName: '',
    mobileNumber: '',
    email: '',
    address: '',
    experience: '',
    skills: '',
    password: '',
    aadhaarNumber: '',
    drivingLicenseNumber: '',
    profilePhoto: '',
  });

  const [selectedSkills, setSelectedSkills] = useState([]);
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Registered operator record from Step 2
  const [registeredOperator, setRegisteredOperator] = useState(null);

  // OTP Verification State
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [otpLoading, setOtpLoading] = useState(false);
  const [devMockOtp, setDevMockOtp] = useState(null);
  const [resendTimer, setResendTimer] = useState(45);
  const [canResend, setCanResend] = useState(false);

  // Document metadata state for Step 2
  const [aadhaarDocFile, setAadhaarDocFile] = useState(null);
  const [dlDocFile, setDlDocFile] = useState(null);

  // Timer countdown effect for Step 3 (OTP)
  useEffect(() => {
    let interval = null;
    if (currentStep === 3 && resendTimer > 0) {
      interval = setInterval(() => {
        setResendTimer((prev) => prev - 1);
      }, 1000);
    } else if (resendTimer === 0) {
      setCanResend(true);
      if (interval) clearInterval(interval);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [currentStep, resendTimer]);

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

  const handleAadhaarChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 12);
    setFormData((prev) => ({ ...prev, aadhaarNumber: val }));
    if (errorMessage) setErrorMessage('');
  };

  const toggleSkill = (skill) => {
    let updated;
    if (selectedSkills.includes(skill)) {
      updated = selectedSkills.filter((s) => s !== skill);
    } else {
      updated = [...selectedSkills, skill];
    }
    setSelectedSkills(updated);
    setFormData((prev) => ({ ...prev, skills: updated.join(', ') }));
  };

  // Step 1 -> Step 2 Validation
  const handleProceedToKyc = (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }
    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Please enter a valid 10-digit mobile number.');
      return;
    }
    if (!formData.address.trim()) {
      setErrorMessage('Service location address is required.');
      return;
    }
    if (!formData.experience || Number(formData.experience) < 0) {
      setErrorMessage('Please specify your operational experience in years.');
      return;
    }
    if (selectedSkills.length === 0 && !formData.skills.trim()) {
      setErrorMessage('Please select or specify at least one machinery skill.');
      return;
    }
    if (!formData.password || formData.password.length < 8) {
      setErrorMessage('Password must be at least 8 characters long.');
      return;
    }

    setCurrentStep(2);
  };

  // Step 2 -> Submit Registration & Trigger OTP (Step 3)
  const handleSubmitRegistration = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    if (!formData.aadhaarNumber || formData.aadhaarNumber.length !== 12) {
      setErrorMessage('Aadhaar number must be exactly 12 digits.');
      return;
    }
    if (!formData.drivingLicenseNumber.trim()) {
      setErrorMessage('Driving / Heavy Machinery License number is required.');
      return;
    }

    setLoading(true);

    try {
      // 1. Submit Registration API
      const operatorPayload = {
        fullName: formData.fullName.trim(),
        mobileNumber: formData.mobileNumber.trim(),
        email: formData.email.trim() || `${formData.mobileNumber.trim()}@agrorental.operator`,
        address: formData.address.trim(),
        experience: Number(formData.experience),
        skills: formData.skills.trim() || selectedSkills.join(', '),
        password: formData.password,
        aadhaarNumber: formData.aadhaarNumber.trim(),
        drivingLicenseNumber: formData.drivingLicenseNumber.trim(),
        profilePhoto: formData.profilePhoto.trim() || null,
      };

      const operatorData = await operatorService.registerOperator(operatorPayload);
      setRegisteredOperator(operatorData);

      // 2. Upload KYC document metadata if provided
      if (aadhaarDocFile && operatorData?.id) {
        try {
          await operatorService.uploadDocument(operatorData.id, {
            documentType: 'AADHAAR',
            documentNumber: formData.aadhaarNumber.trim(),
            fileName: aadhaarDocFile.name || 'aadhaar_card.pdf',
            fileUrl: `https://storage.agrorental.com/operators/${operatorData.id}/aadhaar.pdf`,
            fileSize: aadhaarDocFile.size || 102400,
            mimeType: aadhaarDocFile.type || 'application/pdf',
          });
        } catch (docErr) {
          console.warn('Optional document metadata registration notice:', docErr);
        }
      }

      if (dlDocFile && operatorData?.id) {
        try {
          await operatorService.uploadDocument(operatorData.id, {
            documentType: 'DRIVING_LICENSE',
            documentNumber: formData.drivingLicenseNumber.trim(),
            fileName: dlDocFile.name || 'driving_license.pdf',
            fileUrl: `https://storage.agrorental.com/operators/${operatorData.id}/license.pdf`,
            fileSize: dlDocFile.size || 102400,
            mimeType: dlDocFile.type || 'application/pdf',
          });
        } catch (docErr) {
          console.warn('Optional document metadata registration notice:', docErr);
        }
      }

      // 3. Dispatch OTP to mobile number
      const otpResp = await operatorService.sendOtp(formData.mobileNumber.trim());
      if (otpResp?.devMockOtp) {
        setDevMockOtp(otpResp.devMockOtp);
      }

      setResendTimer(45);
      setCanResend(false);
      setCurrentStep(3);
    } catch (err) {
      console.error('Registration failed:', err);
      setErrorMessage(err.message || 'Registration failed. Please check your inputs.');
    } finally {
      setLoading(false);
    }
  };

  // Step 3 -> OTP Verification Handler
  const handleOtpChange = (index, value) => {
    const digit = value.replace(/\D/g, '').slice(-1);
    const newOtp = [...otp];
    newOtp[index] = digit;
    setOtp(newOtp);

    if (errorMessage) setErrorMessage('');

    // Auto-advance to next input
    if (digit && index < 5) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`);
      if (nextInput) nextInput.focus();
    }
  };

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`);
      if (prevInput) prevInput.focus();
    }
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setErrorMessage('');

    const otpCode = otp.join('');
    if (otpCode.length !== 6) {
      setErrorMessage('Please enter the complete 6-digit OTP code.');
      return;
    }

    setOtpLoading(true);

    try {
      await operatorService.verifyOtp(formData.mobileNumber.trim(), otpCode);
      setSuccessMessage('Mobile number verified successfully!');
      setCurrentStep(4);
    } catch (err) {
      console.error('OTP verification error:', err);
      setErrorMessage(err.message || 'Invalid or expired OTP. Please try again.');
    } finally {
      setOtpLoading(false);
    }
  };

  const handleResendOtp = async () => {
    if (!canResend) return;
    setErrorMessage('');
    setSuccessMessage('');
    setOtpLoading(true);

    try {
      const otpResp = await operatorService.sendOtp(formData.mobileNumber.trim());
      if (otpResp?.devMockOtp) {
        setDevMockOtp(otpResp.devMockOtp);
      }
      setResendTimer(45);
      setCanResend(false);
      setSuccessMessage('A new 6-digit OTP has been sent to your mobile number.');
      setOtp(['', '', '', '', '', '']);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to resend OTP. Please wait.');
    } finally {
      setOtpLoading(false);
    }
  };

  return (
    <RevealGroup stagger={0.07} delayChildren={0.05}>
      <RevealItem className="flex justify-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-100 bg-emerald-50 text-emerald-700">
          <HardHat className="h-6 w-6" />
        </span>
      </RevealItem>

      <RevealItem className="mt-5 text-center">
        <h1 className="font-display text-[26px] font-bold tracking-tight text-slate-900 sm:text-[30px]">
          Equipment Operator Registration
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">
          Join the certified AgroRent machinery crew and get matched to jobs near you.
        </p>
      </RevealItem>

      <RevealItem className="mt-7">
        <div className="grid grid-cols-4 gap-2">
          {STEPS.map((item) => (
            <div key={item.step} className="space-y-1.5 text-center">
              <div
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  currentStep >= item.step ? 'bg-emerald-600' : 'bg-slate-200'
                }`}
              />
              <span
                className={`block text-[10px] font-semibold leading-tight ${
                  currentStep === item.step
                    ? 'text-emerald-800'
                    : currentStep > item.step
                      ? 'text-emerald-600'
                      : 'text-slate-400'
                }`}
              >
                {item.label}
              </span>
            </div>
          ))}
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
        <AnimatePresence mode="wait">
          <motion.div
            key={currentStep}
            initial={{ opacity: 0, x: 24 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -24 }}
            transition={{ duration: 0.3, ease: EASE }}
          >
            {currentStep === 1 && (
              <form onSubmit={handleProceedToKyc} className="space-y-4">
                <AuthField
                  id="fullName"
                  name="fullName"
                  label="Full Legal Name"
                  icon={User}
                  type="text"
                  value={formData.fullName}
                  onChange={handleInputChange}
                />
                <AuthField
                  id="mobileNumber"
                  name="mobileNumber"
                  label="Mobile Number (for OTP)"
                  icon={Phone}
                  type="tel"
                  maxLength={10}
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
                  id="experience"
                  name="experience"
                  label="Machinery Experience (Years)"
                  icon={Award}
                  type="number"
                  min="0"
                  max="50"
                  value={formData.experience}
                  onChange={handleInputChange}
                />
                <AuthField
                  id="address"
                  name="address"
                  label="Service Area / Village Address"
                  icon={MapPin}
                  type="text"
                  value={formData.address}
                  onChange={handleInputChange}
                />

                <div>
                  <label className="mb-2 block text-xs font-semibold text-slate-600">
                    Equipment Operating Skills
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {POPULAR_SKILLS.map((skill) => {
                      const active = selectedSkills.includes(skill);
                      return (
                        <button
                          key={skill}
                          type="button"
                          onClick={() => toggleSkill(skill)}
                          className={`rounded-xl px-3 py-1.5 text-xs font-semibold transition-all duration-200 ${
                            active
                              ? 'bg-emerald-800 text-white shadow-sm'
                              : 'bg-[#F0EFE9] text-slate-600 hover:bg-slate-200'
                          }`}
                        >
                          {active ? '✓ ' : '+ '}
                          {skill}
                        </button>
                      );
                    })}
                  </div>
                  <input
                    type="text"
                    name="skills"
                    value={formData.skills}
                    onChange={handleInputChange}
                    placeholder="Or enter custom skills (comma separated)"
                    className="mt-2.5 w-full rounded-xl border border-transparent bg-[#F7F6F0] px-3.5 py-2.5 text-xs font-medium text-slate-900 outline-none transition-all duration-300 ease-out focus:border-emerald-500 focus:bg-white focus:shadow-[0_0_0_4px_rgba(16,185,129,0.12)]"
                  />
                </div>

                <AuthField
                  id="password"
                  name="password"
                  label="Account Password (min. 8 characters)"
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

                <div className="flex items-center justify-between pt-1 text-xs font-semibold text-slate-500">
                  <Link to="/register" className="hover:text-slate-700">
                    ← Select Different Role
                  </Link>
                  <Link to="/login/operator" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
                    Already registered? Log In →
                  </Link>
                </div>

                <MagneticButton className="block w-full">
                  <motion.button
                    type="submit"
                    animate={{
                      boxShadow: [
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                        '0 0 38px 6px rgba(163,230,53,0.6)',
                        '0 0 20px 0px rgba(163,230,53,0.35)',
                      ],
                    }}
                    transition={{ duration: 2.2, repeat: Infinity, ease: 'easeInOut' }}
                    className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98]"
                  >
                    Proceed to KYC Details
                    <ArrowRight className="h-4 w-4" />
                  </motion.button>
                </MagneticButton>
              </form>
            )}

            {currentStep === 2 && (
              <form onSubmit={handleSubmitRegistration} className="space-y-4">
                <AuthField
                  id="aadhaarNumber"
                  name="aadhaarNumber"
                  label="Aadhaar Number (12 digits)"
                  icon={ShieldCheck}
                  type="text"
                  maxLength={12}
                  value={formData.aadhaarNumber}
                  onChange={handleAadhaarChange}
                  className="[&_input]:font-mono [&_input]:tracking-widest"
                />
                <AuthField
                  id="drivingLicenseNumber"
                  name="drivingLicenseNumber"
                  label="Driving / Heavy Machine License No."
                  icon={FileText}
                  type="text"
                  value={formData.drivingLicenseNumber}
                  onChange={handleInputChange}
                  className="[&_input]:font-mono"
                />

                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-2xl border border-dashed border-slate-300 bg-[#F7F6F0] p-4 text-center">
                    <ShieldCheck className="mx-auto h-6 w-6 text-slate-400" />
                    <span className="mt-1.5 block text-xs font-semibold text-slate-800">Aadhaar Card</span>
                    <span className="block text-[10px] text-slate-400">PDF, JPG (Max 5MB)</span>
                    <input
                      type="file"
                      id="aadhaar-upload"
                      className="hidden"
                      onChange={(e) => setAadhaarDocFile(e.target.files[0])}
                    />
                    <label
                      htmlFor="aadhaar-upload"
                      className="mt-2 inline-block cursor-pointer rounded-lg border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold text-emerald-700 transition-colors hover:bg-emerald-50"
                    >
                      {aadhaarDocFile ? `✓ ${aadhaarDocFile.name.slice(0, 12)}…` : '+ Attach File'}
                    </label>
                  </div>

                  <div className="rounded-2xl border border-dashed border-slate-300 bg-[#F7F6F0] p-4 text-center">
                    <FileText className="mx-auto h-6 w-6 text-slate-400" />
                    <span className="mt-1.5 block text-xs font-semibold text-slate-800">License Copy</span>
                    <span className="block text-[10px] text-slate-400">PDF, JPG (Max 5MB)</span>
                    <input type="file" id="dl-upload" className="hidden" onChange={(e) => setDlDocFile(e.target.files[0])} />
                    <label
                      htmlFor="dl-upload"
                      className="mt-2 inline-block cursor-pointer rounded-lg border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold text-emerald-700 transition-colors hover:bg-emerald-50"
                    >
                      {dlDocFile ? `✓ ${dlDocFile.name.slice(0, 12)}…` : '+ Attach File'}
                    </label>
                  </div>
                </div>

                <div className="flex items-center gap-3 pt-1">
                  <button
                    type="button"
                    onClick={() => setCurrentStep(1)}
                    className="flex min-h-[52px] items-center gap-1.5 rounded-2xl border border-slate-200 bg-white px-5 text-sm font-semibold text-slate-600 transition-all duration-200 ease-out hover:border-slate-300 hover:bg-slate-50"
                  >
                    <ChevronLeft className="h-4 w-4" />
                    Back
                  </button>

                  <MagneticButton className="flex-1">
                    <motion.button
                      type="submit"
                      disabled={loading}
                      className="flex min-h-[52px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-sm font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-70"
                    >
                      {loading ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin" />
                          Creating Account…
                        </>
                      ) : (
                        <>
                          Submit &amp; Verify Mobile
                          <ArrowRight className="h-4 w-4" />
                        </>
                      )}
                    </motion.button>
                  </MagneticButton>
                </div>
              </form>
            )}

            {currentStep === 3 && (
              <div className="text-center">
                <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
                  <Phone className="h-6 w-6" />
                </span>
                <h2 className="mt-4 font-display text-xl font-bold text-slate-900">Verify Your Mobile Number</h2>
                <p className="mt-1.5 text-sm text-slate-500">
                  Enter the 6-digit OTP sent to <strong className="text-slate-800">+91 {formData.mobileNumber}</strong>.
                </p>

                {devMockOtp && (
                  <div className="mt-4 flex flex-wrap items-center justify-center gap-2 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-xs font-semibold text-amber-900">
                    <Sparkles className="h-4 w-4 text-amber-600" />
                    <span>
                      Dev OTP: <code className="rounded bg-amber-200 px-1.5 py-0.5 font-mono text-black">{devMockOtp}</code>
                    </span>
                    <button
                      type="button"
                      onClick={() => setOtp(devMockOtp.split(''))}
                      className="font-semibold text-emerald-700 underline-offset-2 hover:underline"
                    >
                      Auto-fill
                    </button>
                  </div>
                )}

                <form onSubmit={handleVerifyOtp} className="mt-6 space-y-5">
                  <div className="flex justify-center gap-2">
                    {otp.map((digit, idx) => (
                      <input
                        key={idx}
                        id={`otp-input-${idx}`}
                        type="text"
                        inputMode="numeric"
                        maxLength={1}
                        value={digit}
                        onChange={(e) => handleOtpChange(idx, e.target.value)}
                        onKeyDown={(e) => handleOtpKeyDown(idx, e)}
                        className="h-14 w-11 rounded-2xl border border-transparent bg-[#F7F6F0] text-center text-xl font-bold text-slate-900 outline-none transition-all duration-300 ease-out focus:border-emerald-500 focus:bg-white focus:shadow-[0_0_0_4px_rgba(16,185,129,0.12),0_0_22px_-6px_rgba(132,204,22,0.55)]"
                      />
                    ))}
                  </div>

                  <div className="flex items-center justify-center gap-1.5 text-xs">
                    {canResend ? (
                      <button
                        type="button"
                        onClick={handleResendOtp}
                        disabled={otpLoading}
                        className="inline-flex items-center gap-1 font-semibold text-emerald-700 transition-colors hover:underline disabled:opacity-50"
                      >
                        <RefreshCw className="h-3.5 w-3.5" />
                        Resend OTP
                      </button>
                    ) : (
                      <span className="inline-flex items-center gap-1 font-medium text-slate-400">
                        <Clock className="h-3.5 w-3.5" />
                        Resend in <strong className="text-slate-600">{resendTimer}s</strong>
                      </span>
                    )}
                  </div>

                  <MagneticButton className="block w-full">
                    <motion.button
                      type="submit"
                      disabled={otpLoading || otp.join('').length !== 6}
                      className="flex min-h-[54px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-800 text-[15px] font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900 active:scale-[0.98] disabled:opacity-60"
                    >
                      {otpLoading ? (
                        <>
                          <Loader2 className="h-[18px] w-[18px] animate-spin" />
                          Verifying…
                        </>
                      ) : (
                        <>
                          <CheckCircle2 className="h-[18px] w-[18px]" />
                          Verify &amp; Confirm
                        </>
                      )}
                    </motion.button>
                  </MagneticButton>
                </form>
              </div>
            )}

            {currentStep === 4 && (
              <div className="text-center">
                <motion.span
                  initial={{ scale: 0.4, opacity: 0, rotate: -20 }}
                  animate={{ scale: 1, opacity: 1, rotate: 0 }}
                  transition={{ type: 'spring', stiffness: 260, damping: 18 }}
                  className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-lime-400/15 text-lime-600 shadow-[0_0_35px_-8px_rgba(132,204,22,0.5)]"
                >
                  <CheckCircle2 className="h-9 w-9" />
                </motion.span>

                <span className="mt-5 inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-semibold text-amber-800">
                  <span className="h-1.5 w-1.5 animate-pulse rounded-full bg-amber-500" />
                  Pending Admin Approval
                </span>

                <h2 className="mt-3 font-display text-2xl font-bold text-slate-900">Registration Complete!</h2>
                <p className="mt-2 text-sm leading-relaxed text-slate-600">
                  Thank you, <strong>{formData.fullName}</strong>. Your mobile number has been verified. Your
                  application is queued for review by the AgroRent safety team.
                </p>

                <div className="mt-5 space-y-2 rounded-2xl border border-slate-200 bg-[#F7F6F0] p-4 text-left text-xs">
                  <div className="flex justify-between border-b border-slate-200/70 pb-1.5">
                    <span className="font-semibold uppercase tracking-wide text-slate-400">Operator ID</span>
                    <span className="font-bold text-slate-800">#{registeredOperator?.id || 'NEW'}</span>
                  </div>
                  <div className="flex justify-between border-b border-slate-200/70 pb-1.5">
                    <span className="font-semibold uppercase tracking-wide text-slate-400">Mobile</span>
                    <span className="font-semibold text-emerald-700">✓ Verified</span>
                  </div>
                  <div className="flex justify-between gap-3 border-b border-slate-200/70 pb-1.5">
                    <span className="shrink-0 font-semibold uppercase tracking-wide text-slate-400">
                      Experience &amp; Skills
                    </span>
                    <span className="text-right font-medium text-slate-700">
                      {formData.experience} yrs • {formData.skills || selectedSkills.join(', ')}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="font-semibold uppercase tracking-wide text-slate-400">License No.</span>
                    <span className="font-mono text-slate-700">{formData.drivingLicenseNumber}</span>
                  </div>
                </div>

                <div className="mt-6 space-y-3">
                  <Link
                    to="/login/operator"
                    className="flex min-h-[52px] w-full items-center justify-center rounded-2xl bg-emerald-800 text-sm font-semibold text-white transition-all duration-200 ease-out hover:bg-emerald-900"
                  >
                    Go to Operator Login
                  </Link>
                  <Link
                    to="/"
                    className="flex min-h-[52px] w-full items-center justify-center rounded-2xl border border-slate-200 bg-white text-sm font-semibold text-slate-700 transition-all duration-200 ease-out hover:bg-slate-50"
                  >
                    Return to Home
                  </Link>
                </div>
              </div>
            )}
          </motion.div>
        </AnimatePresence>
      </RevealItem>

      <RevealItem className="mt-8 text-center text-sm text-slate-500">
        Already have an equipment operator account?{' '}
        <Link to="/login/operator" className="font-bold text-emerald-700 underline-offset-2 hover:underline">
          Log In Here
        </Link>
      </RevealItem>
    </RevealGroup>
  );
}

export default RegisterOperator;
