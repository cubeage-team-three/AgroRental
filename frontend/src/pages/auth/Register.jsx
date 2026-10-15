import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import {
  ArrowRight,
  Award,
  Building2,
  Clock,
  Globe,
  HardHat,
  Mail,
  MapPin,
  ShieldCheck,
  Upload,
  User,
  X,
} from 'lucide-react';
import { registerFarmer } from '../../services/farmerAuthService';
import { partnerService } from '../../services/partnerService';
import { operatorService } from '../../services/operatorService';
import { useLanguage } from '../../context/LanguageContext';
import { RevealGroup, RevealItem } from '../../components/motion/Reveal';
import RoleSelector from '../../components/auth/RoleSelector';
import AuthTextField from '../../components/auth/AuthTextField';
import AuthPasswordField from '../../components/auth/AuthPasswordField';
import AuthPrimaryButton from '../../components/auth/AuthPrimaryButton';
import {
  PASSWORD_POLICY_HINT,
  getPasswordConfirmError,
  getPasswordPolicyError,
} from './passwordPolicy';
import { saveOnboardingSession } from './operatorOnboardingSession';

const SKILL_SUGGESTIONS = [
  'Tractor Operation',
  'Combine Harvester',
  'Rotavator & Cultivator',
  'Paddy Transplanter',
  'Laser Land Leveler',
  'Agricultural Drone',
  'Baler & Thresher',
];

