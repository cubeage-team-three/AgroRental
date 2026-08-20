import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HardHat,
  User,
  Phone,
  Mail,
  MapPin,
  Lock,
  Eye,
  EyeOff,
  Wrench,
  Award,
  ShieldCheck,
  FileText,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ArrowLeft,
  Loader2,
  RefreshCw,
  Clock,
  Sparkles,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';

const POPULAR_SKILLS = [
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
    <div className="min-h-screen bg-[#FDFBF7] py-8 px-4 sm:px-6 lg:px-8 font-sans">
      <div className="max-w-2xl mx-auto space-y-6">

        {/* Brand Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-3xl bg-[#142E1C] text-[#C1FF72] shadow-md mb-1">
            <HardHat className="w-7 h-7" />
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Equipment Operator Registration
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 font-medium">
            Join the certified AgroRental machinery crew. Connect with fleet owners and earn verified job deployments.
          </p>
        </div>

        {/* Stepper Wizard Progress */}
        <div className="bg-white rounded-3xl p-4 sm:p-6 border border-gray-200/70 shadow-xs">
          <div className="grid grid-cols-4 gap-2 text-center">
            {[
              { step: 1, label: 'Profile & Skills' },
              { step: 2, label: 'KYC & License' },
              { step: 3, label: 'OTP Verification' },
              { step: 4, label: 'Approval Status' },
            ].map((item) => (
              <div key={item.step} className="space-y-1">
                <div
                  className={`h-2 rounded-full transition-all duration-300 ${
                    currentStep >= item.step ? 'bg-[#3E7B27]' : 'bg-gray-200'
                  }`}
                />
                <span
                  className={`text-[10px] sm:text-xs font-bold block ${
                    currentStep === item.step
                      ? 'text-[#142E1C] font-black'
                      : currentStep > item.step
                      ? 'text-[#3E7B27]'
                      : 'text-gray-400'
                  }`}
                >
                  {item.step}. {item.label}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Error / Success Banners */}
        <AnimatePresence>
          {errorMessage && (
            <motion.div
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs font-bold flex items-center justify-between shadow-xs"
            >
              <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
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
              initial={{ opacity: 0, y: -8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              className="p-4 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-900 text-xs font-bold flex items-center justify-between shadow-xs"
            >
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600" />
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

        {/* Step 1: Personal & Operational Skills Form */}
        {currentStep === 1 && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6"
          >
            <div className="border-b border-gray-100 pb-3">
              <h2 className="text-lg font-black text-[#142E1C]">
                Step 1: Personal & Operational Experience
              </h2>
              <p className="text-xs text-gray-500">
                Provide your basic contact and agricultural equipment skill profile.
              </p>
            </div>

            <form onSubmit={handleProceedToKyc} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Full Legal Name *
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleInputChange}
                      placeholder="e.g. Suresh Shinde"
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Mobile Number (For OTP Verification) *
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="tel"
                      name="mobileNumber"
                      value={formData.mobileNumber}
                      onChange={handleMobileChange}
                      placeholder="10-digit mobile number"
                      maxLength={10}
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Email Address (Optional)
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleInputChange}
                      placeholder="suresh.shinde@example.com"
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Machinery Experience (Years) *
                  </label>
                  <div className="relative">
                    <Award className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="number"
                      name="experience"
                      min="0"
                      max="50"
                      value={formData.experience}
                      onChange={handleInputChange}
                      placeholder="e.g. 5"
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Service Area / Village Address *
                </label>
                <div className="relative">
                  <MapPin className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                  <input
                    type="text"
                    name="address"
                    value={formData.address}
                    onChange={handleInputChange}
                    placeholder="Village, Taluka, District (e.g. Khed, Pune, Maharashtra)"
                    className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Equipment Operating Skills *
                </label>
                <div className="flex flex-wrap gap-2 mb-2">
                  {POPULAR_SKILLS.map((skill) => {
                    const active = selectedSkills.includes(skill);
                    return (
                      <button
                        key={skill}
                        type="button"
                        onClick={() => toggleSkill(skill)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                          active
                            ? 'bg-[#142E1C] text-[#C1FF72] shadow-xs'
                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
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
                  className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Account Password * (Minimum 8 characters)
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={formData.password}
                    onChange={handleInputChange}
                    placeholder="Create secure password"
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

              <div className="pt-4 flex items-center justify-between">
                <Link
                  to="/register"
                  className="text-xs font-bold text-gray-500 hover:text-gray-700"
                >
                  ← Select Different Role
                </Link>

                <button
                  type="submit"
                  className="px-6 py-3 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-extrabold rounded-xl shadow-md transition-all flex items-center gap-2"
                >
                  <span>Proceed to KYC Details</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </form>
          </motion.div>
        )}

        {/* Step 2: KYC & Compliance Details Form */}
        {currentStep === 2 && (
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6"
          >
            <div className="border-b border-gray-100 pb-3">
              <h2 className="text-lg font-black text-[#142E1C]">
                Step 2: Government KYC & Certification
              </h2>
              <p className="text-xs text-gray-500">
                AgroRental requires identity and driving permit verification for equipment safety.
              </p>
            </div>

            <form onSubmit={handleSubmitRegistration} className="space-y-5">
              <div className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Aadhaar Number (12 Digits) *
                  </label>
                  <div className="relative">
                    <ShieldCheck className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="text"
                      name="aadhaarNumber"
                      value={formData.aadhaarNumber}
                      onChange={handleAadhaarChange}
                      placeholder="12-digit Aadhaar number"
                      maxLength={12}
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none tracking-widest font-mono"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Driving / Heavy Machine License Number *
                  </label>
                  <div className="relative">
                    <FileText className="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                    <input
                      type="text"
                      name="drivingLicenseNumber"
                      value={formData.drivingLicenseNumber}
                      onChange={handleInputChange}
                      placeholder="e.g. MH1220200012345"
                      className="w-full pl-10 pr-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none font-mono"
                    />
                  </div>
                </div>

                {/* Optional Document Upload Previews */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                  <div className="border border-dashed border-gray-300 rounded-2xl p-4 text-center bg-gray-50 space-y-1">
                    <ShieldCheck className="w-6 h-6 text-gray-400 mx-auto" />
                    <span className="block text-xs font-bold text-gray-800">
                      Aadhaar Card Document
                    </span>
                    <span className="block text-[11px] text-gray-400">PDF, JPG (Max 5MB)</span>
                    <input
                      type="file"
                      id="aadhaar-upload"
                      className="hidden"
                      onChange={(e) => setAadhaarDocFile(e.target.files[0])}
                    />
                    <label
                      htmlFor="aadhaar-upload"
                      className="mt-2 inline-block cursor-pointer px-3 py-1 bg-white border border-gray-200 rounded-lg text-[11px] font-bold text-[#3E7B27] hover:bg-gray-100"
                    >
                      {aadhaarDocFile ? `✓ ${aadhaarDocFile.name.slice(0, 15)}...` : '+ Attach File'}
                    </label>
                  </div>

                  <div className="border border-dashed border-gray-300 rounded-2xl p-4 text-center bg-gray-50 space-y-1">
                    <FileText className="w-6 h-6 text-gray-400 mx-auto" />
                    <span className="block text-xs font-bold text-gray-800">
                      Driving License Copy
                    </span>
                    <span className="block text-[11px] text-gray-400">PDF, JPG (Max 5MB)</span>
                    <input
                      type="file"
                      id="dl-upload"
                      className="hidden"
                      onChange={(e) => setDlDocFile(e.target.files[0])}
                    />
                    <label
                      htmlFor="dl-upload"
                      className="mt-2 inline-block cursor-pointer px-3 py-1 bg-white border border-gray-200 rounded-lg text-[11px] font-bold text-[#3E7B27] hover:bg-gray-100"
                    >
                      {dlDocFile ? `✓ ${dlDocFile.name.slice(0, 15)}...` : '+ Attach File'}
                    </label>
                  </div>
                </div>
              </div>

              <div className="pt-4 flex items-center justify-between">
                <button
                  type="button"
                  onClick={() => setCurrentStep(1)}
                  className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition"
                >
                  ← Back to Profile
                </button>

                <button
                  type="submit"
                  disabled={loading}
                  className="px-6 py-3 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-extrabold rounded-xl shadow-md transition-all flex items-center gap-2 disabled:opacity-70"
                >
                  {loading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Creating Account & Sending OTP...</span>
                    </>
                  ) : (
                    <>
                      <span>Submit & Verify Mobile</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </div>
            </form>
          </motion.div>
        )}

        {/* Step 3: Instant Mobile OTP Verification */}
        {currentStep === 3 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6 text-center"
          >
            <div className="w-14 h-14 bg-emerald-50 text-[#3E7B27] rounded-3xl flex items-center justify-center mx-auto shadow-inner">
              <Phone className="w-7 h-7" />
            </div>

            <div className="space-y-1">
              <h2 className="text-xl font-black text-[#142E1C]">
                Verify Your Mobile Number
              </h2>
              <p className="text-xs text-gray-500">
                Enter the 6-digit OTP code dispatched to{' '}
                <strong className="text-gray-900">+91 {formData.mobileNumber}</strong>.
              </p>
            </div>

            {/* Dev Mock Auto-Fill Banner */}
            {devMockOtp && (
              <div className="p-3 bg-amber-50 border border-amber-200 rounded-2xl text-xs font-bold text-amber-900 inline-flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-amber-600" />
                <span>Dev Mode Generated OTP: <code className="bg-amber-200 px-1.5 py-0.5 rounded text-black font-mono">{devMockOtp}</code></span>
                <button
                  type="button"
                  onClick={() => {
                    const digits = devMockOtp.split('');
                    setOtp(digits);
                  }}
                  className="underline text-emerald-800 ml-1 hover:text-emerald-950"
                >
                  Auto-fill
                </button>
              </div>
            )}

            <form onSubmit={handleVerifyOtp} className="space-y-6">
              {/* 6-Digit PIN Boxes */}
              <div className="flex justify-center gap-2 sm:gap-3">
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
                    className="w-11 h-13 sm:w-12 sm:h-14 text-center text-lg sm:text-xl font-black font-mono bg-gray-50 border-2 border-gray-200 rounded-2xl focus:bg-white focus:border-[#3E7B27] focus:ring-4 focus:ring-emerald-100 focus:outline-none transition-all"
                  />
                ))}
              </div>

              <div className="flex items-center justify-center gap-2 text-xs">
                {canResend ? (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={otpLoading}
                    className="font-bold text-[#3E7B27] hover:underline inline-flex items-center gap-1"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    Resend 6-Digit OTP
                  </button>
                ) : (
                  <span className="text-gray-400 font-medium inline-flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5" />
                    Resend OTP in <strong className="text-gray-700">{resendTimer}s</strong>
                  </span>
                )}
              </div>

              <button
                type="submit"
                disabled={otpLoading || otp.join('').length !== 6}
                className="w-full py-3.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {otpLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Verifying Code...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Verify Mobile & Confirm Application</span>
                  </>
                )}
              </button>
            </form>
          </motion.div>
        )}

        {/* Step 4: Submission Confirmation & Verification Pending Status */}
        {currentStep === 4 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-3xl p-6 sm:p-8 border border-emerald-100 shadow-lg space-y-6 text-center"
          >
            <div className="w-16 h-16 bg-emerald-100 text-emerald-800 rounded-3xl flex items-center justify-center mx-auto shadow-inner">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="space-y-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-amber-100 text-amber-900 border border-amber-300 text-xs font-bold rounded-full">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse" />
                Application Status: PENDING ADMIN APPROVAL
              </span>
              <h2 className="text-2xl font-black text-[#142E1C]">
                Registration & Mobile Verification Complete!
              </h2>
              <p className="text-xs text-gray-600 max-w-md mx-auto leading-relaxed">
                Thank you, <strong>{formData.fullName}</strong>. Your mobile number (+91 {formData.mobileNumber}) has been verified. Your application has been queued for verification review by the AgroRental safety team.
              </p>
            </div>

            {/* Application Summary Card */}
            <div className="bg-[#FAF8F5] border border-amber-100 rounded-2xl p-4 text-left text-xs space-y-2">
              <div className="flex justify-between border-b border-gray-200/60 pb-1.5">
                <span className="text-gray-400 font-bold uppercase text-[10px]">Operator ID:</span>
                <span className="font-extrabold text-gray-900">#{registeredOperator?.id || 'NEW'}</span>
              </div>
              <div className="flex justify-between border-b border-gray-200/60 pb-1.5">
                <span className="text-gray-400 font-bold uppercase text-[10px]">Mobile Verification:</span>
                <span className="font-bold text-emerald-700">✓ Verified</span>
              </div>
              <div className="flex justify-between border-b border-gray-200/60 pb-1.5">
                <span className="text-gray-400 font-bold uppercase text-[10px]">Experience & Skills:</span>
                <span className="font-semibold text-gray-800">{formData.experience} Years • {formData.skills || selectedSkills.join(', ')}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400 font-bold uppercase text-[10px]">License Number:</span>
                <span className="font-mono text-gray-700">{formData.drivingLicenseNumber}</span>
              </div>
            </div>

            <div className="pt-2 flex flex-col sm:flex-row gap-3 justify-center">
              <Link
                to="/login"
                className="w-full sm:w-auto px-6 py-3 bg-[#142E1C] hover:bg-[#0E2013] text-white text-xs font-bold rounded-xl shadow transition text-center"
              >
                Go to Login Page
              </Link>
              <Link
                to="/"
                className="w-full sm:w-auto px-6 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition text-center"
              >
                Return to Home
              </Link>
            </div>
          </motion.div>
        )}

      </div>
    </div>
  );
}

export default RegisterOperator;
