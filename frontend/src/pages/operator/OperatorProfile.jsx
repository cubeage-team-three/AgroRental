import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getAuthUser, isAuthenticated, saveAuth } from "../../utils/auth";
import {
  getOperatorProfile,
  updateOperatorProfile,
  uploadOperatorProfilePhoto,
} from "../../services/operatorService";

function OperatorProfile() {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [profile, setProfile] = useState(null);
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    address: "",
    experience: 0,
    skills: "",
  });

  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
      return;
    }
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getOperatorProfile();
      setProfile(data);
      setFormData({
        fullName: data.fullName || "",
        email: data.email || "",
        address: data.address || "",
        experience: data.experience !== undefined ? data.experience : 0,
        skills: data.skills || "",
      });

      // Synchronize updated user profile with localStorage auth
      const currentUser = getAuthUser() || {};
      saveAuth(null, {
        ...currentUser,
        id: data.id,
        fullName: data.fullName,
        mobileNumber: data.mobileNumber,
        email: data.email,
        address: data.address,
        experience: data.experience,
        skills: data.skills,
        profilePhoto: data.profilePhoto,
        status: data.status,
      });
    } catch (err) {
      console.error("Error fetching operator profile:", err);
      setError(err.message || "Failed to load profile details from server");

      // Fallback from localStorage cached user if server is loading or offline
      const cached = getAuthUser();
      if (cached) {
        setProfile(cached);
        setFormData({
          fullName: cached.fullName || "",
          email: cached.email || "",
          address: cached.address || "",
          experience: cached.experience !== undefined ? cached.experience : 0,
          skills: cached.skills || "",
        });
      }
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === "experience" ? (value === "" ? "" : Number(value)) : value,
    }));
    setError("");
  };

  const handleCancelEdit = () => {
    if (profile) {
      setFormData({
        fullName: profile.fullName || "",
        email: profile.email || "",
        address: profile.address || "",
        experience: profile.experience !== undefined ? profile.experience : 0,
        skills: profile.skills || "",
      });
    }
    setIsEditing(false);
    setError("");
    setMessage("");
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();

    if (!formData.fullName.trim()) {
      setError("Full name cannot be empty");
      return;
    }
    if (!formData.email.trim() || !formData.email.includes("@")) {
      setError("Please enter a valid email address");
      return;
    }
    if (!formData.address.trim()) {
      setError("Address cannot be empty");
      return;
    }
    if (formData.experience === "" || Number(formData.experience) < 0) {
      setError("Experience years must be 0 or greater");
      return;
    }
    if (!formData.skills.trim()) {
      setError("Skills description cannot be empty");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");

    try {
      const updated = await updateOperatorProfile({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        address: formData.address.trim(),
        experience: Number(formData.experience),
        skills: formData.skills.trim(),
      });

      setProfile(updated);
      setIsEditing(false);
      setMessage("Profile updated successfully!");

      // Update cached user
      const currentUser = getAuthUser() || {};
      saveAuth(null, {
        ...currentUser,
        fullName: updated.fullName,
        email: updated.email,
        address: updated.address,
        experience: updated.experience,
        skills: updated.skills,
      });

      setTimeout(() => setMessage(""), 4000);
    } catch (err) {
      setError(err.message || "Failed to update profile");
    } finally {
      setSaving(false);
    }
  };

  const handlePhotoUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) {
      setError("Profile image must be less than 5MB");
      return;
    }

    setUploadingPhoto(true);
    setError("");
    setMessage("");

    try {
      const updated = await uploadOperatorProfilePhoto(file);
      setProfile(updated);
      setMessage("Profile photo updated successfully!");

      // Update cached user
      const currentUser = getAuthUser() || {};
      saveAuth(null, {
        ...currentUser,
        profilePhoto: updated.profilePhoto,
      });

      setTimeout(() => setMessage(""), 4000);
    } catch (err) {
      setError(err.message || "Failed to upload profile photo");
    } finally {
      setUploadingPhoto(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case "APPROVED":
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-emerald-100 border border-emerald-300 text-emerald-900 text-xs font-bold rounded-full shadow-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
            ✓ Approved Operator
          </span>
        );
      case "REJECTED":
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-red-100 border border-red-300 text-red-900 text-xs font-bold rounded-full shadow-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span>
            ✕ Application Rejected
          </span>
        );
      case "PENDING":
      default:
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-amber-100 border border-amber-300 text-amber-900 text-xs font-bold rounded-full shadow-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse"></span>
            ⏳ Verification Pending
          </span>
        );
    }
  };

  const getDocStatusBadge = (status) => {
    switch (status) {
      case "VERIFIED":
        return (
          <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 text-[11px] font-bold rounded-full">
            ✓ Verified
          </span>
        );
      case "REJECTED":
        return (
          <span className="px-2.5 py-0.5 bg-red-100 text-red-800 text-[11px] font-bold rounded-full">
            ✕ Rejected
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-0.5 bg-yellow-100 text-yellow-800 text-[11px] font-bold rounded-full">
            ⏳ In Review
          </span>
        );
    }
  };

  if (loading && !profile) {
    return (
      <div className="min-h-[500px] flex items-center justify-center">
        <div className="text-center space-y-3">
          <div className="inline-block animate-spin rounded-full h-10 w-10 border-4 border-emerald-600 border-t-transparent"></div>
          <p className="text-sm font-semibold text-emerald-950">Loading Operator Profile...</p>
        </div>
      </div>
    );
  }

  const avatarUrl =
    profile?.profilePhotoUrl ||
    (profile?.profilePhoto?.startsWith("http")
      ? profile.profilePhoto
      : profile?.profilePhoto
      ? `http://localhost:8080/api/operators/profile/photo/${profile.profilePhoto}`
      : null);

  const aadhaarDoc = profile?.documents?.find((d) => d.documentType === "AADHAAR_CARD");
  const dlDoc = profile?.documents?.find((d) => d.documentType === "DRIVING_LICENSE");
  const expDoc = profile?.documents?.find((d) => d.documentType === "EXPERIENCE_CERTIFICATE");

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Toast / Message Alerts */}
      {message && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-950 text-sm flex items-center justify-between shadow-sm animate-fadeIn">
          <div className="flex items-center gap-3">
            <span className="w-7 h-7 rounded-full bg-emerald-200 text-emerald-900 flex items-center justify-center font-bold">
              ✓
            </span>
            <span className="font-semibold">{message}</span>
          </div>
          <button
            onClick={() => setMessage("")}
            className="text-emerald-800 hover:text-emerald-950 text-sm font-bold px-2"
          >
            ✕
          </button>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-2xl bg-red-50 border border-red-200 text-red-950 text-sm flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-3">
            <span className="w-7 h-7 rounded-full bg-red-200 text-red-900 flex items-center justify-center font-bold">
              ⚠️
            </span>
            <span className="font-semibold">{error}</span>
          </div>
          <button
            onClick={() => setError("")}
            className="text-red-800 hover:text-red-950 text-sm font-bold px-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Hero Profile Overview Card */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-md">
        <div className="flex flex-col md:flex-row items-center md:items-start justify-between gap-6">
          <div className="flex flex-col md:flex-row items-center gap-6 text-center md:text-left">
            {/* Avatar with Upload Trigger */}
            <div className="relative group">
              <div className="w-28 h-28 md:w-32 md:h-32 rounded-3xl overflow-hidden bg-gradient-to-tr from-emerald-100 to-amber-50 border-4 border-white shadow-lg flex items-center justify-center text-4xl font-extrabold text-emerald-900">
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt={profile?.fullName}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.target.style.display = "none";
                    }}
                  />
                ) : (
                  <span>{profile?.fullName?.charAt(0) || "🚜"}</span>
                )}
              </div>

              {/* Camera Upload Overlay */}
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingPhoto}
                className="absolute -bottom-2 -right-2 bg-amber-600 hover:bg-amber-700 text-white p-2.5 rounded-2xl shadow-md border-2 border-white transition transform hover:scale-110 active:scale-95 disabled:bg-gray-400"
                title="Change profile photo"
              >
                {uploadingPhoto ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                ) : (
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"
                    />
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth="2"
                      d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"
                    />
                  </svg>
                )}
              </button>
              <input
                type="file"
                ref={fileInputRef}
                onChange={handlePhotoUpload}
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
              />
            </div>

            {/* Basic Info */}
            <div className="space-y-2">
              <div className="flex flex-wrap items-center justify-center md:justify-start gap-2">
                <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight">
                  {profile?.fullName || "Operator"}
                </h1>
                {getStatusBadge(profile?.status)}
              </div>

              <p className="text-xs font-bold text-emerald-800 uppercase tracking-wider">
                Certified Heavy Machinery Operator
              </p>

              <div className="flex flex-wrap items-center justify-center md:justify-start gap-4 text-xs text-gray-600 pt-1">
                <span className="flex items-center gap-1.5 font-medium">
                  <span className="text-emerald-700 font-bold">📱</span>
                  +91 {profile?.mobileNumber}
                  {profile?.mobileVerified && (
                    <span className="text-emerald-700 font-bold ml-1">✓</span>
                  )}
                </span>
                <span className="flex items-center gap-1.5 font-medium">
                  <span className="text-emerald-700 font-bold">✉️</span>
                  {profile?.email}
                </span>
                <span className="flex items-center gap-1.5 font-medium">
                  <span className="text-emerald-700 font-bold">📍</span>
                  {profile?.address}
                </span>
              </div>
            </div>
          </div>

          {/* Action Header Button */}
          <div className="flex gap-2">
            {!isEditing ? (
              <button
                type="button"
                onClick={() => setIsEditing(true)}
                className="inline-flex items-center gap-2 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs px-5 py-3 rounded-2xl shadow-md hover:shadow-lg transition"
              >
                <span>✏️</span>
                <span>Edit Profile</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={handleCancelEdit}
                className="inline-flex items-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs px-5 py-3 rounded-2xl transition"
              >
                <span>✕</span>
                <span>Cancel</span>
              </button>
            )}
            <Link
              to="/operator/documents"
              className="inline-flex items-center gap-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-900 border border-emerald-200 font-bold text-xs px-5 py-3 rounded-2xl transition"
            >
              <span>🪪</span>
              <span>Documents</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Main Grid: Details Form / View */}
      <form onSubmit={handleSaveProfile} className="space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left 2 Columns: Personal & Professional Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* 1. Personal Information */}
            <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-5">
              <div className="flex items-center justify-between border-b border-amber-100/60 pb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xl">👤</span>
                  <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                    Personal Information
                  </h2>
                </div>
                <span className="text-xs text-gray-400 font-medium">
                  {isEditing ? "Editing Mode" : "Read-Only"}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {/* Full Name */}
                <div>
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Full Legal Name
                  </label>
                  {isEditing ? (
                    <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleInputChange}
                      required
                      className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                    />
                  ) : (
                    <p className="text-sm font-semibold text-gray-800 bg-[#FAF8F5] px-4 py-2.5 rounded-xl border border-gray-100">
                      {profile?.fullName || "—"}
                    </p>
                  )}
                </div>

                {/* Mobile Number (Locked / Immutable) */}
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider">
                      Mobile Number
                    </label>
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                      🔒 Immutable
                    </span>
                  </div>
                  <div className="relative">
                    <p className="text-sm font-semibold text-gray-700 bg-gray-100/80 px-4 py-2.5 rounded-xl border border-gray-200 flex items-center justify-between">
                      <span>+91 {profile?.mobileNumber || "—"}</span>
                      {profile?.mobileVerified && (
                        <span className="text-xs font-bold text-emerald-700">
                          ✓ OTP Verified
                        </span>
                      )}
                    </p>
                  </div>
                  <p className="text-[10px] text-gray-400 mt-1">
                    Mobile number is tied to your cryptographic security key and cannot be edited.
                  </p>
                </div>

                {/* Email */}
                <div>
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Email Address
                  </label>
                  {isEditing ? (
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleInputChange}
                      required
                      className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                    />
                  ) : (
                    <p className="text-sm font-semibold text-gray-800 bg-[#FAF8F5] px-4 py-2.5 rounded-xl border border-gray-100">
                      {profile?.email || "—"}
                    </p>
                  )}
                </div>

                {/* Address */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Residential Address & Location
                  </label>
                  {isEditing ? (
                    <textarea
                      name="address"
                      value={formData.address}
                      onChange={handleInputChange}
                      rows="2"
                      required
                      className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                    />
                  ) : (
                    <p className="text-sm font-semibold text-gray-800 bg-[#FAF8F5] px-4 py-2.5 rounded-xl border border-gray-100">
                      {profile?.address || "—"}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* 2. Professional Information */}
            <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-5">
              <div className="flex items-center justify-between border-b border-amber-100/60 pb-3">
                <div className="flex items-center gap-2">
                  <span className="text-xl">🚜</span>
                  <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                    Professional Experience & Machinery
                  </h2>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                {/* Experience */}
                <div>
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Operating Experience (Years)
                  </label>
                  {isEditing ? (
                    <input
                      type="number"
                      name="experience"
                      min="0"
                      value={formData.experience}
                      onChange={handleInputChange}
                      required
                      className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                    />
                  ) : (
                    <p className="text-sm font-semibold text-gray-800 bg-[#FAF8F5] px-4 py-2.5 rounded-xl border border-gray-100">
                      {profile?.experience} {profile?.experience === 1 ? "Year" : "Years"}
                    </p>
                  )}
                </div>

                {/* Driving License Number (Locked KYC) */}
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider">
                      Driving License No.
                    </label>
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                      🔒 Verified KYC
                    </span>
                  </div>
                  <p className="text-sm font-mono font-semibold text-gray-700 bg-gray-100/80 px-4 py-2.5 rounded-xl border border-gray-200">
                    {profile?.drivingLicenseNumber || "—"}
                  </p>
                </div>

                {/* Aadhaar Number (Locked KYC) */}
                <div>
                  <div className="flex justify-between items-center mb-2">
                    <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider">
                      Aadhaar Number
                    </label>
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full">
                      🔒 Verified KYC
                    </span>
                  </div>
                  <p className="text-sm font-mono font-semibold text-gray-700 bg-gray-100/80 px-4 py-2.5 rounded-xl border border-gray-200">
                    {profile?.aadhaarNumber
                      ? `XXXX-XXXX-${profile.aadhaarNumber.slice(-4)}`
                      : "—"}
                  </p>
                </div>

                {/* Associated Partner (if assigned) */}
                <div>
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Associated Partner
                  </label>
                  <p className="text-sm font-semibold text-gray-800 bg-[#FAF8F5] px-4 py-2.5 rounded-xl border border-gray-100">
                    {profile?.partnerName || "Independent Operator (Agro Platform)"}
                  </p>
                </div>

                {/* Skills */}
                <div className="md:col-span-2">
                  <label className="block text-xs font-bold text-emerald-950 uppercase tracking-wider mb-2">
                    Skills & Certified Machinery
                  </label>
                  {isEditing ? (
                    <div>
                      <input
                        type="text"
                        name="skills"
                        value={formData.skills}
                        onChange={handleInputChange}
                        placeholder="e.g. Tractor 4WD, Combine Harvester, Rotavator, Drone Sprayer"
                        required
                        className="w-full bg-[#FAF8F5] border border-amber-200/80 rounded-xl px-4 py-2.5 text-sm text-gray-800 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:bg-white transition"
                      />
                      <p className="text-[11px] text-gray-400 mt-1">
                        Separate machinery and certifications with commas.
                      </p>
                    </div>
                  ) : (
                    <div className="flex flex-wrap gap-2 pt-1">
                      {profile?.skills
                        ?.split(",")
                        .map((skill, index) => (
                          <span
                            key={index}
                            className="bg-emerald-50 text-emerald-900 border border-emerald-200/80 text-xs font-semibold px-3 py-1.5 rounded-xl shadow-2xs"
                          >
                            🚜 {skill.trim()}
                          </span>
                        ))}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Save Button in Edit Mode */}
            {isEditing && (
              <div className="flex items-center gap-3 pt-2">
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 bg-amber-600 hover:bg-amber-700 text-white font-bold py-3.5 px-6 rounded-2xl shadow-md hover:shadow-lg transition flex items-center justify-center gap-2 disabled:bg-gray-400"
                >
                  {saving ? (
                    <>
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                      <span>Saving Changes...</span>
                    </>
                  ) : (
                    <span>Save Profile Changes ✓</span>
                  )}
                </button>
                <button
                  type="button"
                  onClick={handleCancelEdit}
                  className="bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold py-3.5 px-6 rounded-2xl transition"
                >
                  Cancel
                </button>
              </div>
            )}
          </div>

          {/* Right Column: Verification, Documents & Security Status */}
          <div className="space-y-6">
            {/* 3. Verification Overview */}
            <div className="bg-white rounded-3xl p-6 border border-amber-100/70 shadow-sm space-y-4">
              <div className="flex items-center gap-2 border-b border-amber-100/60 pb-3">
                <span className="text-xl">🛡️</span>
                <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
                  Verification Badges
                </h2>
              </div>

              {/* Mobile OTP Badge */}
              <div className="p-3.5 bg-[#FAF8F5] rounded-2xl border border-amber-100/50 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-gray-800">Mobile OTP</p>
                  <p className="text-[11px] text-gray-500">
                    +91 {profile?.mobileNumber}
                  </p>
                </div>
                <span className="px-2.5 py-1 bg-emerald-100 text-emerald-800 text-[11px] font-bold rounded-full">
                  ✓ Verified
                </span>
              </div>

              {/* Aadhaar KYC */}
              <div className="p-3.5 bg-[#FAF8F5] rounded-2xl border border-amber-100/50 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-gray-800">Aadhaar Card</p>
                  <p className="text-[11px] text-gray-500">Government KYC</p>
                </div>
                <div>{getDocStatusBadge(aadhaarDoc?.verificationStatus || "VERIFIED")}</div>
              </div>

              {/* Driving License KYC */}
              <div className="p-3.5 bg-[#FAF8F5] rounded-2xl border border-amber-100/50 flex items-center justify-between">
                <div>
                  <p className="text-xs font-bold text-gray-800">Driving License</p>
                  <p className="text-[11px] text-gray-500">Heavy Vehicle / Tractor</p>
                </div>
                <div>{getDocStatusBadge(dlDoc?.verificationStatus || "VERIFIED")}</div>
              </div>

              {/* Experience Certificate */}
              {expDoc && (
                <div className="p-3.5 bg-[#FAF8F5] rounded-2xl border border-amber-100/50 flex items-center justify-between">
                  <div>
                    <p className="text-xs font-bold text-gray-800">Experience Cert</p>
                    <p className="text-[11px] text-gray-500">Skill Accreditation</p>
                  </div>
                  <div>{getDocStatusBadge(expDoc.verificationStatus)}</div>
                </div>
              )}

              {/* Module 3 Document Management Shortcut */}
              <div className="pt-2">
                <Link
                  to="/operator/documents"
                  className="w-full block text-center bg-emerald-50 hover:bg-emerald-100 text-emerald-900 font-bold text-xs py-2.5 rounded-xl border border-emerald-200/80 transition"
                >
                  Manage / Upload Documents (Module 3) →
                </Link>
              </div>
            </div>

            {/* 4. Account Summary Card */}
            <div className="bg-gradient-to-br from-emerald-900 to-green-950 text-white rounded-3xl p-6 shadow-md space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs uppercase font-bold text-emerald-200 tracking-wider">
                  Operator Account
                </span>
                <span className="text-xs px-2.5 py-0.5 rounded-full bg-white/10 backdrop-blur-md border border-white/20">
                  ID: #{profile?.id || "—"}
                </span>
              </div>

              <div>
                <p className="text-2xl font-black">{profile?.fullName}</p>
                <p className="text-xs text-emerald-200 mt-0.5">
                  Platform Status: {profile?.status}
                </p>
              </div>

              <div className="pt-3 border-t border-white/10 text-xs text-emerald-100/80 space-y-1.5">
                <div className="flex justify-between">
                  <span>Registered Date:</span>
                  <span className="font-semibold">
                    {profile?.createdAt
                      ? new Date(profile.createdAt).toLocaleDateString("en-IN", {
                          day: "numeric",
                          month: "short",
                          year: "numeric",
                        })
                      : "Active"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Assigned Deployments:</span>
                  <span className="font-semibold text-emerald-300">Ready for Duty</span>
                </div>
              </div>

              <div className="pt-2">
                <Link
                  to="/operator/dashboard"
                  className="block text-center w-full bg-white/10 hover:bg-white/20 text-white text-xs font-bold py-2.5 rounded-xl border border-white/20 transition"
                >
                  Go to Operator Dashboard →
                </Link>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}

export default OperatorProfile;