function Register({ initialRole: propRole }) {
  const { t, setLanguage, LANGUAGES } = useLanguage();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();

  // Role resolution hierarchy: propRole > ?role= > location.state?.role > default 'farmer'
  const paramRole = searchParams.get('role')?.toLowerCase();
  const stateRole = location.state?.role?.toLowerCase();
  const rawRole = propRole || paramRole || stateRole || 'farmer';
  const resolveRole = (r) => {
    if (!r) return 'farmer';
    const clean = r.toLowerCase();
    if (clean === 'owner' || clean === 'partner') return 'partner';
    if (clean === 'operator') return 'operator';
    return 'farmer';
  };

  const [selectedRole, setSelectedRole] = useState(resolveRole(rawRole));

  useEffect(() => {
    if (propRole) {
      setSelectedRole(resolveRole(propRole));
    } else if (paramRole) {
      setSelectedRole(resolveRole(paramRole));
    }
  }, [propRole, paramRole]);

  // Loading, success, error, and duplicate states
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isDuplicateMobile, setIsDuplicateMobile] = useState(false);

  // --- Farmer Form State ---
  const [farmerForm, setFarmerForm] = useState({
    fullName: '',
    mobileNumber: '',
    email: '',
    password: '',
    confirmPassword: '',
    preferredLanguage: 'English',
  });

  // --- Partner Form State ---
  const [partnerForm, setPartnerForm] = useState({
    fullName: '',
    businessName: '',
    mobileNumber: '',
    email: '',
    address: '',
    password: '',
    confirmPassword: '',
    gstNumber: '',
  });

  // --- Operator Form State ---
  const [operatorForm, setOperatorForm] = useState({
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

  const clearAlerts = () => {
    setErrorMessage('');
    setSuccessMessage('');
    setIsDuplicateMobile(false);
  };

  const handleRoleSelect = (roleId) => {
    const nextRole = resolveRole(roleId);
    setSelectedRole(nextRole);
    setSearchParams({ role: nextRole }, { replace: true });
    clearAlerts();
  };

  // -------------------------------------------------------------
  // Farmer Handlers
  // -------------------------------------------------------------
  const handleFarmerChange = (e) => {
    const { name, value } = e.target;
    setFarmerForm((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) clearAlerts();
  };

  const handleFarmerMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setFarmerForm((prev) => ({ ...prev, mobileNumber: val }));
    if (errorMessage) clearAlerts();
  };

  const handleFarmerSubmit = async (e) => {
    e.preventDefault();
    clearAlerts();

    if (!farmerForm.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!farmerForm.mobileNumber || farmerForm.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    const passwordError =
      getPasswordPolicyError(farmerForm.password) ||
      getPasswordConfirmError(farmerForm.password, farmerForm.confirmPassword);
    if (passwordError) {
      setErrorMessage(passwordError);
      return;
    }

    setLoading(true);

    try {
      await registerFarmer({
        fullName: farmerForm.fullName.trim(),
        mobileNumber: farmerForm.mobileNumber.trim(),
        email: farmerForm.email.trim() || null,
        password: farmerForm.password,
        preferredLanguage: farmerForm.preferredLanguage,
      });

      setSuccessMessage('Registration successful! Redirecting to log in...');

      setTimeout(() => {
        navigate('/login?role=FARMER', {
          state: {
            role: 'FARMER',
            mobileOrEmail: farmerForm.mobileNumber.trim(),
            message:
              'Your farmer account is ready. Log in with your mobile number (or email) and the password you just created.',
          },
        });
      }, 1200);
    } catch (err) {
      console.error('Farmer registration error:', err);
      setErrorMessage(
        err.status === 0 || (err.message && err.message.includes('Network error'))
          ? 'Could not reach the server. Your account was not created; please try again.'
          : err.message || 'Failed to register farmer. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  // -------------------------------------------------------------
  // Partner Handlers
  // -------------------------------------------------------------
  const handlePartnerChange = (e) => {
    const { name, value } = e.target;
    setPartnerForm((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) clearAlerts();
  };

  const handlePartnerMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setPartnerForm((prev) => ({ ...prev, mobileNumber: val }));
    if (errorMessage) clearAlerts();
  };

  const handlePartnerSubmit = async (e) => {
    e.preventDefault();
    clearAlerts();

    if (!partnerForm.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!partnerForm.mobileNumber || partnerForm.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    const partnerPasswordError =
      getPasswordPolicyError(partnerForm.password) ||
      getPasswordConfirmError(partnerForm.password, partnerForm.confirmPassword);
    if (partnerPasswordError) {
      setErrorMessage(partnerPasswordError);
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: partnerForm.fullName.trim(),
        businessName: partnerForm.businessName.trim() || null,
        mobileNumber: partnerForm.mobileNumber.trim(),
        email: partnerForm.email.trim() || null,
        address: partnerForm.address.trim() || null,
        gstNumber: partnerForm.gstNumber.trim() || null,
        password: partnerForm.password,
      };

      await partnerService.registerPartner(payload);

      setSuccessMessage('✓ Partner registration submitted successfully! Your account is pending admin approval.');

      setTimeout(() => {
        navigate('/login', {
          state: {
            mobileNumber: partnerForm.mobileNumber.trim(),
            mobileOrEmail: partnerForm.mobileNumber.trim(),
            role: 'PARTNER',
            message:
              'Your partner registration has been submitted and is pending admin approval. You can log in once verified and approved by an administrator.',
          },
        });
      }, 1500);
    } catch (err) {
      console.error('Partner registration error:', err);
      setErrorMessage(err.message || 'Failed to register partner. Please check all fields.');
    } finally {
      setLoading(false);
    }
  };

  // -------------------------------------------------------------
  // Operator Handlers
  // -------------------------------------------------------------
  const handleOperatorChange = (e) => {
    const { name, value } = e.target;
    setOperatorForm((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) clearAlerts();
  };

  const handleOperatorMobileChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 10);
    setOperatorForm((prev) => ({ ...prev, mobileNumber: val }));
    if (errorMessage) clearAlerts();
  };

  const handleOperatorAadhaarChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 12);
    setOperatorForm((prev) => ({ ...prev, aadhaarNumber: val }));
    if (errorMessage) clearAlerts();
  };

  const handleOperatorExperienceChange = (e) => {
    const val = e.target.value.replace(/\D/g, '').slice(0, 2);
    setOperatorForm((prev) => ({ ...prev, experience: val }));
    if (errorMessage) clearAlerts();
  };

  const handleAddSkill = (skill) => {
    const current = operatorForm.skills
      ? operatorForm.skills.split(',').map((s) => s.trim()).filter(Boolean)
      : [];
    if (!current.includes(skill)) {
      const updated = [...current, skill].join(', ');
      setOperatorForm((prev) => ({ ...prev, skills: updated }));
    }
  };

  const handleOperatorSubmit = async (e) => {
    e.preventDefault();
    clearAlerts();

    if (!operatorForm.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!operatorForm.mobileNumber || operatorForm.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    if (!/^[6-9][0-9]{9}$/.test(operatorForm.mobileNumber)) {
      setErrorMessage('Please enter a valid 10-digit mobile number starting with 6, 7, 8, or 9.');
      return;
    }

    if (!operatorForm.email.trim()) {
      setErrorMessage('Email address is required.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(operatorForm.email.trim())) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    if (!operatorForm.address.trim()) {
      setErrorMessage('Residential address is required.');
      return;
    }

    if (!operatorForm.aadhaarNumber || operatorForm.aadhaarNumber.length !== 12) {
      setErrorMessage('Aadhaar number must be exactly 12 digits.');
      return;
    }

    if (!operatorForm.drivingLicenseNumber.trim()) {
      setErrorMessage('Driving license number is required.');
      return;
    }

    if (
      operatorForm.experience === '' ||
      isNaN(Number(operatorForm.experience)) ||
      Number(operatorForm.experience) < 0
    ) {
      setErrorMessage('Please specify your machinery experience in years (0 or more).');
      return;
    }

    if (!operatorForm.skills.trim()) {
      setErrorMessage('Machinery skills are required (e.g. Tractor Operation, Combine Harvester).');
      return;
    }

    const operatorPasswordError =
      getPasswordPolicyError(operatorForm.password) ||
      getPasswordConfirmError(operatorForm.password, operatorForm.confirmPassword);
    if (operatorPasswordError) {
      setErrorMessage(operatorPasswordError);
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: operatorForm.fullName.trim(),
        mobileNumber: operatorForm.mobileNumber.trim(),
        email: operatorForm.email.trim(),
        address: operatorForm.address.trim(),
        aadhaarNumber: operatorForm.aadhaarNumber.trim(),
        drivingLicenseNumber: operatorForm.drivingLicenseNumber.trim().toUpperCase(),
        experience: parseInt(operatorForm.experience, 10),
        skills: operatorForm.skills.trim(),
        password: operatorForm.password,
        profilePhoto: operatorForm.profilePhoto.trim() || null,
      };

      const registeredOperator = await operatorService.registerOperator(payload);

      // The onboarding token only allows uploading this operator's own KYC documents while the
      // account is pending; it is kept in sessionStorage (never localStorage) and is not a login.
      saveOnboardingSession({
        operatorId: registeredOperator?.id,
        onboardingToken: registeredOperator?.onboardingToken,
        fullName: operatorForm.fullName.trim(),
        mobileNumber: operatorForm.mobileNumber.trim(),
        aadhaarNumber: operatorForm.aadhaarNumber.trim(),
        drivingLicenseNumber: operatorForm.drivingLicenseNumber.trim().toUpperCase(),
      });

      setSuccessMessage('✓ Account created! Next, upload your KYC documents...');

      setTimeout(() => {
        navigate('/register/operator/kyc');
      }, 1000);
    } catch (err) {
      console.error('Operator registration error:', err);
      const errMsg = err.message || '';

      if (
        errMsg.toLowerCase().includes('already exists') ||
        errMsg.toLowerCase().includes('duplicate') ||
        err.status === 409
      ) {
        setIsDuplicateMobile(true);
        setErrorMessage(
          'An Operator account with this mobile number already exists. Please login instead.'
        );
      } else {
        setErrorMessage(errMsg || 'Failed to register operator. Please check all fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  // Header content based on role
  const getHeaderInfo = () => {
    switch (selectedRole) {
      case 'partner':
        return {
          title: 'Register as Equipment Owner',
          subtitle: 'List your machines and start earning guaranteed rent on AgroRent.',
        };
      case 'operator':
        return {
          title: 'Register as Machinery Operator',
          subtitle:
            'Get deployed on verified farm tasks, track fieldwork hours, and receive guaranteed payouts.',
        };
      case 'farmer':
      default:
        return {
          title: 'Create your account',
          subtitle: 'Join 50,000+ farmers booking equipment on AgroRent.',
        };
    }
  };

  const headerInfo = getHeaderInfo();

  return (
    <RevealGroup stagger={0.06} delayChildren={0.04}>
      <RevealItem>
        <h1 className="font-display text-[28px] font-bold tracking-tight text-slate-900 sm:text-[32px]">
          {headerInfo.title}
        </h1>
        <p className="mt-2 text-[15px] text-slate-500">{headerInfo.subtitle}</p>
      </RevealItem>

      <RevealItem className="mt-7">
        <RoleSelector selectedRole={selectedRole} onSelectRole={handleRoleSelect} />
      </RevealItem>

      {errorMessage && (
        <RevealItem className="mt-5">
          <div className="space-y-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
            <div className="flex items-start justify-between gap-2">
              <span>{errorMessage}</span>
              <button
                type="button"
                onClick={clearAlerts}
                className="shrink-0 text-red-400 transition-colors hover:text-red-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            {isDuplicateMobile && selectedRole === 'operator' && (
              <div className="pt-1">
                <Link
                  to="/login?role=OPERATOR"
                  state={{ mobileNumber: operatorForm.mobileNumber }}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-red-600 px-4 py-2 text-xs font-bold text-white shadow-xs transition-colors hover:bg-red-700"
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

      {/* ------------------------------------------------------------- */}
      {/* 1. Farmer Registration Form */}
      {/* ------------------------------------------------------------- */}
      {selectedRole === 'farmer' && (
        <>
          <RevealItem className="mt-6">
            <form onSubmit={handleFarmerSubmit} className="space-y-4">
              <AuthTextField
                id="fullName"
                name="fullName"
                label={t('full_name')}
                icon={User}
                type="text"
                value={farmerForm.fullName}
                onChange={handleFarmerChange}
                autoComplete="name"
                required
              />

              <AuthTextField
                id="mobileNumber"
                name="mobileNumber"
                label={t('mobile_number')}
                type="tel"
                maxLength={10}
                prefix="+91"
                value={farmerForm.mobileNumber}
                onChange={handleFarmerMobileChange}
                inputMode="numeric"
                autoComplete="tel"
                required
              />

              <AuthTextField
                id="email"
                name="email"
                label="Email Address (Optional)"
                icon={Mail}
                type="email"
                value={farmerForm.email}
                onChange={handleFarmerChange}
                autoComplete="email"
              />

              <AuthPasswordField
                id="password"
                name="password"
                label="Password"
                value={farmerForm.password}
                onChange={handleFarmerChange}
                autoComplete="new-password"
                aria-describedby="farmer-password-hint"
                required
              />

              <AuthPasswordField
                id="confirmPassword"
                name="confirmPassword"
                label="Confirm Password"
                value={farmerForm.confirmPassword}
                onChange={handleFarmerChange}
                autoComplete="new-password"
                required
              />
              <p id="farmer-password-hint" className="-mt-2 pl-1 text-xs text-slate-500">
                {PASSWORD_POLICY_HINT}
              </p>

              <div>
                <label
                  htmlFor="preferredLanguage"
                  className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-slate-500"
                >
                  {t('preferred_language')}
                </label>
                <div className="relative flex items-center rounded-2xl border border-slate-200/80 bg-[#F7F6F0] transition-all duration-300 ease-out focus-within:border-emerald-600 focus-within:bg-white focus-within:shadow-[0_0_0_4px_rgba(16,185,129,0.12)]">
                  <Globe
                    className="pointer-events-none absolute left-4 h-[18px] w-[18px] text-slate-400"
                    aria-hidden="true"
                  />
                  <select
                    id="preferredLanguage"
                    name="preferredLanguage"
                    value={farmerForm.preferredLanguage}
                    onChange={(e) => {
                      handleFarmerChange(e);
                      setLanguage(e.target.value);
                    }}
                    className="min-h-[56px] w-full cursor-pointer rounded-2xl bg-transparent pl-11 pr-8 text-[15px] font-medium text-slate-900 outline-none"
                  >
                    {LANGUAGES.map((lang) => (
                      <option key={lang.code} value={lang.code}>
                        {lang.native} ({lang.label})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <AuthPrimaryButton loading={loading} loadingText="Creating account...">
                {t('register')}
              </AuthPrimaryButton>
            </form>
          </RevealItem>

          <RevealItem className="mt-8 text-center text-sm text-slate-500">
            Already have an account?{' '}
            <Link
              to="/login?role=FARMER"
              className="font-bold text-emerald-700 underline-offset-2 hover:underline"
            >
              Log In
            </Link>
          </RevealItem>
        </>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 2. Partner Registration Form */}
      {/* ------------------------------------------------------------- */}
      {selectedRole === 'partner' && (
        <>
          <RevealItem className="mt-6">
            <form onSubmit={handlePartnerSubmit} className="space-y-4">
              <AuthTextField
                id="partnerFullName"
                name="fullName"
                label="Full Name"
                icon={User}
                type="text"
                value={partnerForm.fullName}
                onChange={handlePartnerChange}
                autoComplete="name"
                required
              />

              <AuthTextField
                id="partnerBusinessName"
                name="businessName"
                label="Business / Farm Name (Optional)"
                icon={Building2}
                type="text"
                value={partnerForm.businessName}
                onChange={handlePartnerChange}
                autoComplete="organization"
              />

              <AuthTextField
                id="partnerMobileNumber"
                name="mobileNumber"
                label="Mobile Number"
                type="tel"
                maxLength={10}
                prefix="+91"
                value={partnerForm.mobileNumber}
                onChange={handlePartnerMobileChange}
                inputMode="numeric"
                autoComplete="tel"
                required
              />

              <AuthTextField
                id="partnerEmail"
                name="email"
                label="Email Address"
                icon={Mail}
                type="email"
                value={partnerForm.email}
                onChange={handlePartnerChange}
                autoComplete="email"
              />

              <AuthTextField
                id="partnerAddress"
                name="address"
                label="Business Address / Base Location"
                icon={MapPin}
                type="text"
                value={partnerForm.address}
                onChange={handlePartnerChange}
                autoComplete="street-address"
              />

              <AuthTextField
                id="partnerGstNumber"
                name="gstNumber"
                label="GST Number (Optional)"
                type="text"
                maxLength={15}
                value={partnerForm.gstNumber}
                onChange={handlePartnerChange}
                autoComplete="off"
              />

              <AuthPasswordField
                id="partnerPassword"
                name="password"
                label="Password"
                value={partnerForm.password}
                onChange={handlePartnerChange}
                autoComplete="new-password"
                aria-describedby="partner-password-hint"
                required
              />

              <AuthPasswordField
                id="partnerConfirmPassword"
                name="confirmPassword"
                label="Confirm Password"
                value={partnerForm.confirmPassword}
                onChange={handlePartnerChange}
                autoComplete="new-password"
                required
              />
              <p id="partner-password-hint" className="-mt-2 pl-1 text-xs text-slate-500">
                {PASSWORD_POLICY_HINT} Your account is activated after an administrator approves it.
              </p>

              <AuthPrimaryButton loading={loading} loadingText="Submitting Partner Application...">
                Register as Equipment Owner
              </AuthPrimaryButton>
            </form>
          </RevealItem>

          <RevealItem className="mt-8 text-center text-sm text-slate-500">
            Already registered as partner?{' '}
            <Link
              to="/login?role=PARTNER"
              className="font-bold text-emerald-700 underline-offset-2 hover:underline"
            >
              Log In
            </Link>
          </RevealItem>
        </>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 3. Operator Registration Form */}
      {/* ------------------------------------------------------------- */}
      {selectedRole === 'operator' && (
        <>
          <RevealItem className="mt-6">
            <form onSubmit={handleOperatorSubmit} className="space-y-4">
              <AuthTextField
                id="opFullName"
                name="fullName"
                label="Full Name"
                icon={User}
                type="text"
                value={operatorForm.fullName}
                onChange={handleOperatorChange}
                autoComplete="name"
                required
              />

              <AuthTextField
                id="opMobileNumber"
                name="mobileNumber"
                label="Mobile Number"
                type="tel"
                maxLength={10}
                prefix="+91"
                value={operatorForm.mobileNumber}
                onChange={handleOperatorMobileChange}
                inputMode="numeric"
                autoComplete="tel"
                required
              />

              <AuthTextField
                id="opEmail"
                name="email"
                label="Email Address"
                icon={Mail}
                type="email"
                value={operatorForm.email}
                onChange={handleOperatorChange}
                autoComplete="email"
                required
              />

              <AuthTextField
                id="opAddress"
                name="address"
                label="Residential Address"
                icon={MapPin}
                type="text"
                value={operatorForm.address}
                onChange={handleOperatorChange}
                autoComplete="street-address"
                required
              />

              <AuthTextField
                id="opAadhaarNumber"
                name="aadhaarNumber"
                label="Aadhaar Number (12 Digits)"
                icon={ShieldCheck}
                type="text"
                maxLength={12}
                value={operatorForm.aadhaarNumber}
                onChange={handleOperatorAadhaarChange}
                inputMode="numeric"
                required
              />

              <AuthTextField
                id="opDrivingLicenseNumber"
                name="drivingLicenseNumber"
                label="Driving License Number"
                icon={Award}
                type="text"
                value={operatorForm.drivingLicenseNumber}
                onChange={handleOperatorChange}
                autoComplete="off"
                required
              />

              <AuthTextField
                id="opExperience"
                name="experience"
                label="Experience (in Years)"
                icon={Clock}
                type="number"
                min="0"
                max="50"
                value={operatorForm.experience}
                onChange={handleOperatorExperienceChange}
                required
              />

              <div>
                <AuthTextField
                  id="opSkills"
                  name="skills"
                  label="Machinery Skills (e.g. Tractor, Harvester, Drone)"
                  icon={HardHat}
                  type="text"
                  value={operatorForm.skills}
                  onChange={handleOperatorChange}
                  required
                />
                <div className="mt-2 flex flex-wrap items-center gap-1.5 px-1">
                  <span className="text-[11px] font-semibold text-slate-400">Quick add:</span>
                  {SKILL_SUGGESTIONS.map((skill) => (
                    <button
                      key={skill}
                      type="button"
                      onClick={() => handleAddSkill(skill)}
                      className="rounded-lg border border-emerald-200/70 bg-emerald-50/70 px-2.5 py-1 text-[11px] font-semibold text-emerald-800 transition-colors hover:bg-emerald-100"
                    >
                      + {skill}
                    </button>
                  ))}
                </div>
              </div>

              <AuthPasswordField
                id="opPassword"
                name="password"
                label="Account Password"
                minLength={8}
                maxLength={64}
                value={operatorForm.password}
                onChange={handleOperatorChange}
                autoComplete="new-password"
                aria-describedby="operator-password-hint"
                required
              />

              <AuthPasswordField
                id="opConfirmPassword"
                name="confirmPassword"
                label="Confirm Password"
                minLength={8}
                maxLength={64}
                value={operatorForm.confirmPassword}
                onChange={handleOperatorChange}
                autoComplete="new-password"
                required
              />
              <p id="operator-password-hint" className="-mt-2 pl-1 text-xs text-slate-500">
                {PASSWORD_POLICY_HINT} After registering you can upload your KYC documents; you can log in once an
                administrator approves your account.
              </p>

              <AuthTextField
                id="opProfilePhoto"
                name="profilePhoto"
                label="Profile Photo URL (Optional)"
                icon={Upload}
                type="url"
                value={operatorForm.profilePhoto}
                onChange={handleOperatorChange}
                autoComplete="photo"
              />

              <AuthPrimaryButton loading={loading} loadingText="Creating Operator Account...">
                Create Operator Account
              </AuthPrimaryButton>
            </form>
          </RevealItem>

          <RevealItem className="mt-8 text-center text-sm text-slate-500">
            Already have an equipment operator account?{' '}
            <Link
              to="/login?role=OPERATOR"
              className="font-bold text-emerald-700 underline-offset-2 hover:underline"
            >
              Log In Here
            </Link>
          </RevealItem>
        </>
      )}
    </RevealGroup>
  );
}

export default Register;
