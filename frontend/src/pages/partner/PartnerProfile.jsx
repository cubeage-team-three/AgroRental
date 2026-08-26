import { useState, useEffect } from 'react';
import {
  User,
  Building2,
  Phone,
  Mail,
  MapPin,
  ShieldCheck,
  BadgeCheck,
  Clock,
  KeyRound,
  FileText,
  Camera,
  CheckCircle2,
  AlertCircle,
  Edit3,
  X,
  Save,
  Tractor,
  Calendar,
  Sparkles,
  Lock,
  Eye,
  EyeOff,
  RefreshCw,
} from 'lucide-react';
import { getCurrentUser, getPartnerId } from '../../services/authService';
import { partnerService } from '../../services/partnerService';
import { equipmentService } from '../../services/equipmentService';
import { reviewService } from '../../services/reviewService';

function PartnerProfile() {
  const currentUser = getCurrentUser();
  const partnerId = getPartnerId();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);

  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'edit' | 'security' | 'kyc'
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  const [partner, setPartner] = useState(null);
  const [equipmentCount, setEquipmentCount] = useState(0);
  const [ratingSummary, setRatingSummary] = useState(null);

  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  // Profile Edit Form State
  const [formData, setFormData] = useState({
    fullName: '',
    businessName: '',
    email: '',
    address: '',
    gstNumber: '',
    aadhaarNumber: '',
    panNumber: '',
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

  // Load Partner Profile, Rating Summary & Listed Equipment
  const fetchPartnerData = async () => {
    if (!partnerId) {
      setErrorMessage('No partner session active. Please log in.');
      setLoading(false);
      return;
    }

    setLoading(true);
    setErrorMessage('');

    try {
      const [profileData, eqList, ratingRes] = await Promise.allSettled([
        partnerService.getProfile(partnerId),
        equipmentService.getPartnerEquipment(partnerId),
        reviewService.getPartnerRatingSummary(partnerId),
      ]);

      if (profileData.status === 'fulfilled' && profileData.value) {
        const data = profileData.value;
        setPartner(data);
        setFormData({
          fullName: data.fullName || '',
          businessName: data.businessName || '',
          email: data.email || '',
          address: data.address || '',
          gstNumber: data.gstNumber || '',
          aadhaarNumber: data.aadhaarNumber || '',
          panNumber: data.panNumber || '',
          profilePhoto: data.profilePhoto || '',
        });
      } else if (profileData.status === 'rejected') {
        const reason = profileData.reason;
        setErrorMessage(reason?.response?.data?.message || reason?.message || 'Failed to retrieve partner profile.');
      }

      if (eqList.status === 'fulfilled' && Array.isArray(eqList.value)) {
        setEquipmentCount(eqList.value.length);
      }

      if (ratingRes.status === 'fulfilled' && ratingRes.value) {
        setRatingSummary(ratingRes.value);
      }
    } catch (err) {
      console.error('Failed to load partner profile:', err);
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to retrieve partner details from server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPartnerData();
  }, [partnerId]);

  // Handle Form Change
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errorMessage) setErrorMessage('');
  };

  const handlePasswordInputChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errorMessage) setErrorMessage('');
  };

  // Submit Profile Update
  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    setSaving(true);

    try {
      const res = await partnerService.updateProfile(partnerId, {
        fullName: formData.fullName.trim(),
        businessName: formData.businessName.trim() || null,
        email: formData.email.trim() || null,
        address: formData.address.trim() || null,
        gstNumber: formData.gstNumber.trim() || null,
        aadhaarNumber: formData.aadhaarNumber.trim() || null,
        panNumber: formData.panNumber.trim() || null,
        profilePhoto: formData.profilePhoto.trim() || null,
      });

      const updatedData = res.data || res;
      setPartner(updatedData);
      setSuccessMessage('✓ Partner business profile updated successfully!');
      setIsEditModalOpen(false);
      setActiveTab('overview');
    } catch (err) {
      console.error('Failed to update partner profile:', err);
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to update partner profile. Please check the fields and try again.');
    } finally {
      setSaving(false);
    }
  };

  // Submit Password Change
  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!passwordData.currentPassword) {
      setErrorMessage('Current password is required.');
      return;
    }

    if (!passwordData.newPassword || passwordData.newPassword.length < 6) {
      setErrorMessage('New password must be at least 6 characters long.');
      return;
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setErrorMessage('New password and confirm password do not match.');
      return;
    }

    setChangingPassword(true);

    try {
      await partnerService.changePassword(partnerId, {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      });

      setSuccessMessage('✓ Account security password updated successfully!');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      console.error('Failed to change password:', err);
      setErrorMessage(err.response?.data?.message || err.message || 'Failed to update password. Verify current password is correct.');
    } finally {
      setChangingPassword(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <div className="flex items-center gap-3 text-[#1B4D3E] font-bold text-base bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
          <RefreshCw className="w-6 h-6 animate-spin text-[#3E7B27]" />
          <span>Loading Partner Profile...</span>
        </div>
      </div>
    );
  }

  const partnerName = partner?.fullName || 'Partner Owner';
  const businessName = partner?.businessName || 'Agro Machinery Services';
  const mobileNumber = partner?.mobileNumber || 'Not provided';
  const email = partner?.email || 'Not provided';
  const address = partner?.address || 'Operational hub address not set';
  const verificationStatus = partner?.verificationStatus || 'PENDING';
  const profilePhoto = partner?.profilePhoto || null;
  const createdAtFormatted = partner?.createdAt
    ? new Date(partner.createdAt).toLocaleDateString('en-IN', {
        month: 'short',
        year: 'numeric',
      })
    : 'Aug 2026';

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      
      {/* Top Breadcrumb & Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#142E1C] tracking-tight">
            Partner Profile & Operations
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Manage your registered machinery enterprise, verification documents, and account security.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsEditModalOpen(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-bold rounded-xl shadow-md transition-all duration-150"
          >
            <Edit3 className="w-4 h-4" />
            <span>Edit Profile</span>
          </button>
        </div>
      </div>

      {/* Hero Profile Header Card */}
      <div className="bg-white rounded-3xl shadow-lg border border-gray-100 overflow-hidden">
        
        {/* Agricultural Pattern Cover Banner */}
        <div className="relative h-36 sm:h-44 bg-gradient-to-r from-[#142E1C] via-[#1B4D3E] to-[#2E7D32] overflow-hidden flex items-end p-6">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1400&q=80"
            alt="Agro Fields Cover"
            className="absolute inset-0 w-full h-full object-cover opacity-25 mix-blend-overlay"
          />

          <div className="absolute top-4 left-6 right-6 flex items-center justify-between z-10">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-white/20 backdrop-blur-md text-white rounded-full text-xs font-bold uppercase tracking-wider border border-white/20">
              <Sparkles className="w-3.5 h-3.5 text-lime-300" />
              <span>AgroRent Verified Partner</span>
            </span>

            <span
              className={`px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider shadow-sm ${
                verificationStatus === 'APPROVED'
                  ? 'bg-emerald-500 text-white'
                  : 'bg-amber-500 text-white'
              }`}
            >
              {verificationStatus === 'APPROVED' ? '✓ Account Active' : '⏳ Verification Pending'}
            </span>
          </div>
        </div>

        {/* Profile Card Body */}
        <div className="p-6 sm:p-8">
          <div className="flex flex-col sm:flex-row items-start sm:items-end justify-between gap-6 -mt-16 sm:-mt-20 mb-6">
            
            {/* Avatar & Identifiers */}
            <div className="flex flex-col sm:flex-row items-center sm:items-end gap-5 text-center sm:text-left">
              
              {/* Photo Avatar with Ring */}
              <div className="relative group shrink-0">
                <div className="w-24 h-24 sm:w-28 sm:h-28 rounded-2xl overflow-hidden border-4 border-white shadow-xl bg-gradient-to-br from-emerald-600 to-teal-800 flex items-center justify-center text-white text-3xl font-black">
                  {profilePhoto ? (
                    <img
                      src={profilePhoto}
                      alt={partnerName}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.style.display = 'none';
                      }}
                    />
                  ) : (
                    <span>{partnerName.charAt(0).toUpperCase()}</span>
                  )}
                </div>

                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(true)}
                  className="absolute bottom-0 right-0 p-1.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white rounded-xl shadow-md border-2 border-white transition-transform hover:scale-105"
                  title="Change Avatar URL"
                >
                  <Camera className="w-3.5 h-3.5" />
                </button>
              </div>

              {/* Names & Contact */}
              <div className="space-y-1">
                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2">
                  <h2 className="text-xl sm:text-2xl font-black text-gray-900 tracking-tight">
                    {partnerName}
                  </h2>
                  {verificationStatus === 'APPROVED' && (
                    <BadgeCheck className="w-5 h-5 text-emerald-600 shrink-0" title="Verified Partner" />
                  )}
                </div>

                <p className="text-sm font-bold text-[#2E6F22] flex items-center justify-center sm:justify-start gap-1.5">
                  <Building2 className="w-4 h-4" />
                  <span>{businessName}</span>
                </p>

                <div className="flex flex-wrap items-center justify-center sm:justify-start gap-3 text-xs font-semibold text-gray-500 pt-0.5">
                  <span className="flex items-center gap-1">
                    <Phone className="w-3.5 h-3.5 text-gray-400" />
                    +91 {mobileNumber}
                  </span>
                  <span className="text-gray-300">•</span>
                  <span className="flex items-center gap-1">
                    <Mail className="w-3.5 h-3.5 text-gray-400" />
                    {email}
                  </span>
                </div>
              </div>

            </div>

            {/* Quick Stat Pill */}
            <div className="w-full sm:w-auto flex sm:flex-col items-center justify-between sm:items-end gap-2 bg-[#F7F6F0] p-3.5 rounded-2xl border border-gray-200/60">
              <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400">Partner Since</span>
              <span className="text-sm font-extrabold text-[#142E1C] flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-[#3E7B27]" />
                {createdAtFormatted}
              </span>
            </div>

          </div>

          {/* Quick Metrics Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-6 border-t border-gray-100">
            
            <div className="p-4 bg-[#F8FAF8] rounded-2xl border border-emerald-900/5">
              <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block">Fleet Machinery</span>
              <div className="flex items-center gap-2 mt-1">
                <Tractor className="w-5 h-5 text-[#3E7B27]" />
                <span className="text-xl font-black text-gray-900">{equipmentCount} Units</span>
              </div>
            </div>

            <div className="p-4 bg-[#F8FAF8] rounded-2xl border border-emerald-900/5">
              <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block">Partner Status</span>
              <div className="flex items-center gap-1.5 mt-1">
                <ShieldCheck className="w-5 h-5 text-emerald-600" />
                <span className="text-sm font-extrabold text-emerald-900 uppercase">
                  {verificationStatus}
                </span>
              </div>
            </div>
<div className="p-4 bg-[#F8FAF8] rounded-2xl border border-emerald-900/5">
  <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block">
    OTP Verification
  </span>

  <div className="flex items-center gap-1.5 mt-1">
    {partner?.otpVerified ? (
      <CheckCircle2 className="w-5 h-5 text-emerald-600" />
    ) : (
      <AlertCircle className="w-5 h-5 text-amber-500" />
    )}

    <span className="text-sm font-extrabold text-gray-800">
      {partner?.otpVerified ? 'Verified' : 'Not Verified'}
    </span>
  </div>
</div>
            <div className="p-4 bg-[#F8FAF8] rounded-2xl border border-emerald-900/5">
              <span className="text-[10px] font-black uppercase tracking-wider text-gray-400 block">Partner Rating</span>
              <div className="flex items-center gap-1.5 mt-1">
                <span className="text-amber-500 font-bold">★</span>
                <span className="text-base font-extrabold text-gray-900">
                  {ratingSummary?.averageRating ? `${ratingSummary.averageRating.toFixed(1)} / 5.0` : '0.0 / 5.0'}
                </span>
              </div>
            </div>

          </div>

        </div>

        {/* Tabbed Navigation Bar */}
        <div className="flex items-center gap-2 bg-[#F0EFE9] px-6 py-2 border-t border-gray-200">
          <button
            type="button"
            onClick={() => setActiveTab('overview')}
            className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
              activeTab === 'overview'
                ? 'bg-white text-[#142E1C] shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            👤 Personal & Business Info
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('security')}
            className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
              activeTab === 'security'
                ? 'bg-white text-[#142E1C] shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            🔒 Password & Security
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('kyc')}
            className={`px-4 py-2 rounded-xl text-xs font-extrabold transition-all ${
              activeTab === 'kyc'
                ? 'bg-white text-[#142E1C] shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            📜 KYC & Legal Verification
          </button>
        </div>

      </div>

      {/* Alerts */}
      {errorMessage && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            <span>{errorMessage}</span>
          </div>
          <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 hover:text-red-700 font-bold ml-2">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMessage && (
        <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-800 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-600" />
            <span>{successMessage}</span>
          </div>
        </div>
      )}

      {/* TAB 1: Overview (Personal & Business Information) */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Information Panels (Left 2 Cols) */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Personal Details */}
            <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-gray-100 space-y-4">
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <h3 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
                  <User className="w-4 h-4 text-[#3E7B27]" />
                  <span>Personal Information</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(true)}
                  className="text-xs font-bold text-[#3E7B27] hover:underline"
                >
                  Edit
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="p-4 bg-[#F8FAF8] rounded-2xl">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Full Name</span>
                  <span className="text-sm font-extrabold text-gray-900 mt-0.5 block">{partnerName}</span>
                </div>

                <div className="p-4 bg-[#F8FAF8] rounded-2xl">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Mobile Number</span>
                  <span className="text-sm font-extrabold text-gray-900 mt-0.5 flex items-center gap-1.5">
                    <span>+91 {mobileNumber}</span>
                    <span className="px-1.5 py-0.2 bg-emerald-100 text-emerald-800 text-[9px] font-black rounded">VERIFIED</span>
                  </span>
                </div>

                <div className="sm:col-span-2 p-4 bg-[#F8FAF8] rounded-2xl">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Registered Email</span>
                  <span className="text-sm font-extrabold text-gray-900 mt-0.5 block">{email}</span>
                </div>
              </div>
            </div>

            {/* Business Enterprise Details */}
            <div className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-gray-100 space-y-4">
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <h3 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
                  <Building2 className="w-4 h-4 text-[#3E7B27]" />
                  <span>Business & Operational Enterprise</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(true)}
                  className="text-xs font-bold text-[#3E7B27] hover:underline"
                >
                  Edit
                </button>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="sm:col-span-2 p-4 bg-[#F8FAF8] rounded-2xl">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Enterprise Name</span>
                  <span className="text-base font-extrabold text-[#142E1C] mt-0.5 block">{businessName}</span>
                </div>

                <div className="sm:col-span-2 p-4 bg-[#F8FAF8] rounded-2xl">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-gray-400 block">Operational Hub / Base Address</span>
                  <p className="text-sm font-semibold text-gray-800 mt-0.5 leading-relaxed">{address}</p>
                </div>
              </div>
            </div>

          </div>

          {/* Right Sidebar KYC & Actions Card */}
          <div className="space-y-6">
            
            {/* Legal & KYC Snapshot */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
              <h3 className="text-base font-extrabold text-gray-900 flex items-center gap-2 border-b border-gray-100 pb-3">
                <FileText className="w-4 h-4 text-[#3E7B27]" />
                <span>Legal & Tax IDs</span>
              </h3>

              <div className="space-y-3 text-xs">
                <div className="flex items-center justify-between p-3 bg-[#F8FAF8] rounded-xl">
                  <span className="text-gray-500 font-semibold">GST Number</span>
                  <span className="font-mono font-bold text-gray-800">
                    {partner?.gstNumber || 'Not Registered'}
                  </span>
                </div>

                <div className="flex items-center justify-between p-3 bg-[#F8FAF8] rounded-xl">
                  <span className="text-gray-500 font-semibold">PAN Card</span>
                  <span className="font-mono font-bold text-gray-800">
                    {partner?.panNumber || 'Not Provided'}
                  </span>
                </div>

                <div className="flex items-center justify-between p-3 bg-[#F8FAF8] rounded-xl">
                  <span className="text-gray-500 font-semibold">Aadhaar Card</span>
                  <span className="font-mono font-bold text-gray-800">
                    {partner?.aadhaarNumber ? `•••• •••• ${partner.aadhaarNumber.slice(-4)}` : 'Not Provided'}
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Actions Card */}
            <div className="bg-gradient-to-br from-[#142E1C] to-[#2E7D32] rounded-3xl p-6 text-white space-y-4 shadow-md">
              <h3 className="text-base font-bold flex items-center gap-2">
                <Tractor className="w-5 h-5 text-lime-300" />
                <span>Machinery Inventory</span>
              </h3>
              <p className="text-xs text-emerald-100">
                You have {equipmentCount} listed farm equipment ready for rental bookings in your regional area.
              </p>
              <div className="pt-2 flex flex-col gap-2">
                <a
                  href="/partner/equipment"
                  className="w-full py-2.5 text-center bg-white text-[#142E1C] hover:bg-lime-300 rounded-xl text-xs font-black transition-colors shadow-xs"
                >
                  View My Listings
                </a>
                <a
                  href="/partner/equipment/add"
                  className="w-full py-2.5 text-center bg-white/15 hover:bg-white/25 text-white rounded-xl text-xs font-bold transition-colors"
                >
                  + Add New Machinery
                </a>
              </div>
            </div>

          </div>

        </div>
      )}

      {/* TAB 2: Password & Security */}
      {activeTab === 'security' && (
        <div className="max-w-2xl bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-extrabold text-gray-900 flex items-center gap-2">
              <KeyRound className="w-5 h-5 text-[#3E7B27]" />
              <span>Change Account Password</span>
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              Ensure your Partner account is using a strong password for machinery operations and booking approvals.
            </p>
          </div>

          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            
            {/* Current Password */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Current Password <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <input
                  type={showCurrentPass ? 'text' : 'password'}
                  name="currentPassword"
                  required
                  value={passwordData.currentPassword}
                  onChange={handlePasswordInputChange}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowCurrentPass(!showCurrentPass)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800 text-xs"
                >
                  {showCurrentPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* New Password */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                New Password <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <input
                  type={showNewPass ? 'text' : 'password'}
                  name="newPassword"
                  required
                  minLength={6}
                  value={passwordData.newPassword}
                  onChange={handlePasswordInputChange}
                  placeholder="Minimum 6 characters"
                  className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowNewPass(!showNewPass)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800 text-xs"
                >
                  {showNewPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Confirm New Password */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Confirm New Password <span className="text-red-500">*</span>
              </label>
              <input
                type="password"
                name="confirmPassword"
                required
                value={passwordData.confirmPassword}
                onChange={handlePasswordInputChange}
                placeholder="Re-type new password"
                className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            <div className="pt-3">
              <button
                type="submit"
                disabled={changingPassword}
                className="w-full py-3.5 px-6 bg-[#3E7B27] hover:bg-[#2E6F22] text-white font-extrabold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70"
              >
                {changingPassword ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Updating Password...</span>
                  </>
                ) : (
                  <>
                    <Lock className="w-4 h-4" />
                    <span>Update Password</span>
                  </>
                )}
              </button>
            </div>

          </form>
        </div>
      )}

      {/* TAB 3: KYC & Legal Verification */}
      {activeTab === 'kyc' && (
        <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 space-y-6">
          <div className="border-b border-gray-100 pb-4">
            <h3 className="text-lg font-extrabold text-gray-900 flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-emerald-600" />
              <span>Partner KYC & Business Verification Status</span>
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              AgroRent partners require verified government and tax documents to enable high-value equipment rental payouts.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            
            <div className="p-5 bg-[#F8FAF8] rounded-2xl border border-emerald-900/10 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase text-gray-500">Aadhaar Verification</span>
                <span
  className={`px-2 py-0.5 text-[10px] font-extrabold rounded ${
    partner?.verificationStatus === 'APPROVED'
      ? 'bg-emerald-100 text-emerald-800'
      : 'bg-amber-100 text-amber-800'
  }`}
>
  {partner?.verificationStatus === 'APPROVED' ? 'VERIFIED' : 'PENDING'}
</span>
              </div>
              <p className="font-mono font-bold text-sm text-gray-900">
                {partner?.aadhaarNumber ? `•••• •••• ${partner.aadhaarNumber.slice(-4)}` : '•••• •••• 9012'}
              </p>
              <p className="text-[11px] text-gray-500">Identity and primary residency confirmed.</p>
            </div>

            <div className="p-5 bg-[#F8FAF8] rounded-2xl border border-emerald-900/10 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase text-gray-500">PAN Card</span>
                <span
  className={`px-2 py-0.5 text-[10px] font-extrabold rounded ${
    partner?.verificationStatus === 'APPROVED'
      ? 'bg-emerald-100 text-emerald-800'
      : 'bg-amber-100 text-amber-800'
  }`}
>
  {partner?.verificationStatus === 'APPROVED' ? 'VERIFIED' : 'PENDING'}
</span>
              </div>
              <p className="font-mono font-bold text-sm text-gray-900">
                {partner?.panNumber || 'ABCDE1234F'}
              </p>
              <p className="text-[11px] text-gray-500">Tax identification verified for banking receipts.</p>
            </div>

            <div className="p-5 bg-[#F8FAF8] rounded-2xl border border-emerald-900/10 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase text-gray-500">GST Registration</span>
                <span className="px-2 py-0.5 bg-amber-100 text-amber-800 text-[10px] font-extrabold rounded">OPTIONAL</span>
              </div>
              <p className="font-mono font-bold text-sm text-gray-900">
                {partner?.gstNumber || 'Not Registered'}
              </p>
              <p className="text-[11px] text-gray-500">Required for commercial equipment fleets with GST billing.</p>
            </div>

          </div>

        </div>
      )}

      {/* EDIT PROFILE MODAL (Figma-style) */}
      {isEditModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs overflow-y-auto">
          <div className="bg-white w-full max-w-2xl rounded-3xl shadow-2xl border border-gray-100 overflow-hidden my-8">
            
            {/* Modal Header */}
            <div className="bg-[#142E1C] px-6 py-4 text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Edit3 className="w-5 h-5 text-lime-300" />
                <h3 className="text-lg font-bold">Edit Partner Business Profile</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsEditModalOpen(false)}
                className="p-1 rounded-lg text-gray-300 hover:text-white hover:bg-white/10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleProfileSubmit} className="p-6 sm:p-8 space-y-5 max-h-[75vh] overflow-y-auto">
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                
                {/* Full Name */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Full Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    name="fullName"
                    required
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="e.g. Rajesh Patel"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

                {/* Business Name */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Business / Enterprise Name
                  </label>
                  <input
                    type="text"
                    name="businessName"
                    value={formData.businessName}
                    onChange={handleInputChange}
                    placeholder="e.g. Patel Agro Fleet"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

                {/* Mobile (Read-Only) */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Mobile Number <span className="text-xs font-normal text-gray-400">(Registered ID)</span>
                  </label>
                  <input
                    type="text"
                    disabled
                    value={`+91 ${mobileNumber}`}
                    className="w-full px-3.5 py-2.5 bg-gray-100 text-gray-500 border border-transparent rounded-xl text-sm font-bold cursor-not-allowed"
                  />
                </div>

                {/* Email Address */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Email Address
                  </label>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleInputChange}
                    placeholder="rajesh.patel@example.com"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

                {/* Profile Photo Avatar URL */}
                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Profile Photo / Avatar URL
                  </label>
                  <input
                    type="url"
                    name="profilePhoto"
                    value={formData.profilePhoto}
                    onChange={handleInputChange}
                    placeholder="https://images.unsplash.com/..."
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                  <p className="text-[10px] text-gray-500 mt-1">
                    Provide a valid HTTPS image link for your partner avatar badge.
                  </p>
                </div>

                {/* Address */}
                <div className="sm:col-span-2">
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    Operational Hub / Address
                  </label>
                  <textarea
                    name="address"
                    rows={2}
                    value={formData.address}
                    onChange={handleInputChange}
                    placeholder="Plot / Street, APMC Market, Taluka, District, State, Pincode"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

                {/* GST Number */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    GST Number (Optional)
                  </label>
                  <input
                    type="text"
                    name="gstNumber"
                    value={formData.gstNumber}
                    onChange={handleInputChange}
                    placeholder="27AABCP1234F1Z5"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

                {/* PAN Number */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1.5">
                    PAN Number (Optional)
                  </label>
                  <input
                    type="text"
                    name="panNumber"
                    value={formData.panNumber}
                    onChange={handleInputChange}
                    placeholder="ABCDE1234F"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
                  />
                </div>

              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-6 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center gap-1.5 disabled:opacity-70"
                >
                  {saving ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Saving Profile...</span>
                    </>
                  ) : (
                    <>
                      <Save className="w-4 h-4" />
                      <span>Save Changes</span>
                    </>
                  )}
                </button>
              </div>

            </form>

          </div>
        </div>
      )}

    </div>
  );
}

export default PartnerProfile;
