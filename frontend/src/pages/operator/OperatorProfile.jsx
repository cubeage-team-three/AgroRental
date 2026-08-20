import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HardHat,
  Phone,
  Mail,
  MapPin,
  Award,
  Wrench,
  ShieldCheck,
  CheckCircle2,
  AlertCircle,
  Lock,
  Eye,
  EyeOff,
  Save,
  KeyRound,
  Loader2,
  Calendar,
  Building2,
  Sparkles,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { getCurrentUser } from '../../services/authService';

const AVAILABLE_SKILL_TAGS = [
  'Tractor Operation',
  'Combine Harvester',
  'Rotavator Work',
  'Drone Spraying',
  'Seeder & Sowing',
  'Power Tiller',
  'Laser Land Leveler',
  'Baler & Straw Reaper',
  'Paddy Transplanter',
  'Subsoiler Operation',
];

function OperatorProfile() {
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState(null);

  // Profile Form State
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    address: '',
    experience: 0,
    skills: '',
    profilePhoto: '',
  });

  // Password Form State
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [showCurrentPass, setShowCurrentPass] = useState(false);
  const [showNewPass, setShowNewPass] = useState(false);
  const [showConfirmPass, setShowConfirmPass] = useState(false);

  // Action States
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');

  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    setProfileError('');
    try {
      const data = await operatorService.getProfile();
      setProfile(data);
      setFormData({
        fullName: data?.fullName || '',
        email: data?.email || '',
        address: data?.address || '',
        experience: data?.experience !== undefined ? data.experience : 0,
        skills: data?.skills || '',
        profilePhoto: data?.profilePhoto || '',
      });
    } catch (err) {
      console.error('Failed to fetch operator profile:', err);
      setProfileError(err.message || 'Failed to load profile details.');
    } finally {
      setLoading(false);
    }
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'experience' ? (value === '' ? '' : Number(value)) : value,
    }));
    if (profileError) setProfileError('');
    if (profileSuccess) setProfileSuccess('');
  };

  const handleToggleSkill = (skill) => {
    const currentSkills = formData.skills
      ? formData.skills.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    let updatedSkills;
    if (currentSkills.includes(skill)) {
      updatedSkills = currentSkills.filter((s) => s !== skill);
    } else {
      updatedSkills = [...currentSkills, skill];
    }

    setFormData((prev) => ({
      ...prev,
      skills: updatedSkills.join(', '),
    }));
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
    if (passwordError) setPasswordError('');
    if (passwordSuccess) setPasswordSuccess('');
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setProfileError('');
    setProfileSuccess('');

    if (!formData.fullName.trim()) {
      setProfileError('Full name is required.');
      return;
    }

    if (!formData.address.trim()) {
      setProfileError('Service location / Address is required.');
      return;
    }

    if (formData.experience === '' || formData.experience < 0) {
      setProfileError('Please enter a valid operational experience in years.');
      return;
    }

    if (!formData.skills.trim()) {
      setProfileError('Please specify at least one machinery skill.');
      return;
    }

    setSavingProfile(true);

    try {
      const updated = await operatorService.updateProfile({
        fullName: formData.fullName.trim(),
        email: formData.email.trim() || null,
        address: formData.address.trim(),
        experience: Number(formData.experience),
        skills: formData.skills.trim(),
        profilePhoto: formData.profilePhoto.trim() || null,
      });

      setProfile(updated);
      setProfileSuccess('Profile information updated successfully!');

      // Update local storage session so navbar and sidebar immediately reflect changes
      const currentStored = getCurrentUser() || {};
      localStorage.setItem(
        'agro_user',
        JSON.stringify({
          ...currentStored,
          fullName: updated.fullName,
          email: updated.email,
          role: 'OPERATOR',
        })
      );
    } catch (err) {
      console.error('Failed to update profile:', err);
      setProfileError(err.message || 'Failed to update profile.');
    } finally {
      setSavingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError('');
    setPasswordSuccess('');

    if (!passwordData.currentPassword) {
      setPasswordError('Please enter your current password.');
      return;
    }

    if (!passwordData.newPassword || passwordData.newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters long.');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordError('New passwords do not match.');
      return;
    }

    if (passwordData.currentPassword === passwordData.newPassword) {
      setPasswordError('New password must be different from current password.');
      return;
    }

    setSavingPassword(true);

    try {
      await operatorService.changePassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword,
      });

      setPasswordSuccess('Password updated successfully! Keep your credentials safe.');
      setPasswordData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
    } catch (err) {
      console.error('Failed to change password:', err);
      setPasswordError(err.message || 'Failed to change password.');
    } finally {
      setSavingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] space-y-3">
        <Loader2 className="w-8 h-8 text-[#3E7B27] animate-spin" />
        <p className="text-xs font-bold text-gray-500 tracking-wide uppercase">
          Loading Operator Profile...
        </p>
      </div>
    );
  }

  const selectedSkillsList = formData.skills
    ? formData.skills.split(',').map((s) => s.trim()).filter(Boolean)
    : [];

  return (
    <div className="max-w-5xl mx-auto space-y-8 font-sans pb-16">
      {/* Page Header & Operator Identity Hero */}
      <div className="bg-gradient-to-br from-[#0F382C] via-[#142E1C] to-[#1E4D38] rounded-3xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="relative">
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl bg-[#C1FF72] text-[#142E1C] font-black text-3xl sm:text-4xl flex items-center justify-center shadow-lg border-2 border-white/20">
                {profile?.fullName?.charAt(0)?.toUpperCase() || 'O'}
              </div>
              <div className="absolute -bottom-1 -right-1 bg-emerald-500 text-white p-1.5 rounded-xl shadow border-2 border-[#142E1C]">
                <HardHat className="w-4 h-4" />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-xl sm:text-2xl font-black tracking-tight">
                  {profile?.fullName || 'Machinery Operator'}
                </h1>
                <span className="px-2.5 py-0.5 bg-[#C1FF72] text-[#142E1C] rounded-full text-[10px] font-black tracking-wider uppercase">
                  Operator #{profile?.id}
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-emerald-100/90 font-medium">
                <span className="flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-lime-300" />
                  +91 {profile?.mobileNumber}
                </span>
                {profile?.email && (
                  <span className="flex items-center gap-1.5">
                    <Mail className="w-3.5 h-3.5 text-lime-300" />
                    {profile.email}
                  </span>
                )}
                <span className="flex items-center gap-1.5">
                  <MapPin className="w-3.5 h-3.5 text-lime-300" />
                  {profile?.address || 'Operating Hub'}
                </span>
              </div>

              {/* Status Badges */}
              <div className="flex flex-wrap items-center gap-2 pt-1">
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-black bg-emerald-950/60 text-emerald-300 border border-emerald-500/30">
                  <CheckCircle2 className="w-3 h-3 text-emerald-400" />
                  Account Status: {profile?.status || 'APPROVED'}
                </span>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-black bg-lime-400/20 text-lime-300 border border-lime-400/30">
                  <ShieldCheck className="w-3 h-3 text-lime-400" />
                  Mobile Verified
                </span>
                {profile?.partnerName && (
                  <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-black bg-emerald-800/60 text-emerald-200 border border-emerald-600/30">
                    <Building2 className="w-3 h-3" />
                    Partner: {profile.partnerName}
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-md px-4 py-3 rounded-2xl border border-white/10 text-right shrink-0">
            <span className="text-[10px] font-extrabold uppercase tracking-widest text-emerald-200 block">
              Experience Level
            </span>
            <span className="text-xl font-black text-[#C1FF72]">
              {profile?.experience || 0} Years
            </span>
          </div>
        </div>
      </div>

      {/* Grid: Profile Editor & Password Changer */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Editable Profile Form (Span 2) */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6">
            <div className="flex items-center justify-between border-b border-gray-100 pb-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center">
                  <HardHat className="w-5 h-5" />
                </div>
                <div>
                  <h2 className="text-base font-extrabold text-gray-900">
                    Personal & Operational Qualifications
                  </h2>
                  <p className="text-xs text-gray-500 font-medium">
                    Update your operating details, service locations, and certified machinery skills.
                  </p>
                </div>
              </div>
            </div>

            {/* Profile Notifications */}
            <AnimatePresence>
              {profileError && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                    <span>{profileError}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setProfileError('')}
                    className="text-red-500 hover:text-red-700 font-black text-sm"
                  >
                    ✕
                  </button>
                </motion.div>
              )}

              {profileSuccess && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-950 text-xs font-bold flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>{profileSuccess}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setProfileSuccess('')}
                    className="text-emerald-700 hover:text-emerald-950 font-black text-sm"
                  >
                    ✕
                  </button>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleSaveProfile} className="space-y-5">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Full Name */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Full Legal Name *
                  </label>
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleProfileChange}
                    placeholder="Enter your full name"
                    className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                </div>

                {/* Mobile Number (Protected/Disabled) */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-xs font-bold text-gray-700">
                      Registered Mobile Number
                    </label>
                    <span className="text-[10px] font-extrabold text-emerald-600 bg-emerald-50 px-1.5 py-0.2 rounded border border-emerald-200">
                      ✓ Primary Verified
                    </span>
                  </div>
                  <div className="relative">
                    <span className="absolute left-3.5 top-2.5 text-xs font-bold text-gray-400">
                      +91
                    </span>
                    <input
                      type="text"
                      disabled
                      value={profile?.mobileNumber || ''}
                      className="w-full pl-12 pr-3.5 py-2.5 bg-gray-100/70 border border-gray-200 rounded-xl text-xs font-bold text-gray-600 cursor-not-allowed"
                    />
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Email Address */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Email Address
                  </label>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleProfileChange}
                    placeholder="operator@example.com"
                    className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                </div>

                {/* Operational Experience */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">
                    Machinery Experience (Years) *
                  </label>
                  <input
                    type="number"
                    name="experience"
                    min="0"
                    max="50"
                    value={formData.experience}
                    onChange={handleProfileChange}
                    className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                </div>
              </div>

              {/* Service Location / Address */}
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Operating Location / Base Address *
                </label>
                <input
                  type="text"
                  name="address"
                  value={formData.address}
                  onChange={handleProfileChange}
                  placeholder="Village / Tehsil / District / Operating Hub"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                />
              </div>

              {/* Skills Selector */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-bold text-gray-700">
                    Operated Machinery Skills *
                  </label>
                  <span className="text-[11px] font-semibold text-gray-400">
                    Click tags to toggle
                  </span>
                </div>

                <div className="flex flex-wrap gap-1.5">
                  {AVAILABLE_SKILL_TAGS.map((tag) => {
                    const isSelected = selectedSkillsList.includes(tag);
                    return (
                      <button
                        key={tag}
                        type="button"
                        onClick={() => handleToggleSkill(tag)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
                          isSelected
                            ? 'bg-[#3E7B27] text-white border-[#3E7B27] shadow-xs'
                            : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                        }`}
                      >
                        {isSelected ? '✓ ' : '+ '}
                        {tag}
                      </button>
                    );
                  })}
                </div>

                <input
                  type="text"
                  name="skills"
                  value={formData.skills}
                  onChange={handleProfileChange}
                  placeholder="Selected skills or custom additions (e.g. Tractor, Seeder)"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                />
              </div>

              {/* Masked KYC Government Identifiers (Immutable) */}
              <div className="bg-gray-50/80 p-4 rounded-2xl border border-gray-200/80 space-y-3">
                <div className="flex items-center gap-2 text-xs font-extrabold text-gray-700">
                  <ShieldCheck className="w-4 h-4 text-emerald-600" />
                  <span>Verified Identity Records (Immutable KYC)</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div className="p-2.5 bg-white rounded-xl border border-gray-200 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] text-gray-400 font-bold uppercase block">
                        Aadhaar UID
                      </span>
                      <span className="font-extrabold text-gray-900">
                        {profile?.maskedAadhaarNumber || 'XXXX-XXXX-XXXX'}
                      </span>
                    </div>
                    <Lock className="w-3.5 h-3.5 text-gray-400" />
                  </div>

                  <div className="p-2.5 bg-white rounded-xl border border-gray-200 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] text-gray-400 font-bold uppercase block">
                        Driving License
                      </span>
                      <span className="font-extrabold text-gray-900">
                        {profile?.maskedDrivingLicenseNumber || 'DL-XXXX-XXXX'}
                      </span>
                    </div>
                    <Lock className="w-3.5 h-3.5 text-gray-400" />
                  </div>
                </div>
              </div>

              {/* Submit Profile Button */}
              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  disabled={savingProfile}
                  className="px-6 py-3 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-md transition-all flex items-center gap-2 disabled:opacity-60"
                >
                  {savingProfile ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Saving Changes...</span>
                    </>
                  ) : (
                    <>
                      <Save className="w-4 h-4" />
                      <span>Save Profile Changes</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right Column: Password & Account Security (Span 1) */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-gray-200/70 shadow-sm space-y-6">
            <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
              <div className="w-10 h-10 rounded-2xl bg-amber-50 text-amber-700 flex items-center justify-center">
                <KeyRound className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-extrabold text-gray-900">
                  Account Password
                </h2>
                <p className="text-xs text-gray-500 font-medium">
                  Update your account access password.
                </p>
              </div>
            </div>

            {/* Password Notifications */}
            <AnimatePresence>
              {passwordError && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="p-3.5 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
                    <span>{passwordError}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setPasswordError('')}
                    className="text-red-500 hover:text-red-700 font-black text-sm"
                  >
                    ✕
                  </button>
                </motion.div>
              )}

              {passwordSuccess && (
                <motion.div
                  initial={{ opacity: 0, y: -6 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -6 }}
                  className="p-3.5 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-950 text-xs font-bold flex items-center justify-between"
                >
                  <div className="flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>{passwordSuccess}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => setPasswordSuccess('')}
                    className="text-emerald-700 hover:text-emerald-950 font-black text-sm"
                  >
                    ✕
                  </button>
                </motion.div>
              )}
            </AnimatePresence>

            <form onSubmit={handleChangePassword} className="space-y-4">
              {/* Current Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Current Password *
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                  <input
                    type={showCurrentPass ? 'text' : 'password'}
                    name="currentPassword"
                    value={passwordData.currentPassword}
                    onChange={handlePasswordChange}
                    placeholder="Enter current password"
                    className="w-full pl-10 pr-10 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPass(!showCurrentPass)}
                    className="absolute right-3.5 top-2.5 text-gray-400 hover:text-gray-600"
                  >
                    {showCurrentPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* New Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  New Password *
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                  <input
                    type={showNewPass ? 'text' : 'password'}
                    name="newPassword"
                    value={passwordData.newPassword}
                    onChange={handlePasswordChange}
                    placeholder="Min 8 characters"
                    className="w-full pl-10 pr-10 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPass(!showNewPass)}
                    className="absolute right-3.5 top-2.5 text-gray-400 hover:text-gray-600"
                  >
                    {showNewPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Confirm New Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Confirm New Password *
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
                  <input
                    type={showConfirmPass ? 'text' : 'password'}
                    name="confirmPassword"
                    value={passwordData.confirmPassword}
                    onChange={handlePasswordChange}
                    placeholder="Re-type new password"
                    className="w-full pl-10 pr-10 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPass(!showConfirmPass)}
                    className="absolute right-3.5 top-2.5 text-gray-400 hover:text-gray-600"
                  >
                    {showConfirmPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={savingPassword}
                className="w-full py-3 bg-[#142E1C] hover:bg-[#0F2315] text-[#C1FF72] text-xs font-black rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-60"
              >
                {savingPassword ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Updating Password...</span>
                  </>
                ) : (
                  <>
                    <KeyRound className="w-4 h-4" />
                    <span>Update Password</span>
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Account Summary & Verification Footnote */}
          <div className="bg-[#FAF8F5] p-5 rounded-3xl border border-amber-200/60 space-y-3">
            <div className="flex items-center gap-2 text-xs font-black text-gray-800">
              <Sparkles className="w-4 h-4 text-amber-600" />
              <span>Operator Status Information</span>
            </div>
            <p className="text-[11px] text-gray-500 leading-relaxed font-medium">
              Your profile is verified and approved for automated field assignments. To update mobile number or submit new certifications, contact AgroRental Support.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default OperatorProfile;
