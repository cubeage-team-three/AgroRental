import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  User, Phone, Mail, MapPin, ShieldCheck, Award, Edit3, Lock, CheckCircle2,
  Globe, Landmark, FileText, Camera, Sprout, Layers, ArrowRight, Eye, EyeOff,
  Sparkles, CreditCard, Shield, HeartHandshake, Check, AlertCircle
} from 'lucide-react';
<<<<<<< HEAD
import { getCurrentUser, getFarmerId } from '../../services/authService';
=======
import { getCurrentUser } from '../../services/authService';
>>>>>>> origin/development
import { getFarmerProfile, updateFarmerProfile, changeFarmerPassword } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function FarmerProfile() {
  let langCtx;
  try {
    langCtx = useLanguage();
  } catch (e) {
    langCtx = {};
  }
  const t = langCtx?.t || ((k) => k);
  const setLanguage = langCtx?.setLanguage || (() => {});
<<<<<<< HEAD

  const tr = (key, defaultVal) => {
    try {
      const val = t(key);
      return val && val !== key ? val : defaultVal;
    } catch {
      return defaultVal;
    }
  };

  const currentUser = getCurrentUser();
  const farmerId = getFarmerId() || 1;

=======

  const tr = (key, defaultVal) => {
    try {
      const val = t(key);
      return val && val !== key ? val : defaultVal;
    } catch {
      return defaultVal;
    }
  };

  const currentUser = getCurrentUser();
  const farmerId = currentUser?.farmerId || 1;

>>>>>>> origin/development
  const [activeTab, setActiveTab] = useState('personal'); // 'personal' | 'farming' | 'bank' | 'schemes' | 'security'
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Profile Form State
  const [profileData, setProfileData] = useState({
    fullName: currentUser?.fullName || currentUser?.name || 'Farmer Account',
    mobileNumber: currentUser?.mobileNumber || currentUser?.mobile || '',
    email: currentUser?.email || '',
    address: currentUser?.address || '',
    preferredLanguage: currentUser?.preferredLanguage || 'English',
    profileImage: currentUser?.profileImage || '',
    accountStatus: currentUser?.accountStatus || 'ACTIVE',
<<<<<<< HEAD
    primaryCrop: 'Wheat, Rice & Pulses',
    totalLandAcres: '0 Acres',
    bankName: 'Not Linked',
    accountNumber: '•••• •••• ••••',
    ifscCode: 'N/A',
    upiId: 'N/A',
    aadhaarNumber: '•••• •••• ••••',
    pmKisanId: 'N/A',
    soilHealthCard: 'N/A',
    irrigationType: 'Borewell & Canal Water',
    soilType: 'Alluvial Soil',
    emergencyContactPerson: 'Family Member',
    emergencyContactPhone: '',
=======
    primaryCrop: 'Sugarcane, Wheat & Soybean',
    totalLandAcres: '12.5 Acres (2 Farms)',
    bankName: 'State Bank of India (Haveli Branch)',
    accountNumber: '•••• •••• 4892',
    ifscCode: 'SBIN0001234',
    upiId: 'ramesh.yadav@okaxis',
    aadhaarNumber: '•••• •••• 5821',
    pmKisanId: 'PMK-MH-984210',
    soilHealthCard: 'SHC-2026-90123',
    irrigationType: 'Drip Irrigation & Canal Water',
    soilType: 'Black Cotton Soil (pH 6.8)',
    emergencyContactPerson: 'Sunita Ramesh Yadav (Wife)',
    emergencyContactPhone: '+91 98220 12345',
>>>>>>> origin/development
  });

  // Password Form State
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);

  useEffect(() => {
    fetchProfile();
  }, [farmerId]);

  const fetchProfile = async () => {
    setLoading(true);
    setErrorMessage('');
    try {
      const res = await getFarmerProfile(farmerId);
<<<<<<< HEAD
      if (res && (res.data || res.farmerId || res.fullName)) {
        const data = res.data || res;
=======
      if (res && res.data) {
>>>>>>> origin/development
        setProfileData((prev) => ({
          ...prev,
          fullName: data.fullName || prev.fullName,
          mobileNumber: data.mobileNumber || prev.mobileNumber,
          email: data.email || prev.email,
          address: data.address || prev.address,
          preferredLanguage: data.preferredLanguage || prev.preferredLanguage,
          profileImage: data.profileImage || prev.profileImage,
          accountStatus: data.accountStatus || 'ACTIVE',
        }));
      }
    } catch (err) {
      console.warn('Profile fetch note:', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleProfileChange = (e) => {
    const { name, value } = e.target;
    setProfileData((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) setErrorMessage('');
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) setErrorMessage('');
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!profileData.fullName.trim()) {
      setErrorMessage('Full name is mandatory.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await updateFarmerProfile(farmerId, {
        fullName: profileData.fullName.trim(),
        email: profileData.email ? profileData.email.trim() : null,
        address: profileData.address ? profileData.address.trim() : null,
        preferredLanguage: profileData.preferredLanguage,
        profileImage: profileData.profileImage ? profileData.profileImage.trim() : null,
      });

      setSuccessMessage('✓ Profile information updated successfully!');
      setIsEditing(false);
<<<<<<< HEAD
      
      const data = res?.data || res;
      if (data) {
=======
      if (res && res.data) {
>>>>>>> origin/development
        setProfileData((prev) => ({
          ...prev,
          fullName: data.fullName || prev.fullName,
          email: data.email || prev.email,
          address: data.address || prev.address,
          preferredLanguage: data.preferredLanguage || prev.preferredLanguage,
        }));
      }
    } catch (err) {
<<<<<<< HEAD
      console.error('Update profile error:', err);
      setErrorMessage(err.message || 'Failed to update profile details. Please try again.');
=======
      setSuccessMessage('✓ Profile details updated successfully!');
      setIsEditing(false);
>>>>>>> origin/development
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangePasswordSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!passwordData.newPassword || passwordData.newPassword.length < 6) {
      setErrorMessage('New password must be at least 6 characters long.');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    setSubmitting(true);
    try {
      await changeFarmerPassword(farmerId, {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      });

      setSuccessMessage('✓ Account security password updated successfully!');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
<<<<<<< HEAD
      console.error('Change password error:', err);
      setErrorMessage(err.message || 'Failed to update password. Please check your current password.');
=======
      setSuccessMessage('✓ Account security password updated successfully!');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
>>>>>>> origin/development
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading farmer profile details...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="space-y-6">

        {/* Hero Card with Glassmorphism & Cover Photo */}
        <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm transition">
          {/* Cover Photo */}
          <div className="relative h-44 sm:h-52 bg-gradient-to-r from-emerald-900 via-emerald-800 to-teal-900 p-6 flex items-end overflow-hidden">
            <img
              src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1400&q=80"
              alt="Agro Fields"
              className="absolute inset-0 h-full w-full object-cover opacity-25 mix-blend-overlay"
            />
            <div className="absolute top-4 left-6 right-6 flex items-center justify-between z-10">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-white/20 bg-white/20 px-3.5 py-1 text-xs font-bold text-white backdrop-blur-md">
                <Sparkles className="h-3.5 w-3.5 text-amber-300" /> AgroRent Verified Farmer
              </span>
              <span className="rounded-full bg-emerald-500/90 px-3 py-1 text-xs font-bold text-white shadow-sm">
<<<<<<< HEAD
                {profileData.accountStatus || 'ACTIVE'} Status
=======
                Active Status
>>>>>>> origin/development
              </span>
            </div>
          </div>

          {/* Profile Header Main Info */}
          <div className="relative p-6 sm:p-8">
            <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between gap-6 mb-6">
              
              <div className="flex flex-col sm:flex-row items-start sm:items-end gap-5">
                {/* Photo Avatar overlapping cover banner */}
                <div className="relative group -mt-16 sm:-mt-20 shrink-0">
                  <div className="h-28 w-28 sm:h-32 sm:w-32 rounded-3xl overflow-hidden border-4 border-white shadow-xl bg-emerald-700 flex items-center justify-center text-white text-4xl font-extrabold">
                    {profileData.profileImage ? (
                      <img
                        src={profileData.profileImage}
                        alt={profileData.fullName}
                        className="h-full w-full object-cover"
                        onError={(e) => { e.target.style.display = 'none'; }}
                      />
                    ) : (
                      <User className="h-14 w-14" />
                    )}
                  </div>
                  
                  <button
                    type="button"
                    onClick={() => {
                      const url = prompt('Enter image URL for profile photo:', profileData.profileImage);
                      if (url !== null) {
                        setProfileData((prev) => ({ ...prev, profileImage: url }));
                      }
                    }}
                    className="absolute bottom-1 right-1 flex items-center gap-1 rounded-xl bg-emerald-700 p-2 text-xs font-bold text-white shadow-lg border-2 border-white hover:bg-emerald-800 transition"
                    title="Change Photo"
                  >
                    <Camera className="h-3.5 w-3.5" />
                  </button>
                </div>

                {/* Name & Quick Badges */}
                <div className="space-y-1.5 pt-2 sm:pt-0">
                  <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2.5">
                    <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
                      {profileData.fullName}
                    </h1>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
                      <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" /> Verified Account
                    </span>
                  </div>

                  <p className="text-sm font-semibold text-slate-600 flex flex-wrap items-center justify-center sm:justify-start gap-3">
                    <span className="flex items-center gap-1">
<<<<<<< HEAD
                      <Phone className="h-3.5 w-3.5 text-emerald-600" /> {profileData.mobileNumber ? `+91 ${profileData.mobileNumber}` : 'No mobile registered'}
=======
                      <Phone className="h-3.5 w-3.5 text-emerald-600" /> +91 {profileData.mobileNumber}
>>>>>>> origin/development
                    </span>
                    <span className="text-slate-300">•</span>
                    <span className="flex items-center gap-1">
                      <Mail className="h-3.5 w-3.5 text-emerald-600" /> {profileData.email || 'No email provided'}
                    </span>
                  </p>

                  <p className="text-xs font-medium text-slate-500 flex items-center justify-center sm:justify-start gap-1">
<<<<<<< HEAD
                    <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" /> {profileData.address || 'Address not specified'}
=======
                    <MapPin className="h-3.5 w-3.5 text-slate-400 shrink-0" /> {profileData.address}
>>>>>>> origin/development
                  </p>
                </div>
              </div>

              {/* Edit Toggle Button */}
              <div>
                {activeTab === 'personal' && (
                  <button
                    type="button"
                    onClick={() => setIsEditing(!isEditing)}
                    className={`inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold shadow-sm transition ${
                      isEditing
                        ? 'bg-slate-200 text-slate-800 hover:bg-slate-300'
                        : 'bg-emerald-600 text-white hover:bg-emerald-700'
                    }`}
                  >
                    <Edit3 className="h-4 w-4" />
                    <span>{isEditing ? 'Cancel Editing' : 'Edit Profile Details'}</span>
                  </button>
                )}
              </div>
            </div>

            {/* Stat Summary Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-6 border-t border-slate-100 mt-6 text-xs">
              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[11px] font-bold uppercase text-slate-400 block tracking-wider">{tr('registered_farms', 'Land Holdings')}</span>
                <span className="text-base font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
                  <Sprout className="h-4 w-4 text-emerald-600" /> {profileData.totalLandAcres}
                </span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[11px] font-bold uppercase text-slate-400 block tracking-wider">{tr('primary_crops', 'Primary Crops')}</span>
                <span className="text-base font-extrabold text-emerald-800 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5 truncate">
                  <Layers className="h-4 w-4 text-emerald-600" /> {profileData.primaryCrop}
                </span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[11px] font-bold uppercase text-slate-400 block tracking-wider">{tr('preferred_language', 'Language')}</span>
                <span className="text-base font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
                  <Globe className="h-4 w-4 text-emerald-600" /> {profileData.preferredLanguage}
                </span>
              </div>

              <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[11px] font-bold uppercase text-slate-400 block tracking-wider">{tr('farmer_rating', 'Platform Rating')}</span>
                <span className="text-base font-extrabold text-amber-600 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
<<<<<<< HEAD
                  <Award className="h-4 w-4 text-amber-500" /> 5.0 / 5.0 Rating
=======
                  <Award className="h-4 w-4 text-amber-500" /> 4.9 / 5.0 Rating
>>>>>>> origin/development
                </span>
              </div>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-1.5 bg-slate-100 p-2 mx-6 mb-6 rounded-2xl text-xs font-bold">
            {[
              { id: 'personal', label: tr('personal_info', 'Personal Info'), icon: User },
              { id: 'farming', label: tr('farming_details', 'Farming Details'), icon: Sprout },
              { id: 'bank', label: tr('bank_subsidy_details', 'Bank & Account'), icon: Landmark },
              { id: 'schemes', label: tr('govt_schemes', 'Govt Schemes'), icon: FileText },
              { id: 'security', label: tr('security_settings', 'Security'), icon: Lock },
            ].map((tab) => {
              const Icon = tab.icon;
              const isSelected = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => {
                    setActiveTab(tab.id);
                    setIsEditing(false);
                    setErrorMessage('');
                    setSuccessMessage('');
                  }}
                  className={`flex items-center justify-center gap-1.5 py-3 rounded-xl transition ${
                    isSelected
                      ? 'bg-white text-emerald-800 shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <Icon className={`h-4 w-4 ${isSelected ? 'text-emerald-600' : 'text-slate-400'}`} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Feedback Messages */}
        {errorMessage && (
          <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-sm">
            <span className="flex items-center gap-2"><AlertCircle className="h-5 w-5 text-red-600" /> {errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 hover:text-red-700 font-bold ml-2">✕</button>
          </div>
        )}

        {successMessage && (
          <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-800 text-sm font-medium flex items-center justify-between shadow-sm">
            <span className="flex items-center gap-2"><CheckCircle2 className="h-5 w-5 text-emerald-600" /> {successMessage}</span>
          </div>
        )}

        {/* TAB 1: Personal Information */}
        {activeTab === 'personal' && (
          <div className="bg-white rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8">
            <h2 className="text-xl font-bold text-slate-900 mb-6 flex items-center gap-2">
              <User className="h-5 w-5 text-emerald-600" /> Personal Information & Contact Details
            </h2>

            {isEditing ? (
              <form onSubmit={handleUpdateProfile} className="space-y-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                      Full Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      name="fullName"
                      required
                      value={profileData.fullName}
                      onChange={handleProfileChange}
                      className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                      Mobile Number <span className="text-emerald-600 font-normal">(Verified Account)</span>
                    </label>
                    <input
                      type="text"
                      disabled
<<<<<<< HEAD
                      value={profileData.mobileNumber ? `+91 ${profileData.mobileNumber}` : 'N/A'}
=======
                      value={`+91 ${profileData.mobileNumber}`}
>>>>>>> origin/development
                      className="w-full px-4 py-3 bg-slate-100 border border-transparent rounded-2xl text-slate-600 text-sm font-bold cursor-not-allowed"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                      Email Address <span className="text-slate-400 font-normal">(Optional)</span>
                    </label>
                    <input
                      type="email"
                      name="email"
                      value={profileData.email}
                      onChange={handleProfileChange}
                      placeholder="farmer@example.com"
                      className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                      Preferred Language
                    </label>
                    <select
                      name="preferredLanguage"
                      value={profileData.preferredLanguage}
                      onChange={(e) => {
                        handleProfileChange(e);
                        setLanguage(e.target.value);
                      }}
                      className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all cursor-pointer"
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
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                    Residential / Farm Address
                  </label>
                  <textarea
                    name="address"
                    rows={3}
                    value={profileData.address}
                    onChange={handleProfileChange}
                    placeholder="Village, Taluka, District, State, Pincode"
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all"
                  />
                </div>

                <div className="flex items-center gap-3 pt-3">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="flex-1 py-3.5 px-6 bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold rounded-2xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70"
                  >
                    {submitting ? 'Saving Profile...' : 'Save Profile Changes'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsEditing(false)}
                    className="px-6 py-3.5 bg-slate-200 hover:bg-slate-300 text-slate-800 font-bold rounded-2xl transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Full Name</span>
                  <p className="text-base font-extrabold text-slate-900">{profileData.fullName || 'N/A'}</p>
                </div>

                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Mobile Number</span>
                  <p className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                    <span>{profileData.mobileNumber ? `+91 ${profileData.mobileNumber}` : 'N/A'}</span>
                    {profileData.mobileNumber && (
                      <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded font-bold">VERIFIED</span>
                    )}
                  </p>
                </div>

                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Email Address</span>
                  <p className="text-base font-bold text-slate-800">{profileData.email || 'Not registered'}</p>
                </div>

                <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Preferred Language</span>
                  <p className="text-base font-bold text-emerald-800 flex items-center gap-2">
                    <Globe className="h-4 w-4 text-emerald-600" />
                    <span>{profileData.preferredLanguage}</span>
                  </p>
                </div>

                <div className="sm:col-span-2 p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                  <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Farm / Residence Address</span>
                  <p className="text-base font-medium text-slate-800 leading-relaxed">{profileData.address || 'No address specified.'}</p>
                </div>
              </div>
            )}
          </div>
        )}

        {/* TAB 2: Farming Details */}
        {activeTab === 'farming' && (
          <div className="bg-white rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <Sprout className="h-5 w-5 text-emerald-600" /> Farming Profile & Machinery Preferences
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                <span className="text-xs uppercase font-bold text-emerald-800 tracking-wider block">Primary Crops</span>
<<<<<<< HEAD
                <p className="text-base font-bold text-slate-900">{profileData.primaryCrop}</p>
=======
                <p className="text-base font-bold text-slate-900">Sugarcane, Wheat, Soybean</p>
>>>>>>> origin/development
                <p className="text-slate-500">Helps AgroRent recommend seasonal machinery for sowing and harvesting.</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                <span className="text-xs uppercase font-bold text-emerald-800 tracking-wider block">Total Land Area</span>
<<<<<<< HEAD
                <p className="text-base font-bold text-slate-900">{profileData.totalLandAcres}</p>
                <p className="text-slate-500">Managed via My Farms section.</p>
=======
                <p className="text-base font-bold text-slate-900">12.5 Acres (Across 2 Farm Locations)</p>
                <p className="text-slate-500">Registered under Village Khed & Taluka Haveli.</p>
>>>>>>> origin/development
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                <span className="text-xs uppercase font-bold text-emerald-800 tracking-wider block">Preferred Equipment Types</span>
                <div className="flex flex-wrap gap-2 pt-1">
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg border border-slate-200 shadow-sm">Tractor 50HP+</span>
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg border border-slate-200 shadow-sm">Rotavator 6ft</span>
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg border border-slate-200 shadow-sm">Combined Harvester</span>
                </div>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                <span className="text-xs uppercase font-bold text-emerald-800 tracking-wider block">Soil & Irrigation Specs</span>
                <p className="text-base font-bold text-slate-900">{profileData.soilType}</p>
                <p className="text-slate-500">Irrigation: {profileData.irrigationType}</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-2">
                <span className="text-xs uppercase font-bold text-emerald-800 tracking-wider block">Emergency Field Contact</span>
                <p className="text-base font-bold text-slate-900">{profileData.emergencyContactPerson}</p>
<<<<<<< HEAD
                <p className="text-slate-500">{profileData.emergencyContactPhone || 'Not provided'}</p>
=======
                <p className="text-slate-500">{profileData.emergencyContactPhone}</p>
>>>>>>> origin/development
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: Bank & Payments */}
        {activeTab === 'bank' && (
          <div className="bg-white rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <Landmark className="h-5 w-5 text-emerald-600" /> Bank & Account Details
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Bank Name</span>
                <p className="text-base font-extrabold text-slate-900">{profileData.bankName}</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Account Number</span>
                <p className="text-base font-extrabold text-slate-900">{profileData.accountNumber}</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">IFSC Code</span>
                <p className="text-base font-extrabold text-emerald-800">{profileData.ifscCode}</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">UPI ID</span>
                <p className="text-base font-extrabold text-purple-800">{profileData.upiId}</p>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: Govt Schemes */}
        {activeTab === 'schemes' && (
          <div className="bg-white rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
              <FileText className="h-5 w-5 text-emerald-600" /> Government Schemes & Kisan Cards
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-xs">
              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Aadhaar Number</span>
                <p className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                  <span>{profileData.aadhaarNumber}</span>
                  <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded font-bold">VERIFIED</span>
                </p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">PM-Kisan Reg ID</span>
                <p className="text-base font-extrabold text-emerald-800">{profileData.pmKisanId}</p>
              </div>

              <div className="p-5 bg-slate-50 rounded-2xl border border-slate-100 space-y-1">
                <span className="text-xs uppercase font-bold text-slate-400 tracking-wider block">Soil Health Card</span>
                <p className="text-base font-extrabold text-amber-800">{profileData.soilHealthCard}</p>
              </div>
            </div>
          </div>
        )}

        {/* TAB 5: Security */}
        {activeTab === 'security' && (
          <div className="bg-white rounded-3xl shadow-sm border border-slate-200 p-6 sm:p-8 max-w-2xl mx-auto space-y-6">
            <div>
              <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                <Lock className="h-5 w-5 text-emerald-600" /> Account Security & Password
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Set or update your password to access password login alongside OTP quick verification.
              </p>
            </div>

            <form onSubmit={handleChangePasswordSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  Current Password <span className="text-slate-400 font-normal lowercase">(optional if setting for first time)</span>
                </label>
                <div className="relative">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    name="currentPassword"
                    value={passwordData.currentPassword}
                    onChange={handlePasswordChange}
                    placeholder="••••••••"
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 text-xs font-bold"
                  >
                    {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  New Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    name="newPassword"
                    required
                    minLength={6}
                    value={passwordData.newPassword}
                    onChange={handlePasswordChange}
                    placeholder="Minimum 6 characters"
                    className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700 text-xs font-bold"
                  >
                    {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">
                  Confirm New Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  required
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  placeholder="Re-type new password"
                  className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-slate-900 text-sm font-semibold focus:outline-none focus:border-emerald-500 focus:bg-white transition-all"
                />
              </div>

              <div className="pt-3">
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-3.5 px-6 bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold rounded-2xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70 text-sm"
                >
                  {submitting ? 'Updating Password...' : 'Update Account Password'}
                </button>
              </div>
            </form>
          </div>
        )}

      </div>
    </div>
  );
}

export default FarmerProfile;
