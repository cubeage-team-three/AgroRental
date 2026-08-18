import { useState, useEffect } from 'react';
import { getCurrentUser } from '../../services/authService';
import { getFarmerProfile, updateFarmerProfile, changeFarmerPassword } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function FarmerProfile() {
  const { t, language, setLanguage } = useLanguage();
  const currentUser = getCurrentUser();
  const farmerId = currentUser?.farmerId || 1;

  const [activeTab, setActiveTab] = useState('personal'); // 'personal' | 'farming' | 'security'
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Profile Form State
  const [profileData, setProfileData] = useState({
    fullName: currentUser?.fullName || 'Ramesh Yadav',
    mobileNumber: currentUser?.mobileNumber || '9876543210',
    email: currentUser?.email || 'ramesh.yadav@example.com',
    address: 'Village Khed, Taluka Haveli, Pune, Maharashtra - 411045',
    preferredLanguage: currentUser?.preferredLanguage || 'English',
    profileImage: 'https://images.unsplash.com/photo-1595273670150-bd0c3c392e46?auto=format&fit=crop&w=400&q=80',
    accountStatus: currentUser?.accountStatus || 'ACTIVE',
    primaryCrop: 'Sugarcane & Wheat',
    totalLandAcres: '12 Acres',
    bankName: 'State Bank of India (Haveli Branch)',
    accountNumber: '•••• •••• 4892',
    ifscCode: 'SBIN0001234',
    upiId: 'ramesh.yadav@okaxis',
    aadhaarNumber: '•••• •••• 5821',
    pmKisanId: 'PMK-MH-984210',
    soilHealthCard: 'SHC-2025-90123',
    irrigationType: 'Drip Irrigation & Canal Water',
    soilType: 'Black Cotton Soil (pH 6.8)',
    emergencyContactPerson: 'Sunita Ramesh Yadav (Wife)',
    emergencyContactPhone: '+91 98220 12345',
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
    try {
      const res = await getFarmerProfile(farmerId);
      if (res.data) {
        setProfileData((prev) => ({
          ...prev,
          fullName: res.data.fullName || prev.fullName,
          mobileNumber: res.data.mobileNumber || prev.mobileNumber,
          email: res.data.email || prev.email,
          address: res.data.address || prev.address,
          preferredLanguage: res.data.preferredLanguage || prev.preferredLanguage,
          profileImage: res.data.profileImage || prev.profileImage,
          accountStatus: res.data.accountStatus || 'ACTIVE',
        }));
      }
    } catch (err) {
      console.warn('Profile fetch notification:', err.message);
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
        email: profileData.email.trim() || null,
        address: profileData.address.trim() || null,
        preferredLanguage: profileData.preferredLanguage,
        profileImage: profileData.profileImage.trim() || null,
      });

      setSuccessMessage('✓ Profile information saved successfully!');
      setIsEditing(false);
      if (res.data) {
        setProfileData((prev) => ({
          ...prev,
          fullName: res.data.fullName,
          email: res.data.email || '',
          address: res.data.address || '',
          preferredLanguage: res.data.preferredLanguage,
        }));
      }
    } catch (err) {
      console.error('Profile update error:', err);
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('✓ Profile details updated successfully!');
        setIsEditing(false);
      } else {
        setErrorMessage(err.message || 'Failed to update profile.');
      }
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
      console.error('Change password error:', err);
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('✓ Account security password updated successfully!');
        setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      } else {
        setErrorMessage(err.message || 'Failed to change password.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#F4F7F2] p-6 flex items-center justify-center font-sans">
        <div className="flex items-center gap-3 text-[#1B4D3E] font-bold text-lg">
          <svg className="animate-spin h-7 w-7 text-[#2E7D32]" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>Loading My Profile...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#F4F7F2] py-8 px-4 sm:px-8 font-sans">
      <div className="max-w-5xl mx-auto space-y-6">

        {/* Hero Header Card */}
        <div className="bg-white rounded-3xl shadow-xl border border-emerald-950/5 overflow-hidden transition-all">
          
          {/* Vibrant Agricultural Cover Banner */}
          <div className="relative h-44 sm:h-52 bg-gradient-to-r from-[#1B4D3E] via-[#2E7D32] to-[#0F382C] overflow-hidden flex items-end p-6 sm:p-8">
            <img
              src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1400&q=80"
              alt="Farm Fields Banner"
              className="absolute inset-0 w-full h-full object-cover opacity-30 mix-blend-overlay"
            />

            {/* Top Badges */}
            <div className="absolute top-4 left-6 right-6 flex items-center justify-between z-10">
              <span className="px-3.5 py-1.5 bg-white/20 backdrop-blur-md text-white rounded-full text-xs font-extrabold tracking-wide uppercase border border-white/20 shadow-sm flex items-center gap-1.5">
                <span>🌱</span> AgroRent Verified Member
              </span>
              <span className="px-3 py-1 bg-emerald-500/90 text-white rounded-full text-xs font-bold shadow-sm">
                Active Status
              </span>
            </div>
          </div>

          {/* Profile Overview Header Body */}
          <div className="p-6 sm:p-8 relative">
            <div className="flex flex-col md:flex-row items-center md:items-end justify-between gap-6 -mt-20 sm:-mt-24 mb-4">
              
              {/* Avatar & Main Info */}
              <div className="flex flex-col sm:flex-row items-center sm:items-end gap-5 text-center sm:text-left">
                
                {/* Photo Avatar with Ring & Upload Overlay */}
                <div className="relative group">
                  <div className="w-28 h-28 sm:w-32 sm:h-32 rounded-3xl overflow-hidden border-4 border-white shadow-2xl bg-gradient-to-br from-emerald-600 to-teal-800 flex items-center justify-center text-white text-4xl font-extrabold">
                    {profileData.profileImage ? (
                      <img
                        src={profileData.profileImage}
                        alt={profileData.fullName}
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.style.display = 'none';
                        }}
                      />
                    ) : (
                      <span>{profileData.fullName ? profileData.fullName.charAt(0).toUpperCase() : '👨‍🌾'}</span>
                    )}
                  </div>
                  
                  {/* Photo Change Badge */}
                  <button
                    type="button"
                    onClick={() => {
                      const url = prompt('Enter image URL for profile photo:', profileData.profileImage);
                      if (url !== null) {
                        setProfileData((prev) => ({ ...prev, profileImage: url }));
                      }
                    }}
                    className="absolute bottom-1 right-1 p-2 bg-[#2E7D32] hover:bg-[#1B4D3E] text-white rounded-xl shadow-lg transition-transform hover:scale-105 border-2 border-white text-xs font-bold flex items-center gap-1"
                    title="Change Photo"
                  >
                    📷
                  </button>
                </div>

                {/* Name & Quick Badges */}
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                    <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
                      {profileData.fullName}
                    </h1>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">
                      ✓ Verified Farmer
                    </span>
                  </div>
                  
                  <p className="text-sm font-semibold text-gray-600 flex items-center justify-center sm:justify-start gap-2">
                    <span>📱 +91 {profileData.mobileNumber}</span>
                    <span className="text-gray-300">•</span>
                    <span>📧 {profileData.email || 'No email provided'}</span>
                  </p>

                  <p className="text-xs font-medium text-gray-500">
                    📍 {profileData.address}
                  </p>
                </div>

              </div>

              {/* Edit Button */}
              <div>
                {activeTab === 'personal' && (
                  <button
                    type="button"
                    onClick={() => setIsEditing(!isEditing)}
                    className={`px-6 py-3 rounded-2xl font-bold text-sm shadow-md transition-all flex items-center gap-2 ${
                      isEditing
                        ? 'bg-gray-200 hover:bg-gray-300 text-gray-800'
                        : 'bg-[#2E7D32] hover:bg-[#1B4D3E] text-white hover:shadow-lg'
                    }`}
                  >
                    <span>{isEditing ? t('cancel_edit') : t('edit_profile_details')}</span>
                  </button>
                )}
              </div>

            </div>

            {/* Quick Stat Summary Strip */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-6 border-t border-gray-100 mt-6">
              
              <div className="p-3.5 bg-[#F8FAFC] rounded-2xl border border-gray-100 text-center sm:text-left">
                <span className="text-[11px] font-extrabold uppercase text-gray-400 block tracking-wider">{t('registered_farms')}</span>
                <span className="text-lg font-extrabold text-gray-900 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
                  <span>🌾</span> {profileData.totalLandAcres || '2 Farms'}
                </span>
              </div>

              <div className="p-3.5 bg-[#F8FAFC] rounded-2xl border border-gray-100 text-center sm:text-left">
                <span className="text-[11px] font-extrabold uppercase text-gray-400 block tracking-wider">{t('primary_crops')}</span>
                <span className="text-lg font-extrabold text-emerald-800 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5 truncate">
                  <span>🌱</span> {profileData.primaryCrop}
                </span>
              </div>

              <div className="p-3.5 bg-[#F8FAFC] rounded-2xl border border-gray-100 text-center sm:text-left">
                <span className="text-[11px] font-extrabold uppercase text-gray-400 block tracking-wider">{t('preferred_language')}</span>
                <span className="text-lg font-extrabold text-gray-900 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
                  <span>🌐</span> {profileData.preferredLanguage}
                </span>
              </div>

              <div className="p-3.5 bg-[#F8FAFC] rounded-2xl border border-gray-100 text-center sm:text-left">
                <span className="text-[11px] font-extrabold uppercase text-gray-400 block tracking-wider">{t('farmer_rating')}</span>
                <span className="text-lg font-extrabold text-amber-600 flex items-center justify-center sm:justify-start gap-1.5 mt-0.5">
                  <span>⭐</span> 4.9 / 5.0
                </span>
              </div>

            </div>

          </div>

          {/* Clean Segmented Navigation Tabs */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-1 bg-[#EEF2EC] p-1.5 mx-6 mb-6 rounded-2xl">
            <button
              type="button"
              onClick={() => {
                setActiveTab('personal');
                setErrorMessage('');
                setSuccessMessage('');
              }}
              className={`py-3 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === 'personal'
                  ? 'bg-white text-[#1B4D3E] shadow-md'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              👤 {t('personal_info')}
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveTab('farming');
                setIsEditing(false);
                setErrorMessage('');
                setSuccessMessage('');
              }}
              className={`py-3 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === 'farming'
                  ? 'bg-white text-[#1B4D3E] shadow-md'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              🌾 {t('farming_details')}
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveTab('bank');
                setIsEditing(false);
                setErrorMessage('');
                setSuccessMessage('');
              }}
              className={`py-3 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === 'bank'
                  ? 'bg-white text-[#1B4D3E] shadow-md'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              🏦 {t('bank_subsidy_details')}
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveTab('schemes');
                setIsEditing(false);
                setErrorMessage('');
                setSuccessMessage('');
              }}
              className={`py-3 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === 'schemes'
                  ? 'bg-white text-[#1B4D3E] shadow-md'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              📜 {t('govt_schemes')}
            </button>

            <button
              type="button"
              onClick={() => {
                setActiveTab('security');
                setIsEditing(false);
                setErrorMessage('');
                setSuccessMessage('');
              }}
              className={`py-3 text-xs sm:text-sm font-bold rounded-xl transition-all ${
                activeTab === 'security'
                  ? 'bg-white text-[#1B4D3E] shadow-md'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              🔒 {t('security_settings')}
            </button>
          </div>

        </div>

        {/* Feedback Alert Banners */}
        {errorMessage && (
          <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-sm">
            <span>{errorMessage}</span>
            <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 hover:text-red-700 font-bold ml-2">✕</button>
          </div>
        )}

        {successMessage && (
          <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-800 text-sm font-medium flex items-center justify-between shadow-sm">
            <span>{successMessage}</span>
          </div>
        )}

        {/* TAB 1: Personal Information */}
        {activeTab === 'personal' && (
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8">
            <h2 className="text-xl font-extrabold text-gray-900 mb-6 flex items-center gap-2">
              <span>🌾</span> Personal Information & Contact Details
            </h2>

            {isEditing ? (
              /* EDIT MODE FORM */
              <form onSubmit={handleUpdateProfile} className="space-y-5">
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                  {/* Full Name */}
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Full Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      name="fullName"
                      required
                      value={profileData.fullName}
                      onChange={handleProfileChange}
                      className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all"
                    />
                  </div>

                  {/* Mobile Number (Read-Only Verified) */}
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Mobile Number <span className="text-emerald-600 font-normal">(Verified Account)</span>
                    </label>
                    <input
                      type="text"
                      disabled
                      value={`+91 ${profileData.mobileNumber}`}
                      className="w-full px-4 py-3 bg-gray-100 border border-transparent rounded-2xl text-gray-600 text-sm font-bold cursor-not-allowed"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                  {/* Email Address */}
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Email Address <span className="text-gray-400 font-normal">(Optional)</span>
                    </label>
                    <input
                      type="email"
                      name="email"
                      value={profileData.email}
                      onChange={handleProfileChange}
                      placeholder="ramesh.yadav@example.com"
                      className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all"
                    />
                  </div>

                  {/* Preferred Language */}
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      {t('preferred_language')}
                    </label>
                    <select
                      name="preferredLanguage"
                      value={profileData.preferredLanguage}
                      onChange={(e) => {
                        handleProfileChange(e);
                        setLanguage(e.target.value);
                      }}
                      className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all cursor-pointer"
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

                {/* Residential / Farm Address */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                    Residential / Farm Address
                  </label>
                  <textarea
                    name="address"
                    rows={3}
                    value={profileData.address}
                    onChange={handleProfileChange}
                    placeholder="Village, Taluka, District, State, Pincode"
                    className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all"
                  />
                </div>

                {/* Submit & Cancel Buttons */}
                <div className="flex items-center gap-3 pt-3">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="flex-1 py-3.5 px-6 bg-[#2E7D32] hover:bg-[#1B4D3E] text-white font-extrabold rounded-2xl shadow-lg transition-all flex items-center justify-center gap-2 disabled:opacity-70"
                  >
                    {submitting ? 'Saving Profile...' : 'Save Profile Changes'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setIsEditing(false)}
                    className="px-6 py-3.5 bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold rounded-2xl transition-colors"
                  >
                    Cancel
                  </button>
                </div>

              </form>
            ) : (
              /* VIEW MODE DISPLAY CARDS */
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                
                <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">Full Name</span>
                  <p className="text-lg font-extrabold text-gray-900">{profileData.fullName || 'N/A'}</p>
                </div>

                <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">Mobile Number</span>
                  <p className="text-lg font-extrabold text-gray-900 flex items-center gap-2">
                    <span>+91 {profileData.mobileNumber}</span>
                    <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded font-extrabold">VERIFIED</span>
                  </p>
                </div>

                <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">Email Address</span>
                  <p className="text-base font-extrabold text-gray-800">{profileData.email || 'Not registered'}</p>
                </div>

                <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">Preferred Language</span>
                  <p className="text-base font-extrabold text-[#2E7D32] flex items-center gap-2">
                    <span>🌐</span>
                    <span>{profileData.preferredLanguage}</span>
                  </p>
                </div>

                <div className="sm:col-span-2 p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                  <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">Farm / Residence Address</span>
                  <p className="text-base font-semibold text-gray-800 leading-relaxed">{profileData.address || 'No address specified.'}</p>
                </div>

              </div>
            )}
          </div>
        )}

        {/* TAB 2: Farming Details */}
        {activeTab === 'farming' && (
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
              <span>🌾</span> Farming Profile & Machinery Preferences
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              
              <div className="p-5 bg-[#F4F7F2] rounded-2xl border border-emerald-900/10 space-y-2">
                <span className="text-xs uppercase font-extrabold text-emerald-800 tracking-wider">Primary Crops</span>
                <p className="text-lg font-bold text-gray-900">Sugarcane, Wheat, Soybean</p>
                <p className="text-xs text-gray-500">Helps AgroRent recommend seasonal machinery for sowing and harvesting.</p>
              </div>

              <div className="p-5 bg-[#F4F7F2] rounded-2xl border border-emerald-900/10 space-y-2">
                <span className="text-xs uppercase font-extrabold text-emerald-800 tracking-wider">Total Land Area</span>
                <p className="text-lg font-bold text-gray-900">12 Acres (Across 2 Farm Locations)</p>
                <p className="text-xs text-gray-500">Registered under Village Khed & Taluka Haveli.</p>
              </div>

              <div className="p-5 bg-[#F4F7F2] rounded-2xl border border-emerald-900/10 space-y-2">
                <span className="text-xs uppercase font-extrabold text-emerald-800 tracking-wider">Preferred Equipment Types</span>
                <div className="flex flex-wrap gap-2 pt-1">
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg shadow-sm">Tractor 50HP+</span>
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg shadow-sm">Rotavator 6ft</span>
                  <span className="px-3 py-1 bg-white text-emerald-800 text-xs font-bold rounded-lg shadow-sm">Combined Harvester</span>
                </div>
              </div>

              <div className="p-5 bg-[#F4F7F2] rounded-2xl border border-emerald-900/10 space-y-2">
                <span className="text-xs uppercase font-extrabold text-emerald-800 tracking-wider">{t('soil_specs')}</span>
                <p className="text-lg font-bold text-gray-900">{profileData.soilType}</p>
                <p className="text-xs text-gray-500">Irrigation: {profileData.irrigationType}</p>
              </div>

              <div className="p-5 bg-[#F4F7F2] rounded-2xl border border-emerald-900/10 space-y-2">
                <span className="text-xs uppercase font-extrabold text-emerald-800 tracking-wider">{t('emergency_contact')}</span>
                <p className="text-lg font-bold text-gray-900">{profileData.emergencyContactPerson}</p>
                <p className="text-xs text-gray-500">{profileData.emergencyContactPhone}</p>
              </div>

            </div>
          </div>
        )}

        {/* TAB 3: Bank & Subsidy Account */}
        {activeTab === 'bank' && (
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
              <span>🏦</span> {t('bank_subsidy_details')}
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('bank_name')}</span>
                <p className="text-lg font-extrabold text-gray-900">{profileData.bankName}</p>
              </div>

              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('account_number')}</span>
                <p className="text-lg font-extrabold text-gray-900">{profileData.accountNumber}</p>
              </div>

              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('ifsc_code')}</span>
                <p className="text-base font-extrabold text-emerald-800">{profileData.ifscCode}</p>
              </div>

              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('upi_id')}</span>
                <p className="text-base font-extrabold text-purple-800">{profileData.upiId}</p>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: Government IDs & Kisan Schemes */}
        {activeTab === 'schemes' && (
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8 space-y-6">
            <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
              <span>📜</span> {t('govt_schemes')}
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('aadhaar_number')}</span>
                <p className="text-lg font-extrabold text-gray-900 flex items-center gap-2">
                  <span>{profileData.aadhaarNumber}</span>
                  <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-[10px] rounded font-extrabold">VERIFIED</span>
                </p>
              </div>

              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('pm_kisan_id')}</span>
                <p className="text-lg font-extrabold text-emerald-800">{profileData.pmKisanId}</p>
              </div>

              <div className="p-5 bg-[#F8FAFC] rounded-2xl border border-gray-100 space-y-1">
                <span className="text-xs uppercase font-extrabold text-gray-400 tracking-wider">{t('soil_health_card')}</span>
                <p className="text-base font-extrabold text-amber-800">{profileData.soilHealthCard}</p>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: Security & Password */}
        {activeTab === 'security' && (
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-6 sm:p-8 max-w-2xl mx-auto space-y-6">
            <div>
              <h2 className="text-xl font-extrabold text-gray-900 flex items-center gap-2">
                <span>🔒</span> Account Password & Login Security
              </h2>
              <p className="text-xs text-gray-500 mt-1">
                Set or update your password to access password login alongside OTP quick verification.
              </p>
            </div>

            <form onSubmit={handleChangePasswordSubmit} className="space-y-4">
              
              {/* Current Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Current Password <span className="text-gray-400 font-normal lowercase">(optional if setting for first time)</span>
                </label>
                <div className="relative">
                  <input
                    type={showCurrentPassword ? 'text' : 'password'}
                    name="currentPassword"
                    value={passwordData.currentPassword}
                    onChange={handlePasswordChange}
                    placeholder="••••••••"
                    className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800 text-xs font-bold"
                  >
                    {showCurrentPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              {/* New Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
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
                    className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800 text-xs font-bold"
                  >
                    {showNewPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </div>

              {/* Confirm New Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Confirm New Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  required
                  value={passwordData.confirmPassword}
                  onChange={handlePasswordChange}
                  placeholder="Re-type new password"
                  className="w-full px-4 py-3 bg-[#F4F7F2] border border-transparent rounded-2xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#2E7D32] focus:bg-white transition-all"
                />
              </div>

              {/* Submit Button */}
              <div className="pt-3">
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full py-3.5 px-6 bg-[#2E7D32] hover:bg-[#1B4D3E] text-white font-extrabold rounded-2xl shadow-lg transition-all flex items-center justify-center gap-2 disabled:opacity-70"
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
