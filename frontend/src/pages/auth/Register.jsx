import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerFarmer } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

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
    <div className="min-h-screen bg-[#F7F6F0] flex flex-col items-center justify-start py-6 px-4 sm:px-6 font-sans">
      {/* Top Header Navigation Brand */}
      <div className="w-full max-w-xl flex items-center justify-between py-3 mb-4">
        <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-[#2E6F22] tracking-tight">
          <span className="w-9 h-9 rounded-full bg-[#3E7B27] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            🌱
          </span>
          <span>AgroRent</span>
        </Link>
      </div>

      {/* Main Registration Card */}
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100/80 transition-all duration-300">
        
        {/* Banner Hero Image Header */}
        <div className="relative h-28 sm:h-32 bg-gradient-to-r from-emerald-800 to-green-900 overflow-hidden flex items-center justify-center">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1000&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-50"
          />
          <div className="relative z-10 text-center px-4">
            <span className="text-xs uppercase tracking-widest font-semibold text-emerald-200 block mb-0.5">Welcome to</span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-wide drop-shadow-md">AgroRent</h2>
          </div>
        </div>

        {/* Form Container */}
        <div className="p-6 sm:p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">Create your account</h1>
            <p className="text-sm text-gray-500 mt-1 font-medium">Join 50,000+ farmers on AgroRent</p>
          </div>

          {/* Role Selection Tabs */}
          <div className="mb-6">
            <label className="block text-xs font-semibold text-gray-500 mb-2">I am a</label>
            <div className="grid grid-cols-3 gap-2 sm:gap-3">
              {/* Farmer Tab */}
              <button
                type="button"
                onClick={() => setRole('farmer')}
                className={`flex flex-col items-center justify-center p-3 rounded-xl border-2 transition-all duration-200 ${
                  role === 'farmer'
                    ? 'border-[#3E7B27] bg-[#F1F8EE] text-[#2E6F22] shadow-sm font-bold'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <span className="text-xl mb-1">🌾</span>
                <span className="text-xs font-semibold">Farmer</span>
              </button>

              {/* Equipment Owner Tab */}
              <button
                type="button"
                onClick={() => {
                  setRole('owner');
                  navigate('/register/partner');
                }}
                className={`flex flex-col items-center justify-center p-3 rounded-xl border-2 transition-all duration-200 ${
                  role === 'owner'
                    ? 'border-[#3E7B27] bg-[#F1F8EE] text-[#2E6F22] shadow-sm font-bold'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <span className="text-xl mb-1">🚜</span>
                <span className="text-xs font-semibold">Equipment Owner</span>
              </button>

              {/* Admin Tab */}
              <button
                type="button"
                onClick={() => {
                  setRole('admin');
                }}
                className={`flex flex-col items-center justify-center p-3 rounded-xl border-2 transition-all duration-200 ${
                  role === 'admin'
                    ? 'border-[#3E7B27] bg-[#F1F8EE] text-[#2E6F22] shadow-sm font-bold'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <span className="text-xl mb-1">📊</span>
                <span className="text-xs font-semibold">Admin</span>
              </button>
            </div>
          </div>

          {/* Feedback Banners */}
          {errorMessage && (
            <div className="mb-4 p-3 bg-red-50 border-l-4 border-red-500 rounded-md text-red-700 text-sm font-medium flex items-center justify-between">
              <span>{errorMessage}</span>
              <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 hover:text-red-700 font-bold ml-2">✕</button>
            </div>
          )}

          {successMessage && (
            <div className="mb-4 p-3 bg-emerald-50 border-l-4 border-emerald-500 rounded-md text-emerald-800 text-sm font-medium flex items-center justify-between">
              <span>{successMessage}</span>
            </div>
          )}

          {/* Registration Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Full Name */}
            <div>
              <label htmlFor="fullName" className="block text-xs font-semibold text-gray-700 mb-1.5">
                {t('full_name')} <span className="text-red-500">*</span>
              </label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                required
                value={formData.fullName}
                onChange={handleInputChange}
                placeholder="Ramesh Yadav"
                className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Mobile Number */}
            <div>
              <label htmlFor="mobileNumber" className="block text-xs font-semibold text-gray-700 mb-1.5">
                {t('mobile_number')} <span className="text-red-500">*</span>
              </label>
              <div className="flex rounded-xl overflow-hidden border border-transparent focus-within:ring-2 focus-within:ring-[#3E7B27] focus-within:bg-white">
                <span className="inline-flex items-center px-3.5 bg-[#E6E4DC] text-gray-700 text-xs font-bold border-r border-gray-300">
                  IN +91
                </span>
                <input
                  id="mobileNumber"
                  name="mobileNumber"
                  type="tel"
                  required
                  maxLength={10}
                  value={formData.mobileNumber}
                  onChange={handleMobileChange}
                  placeholder="98765 43210"
                  className="w-full px-4 py-3 bg-[#F0EFE9] text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:bg-white transition-all"
                />
              </div>
            </div>

            {/* Optional Email Address */}
            <div>
              <label htmlFor="email" className="block text-xs font-semibold text-gray-700 mb-1.5">
                Email Address <span className="text-gray-400 text-xs font-normal">(Optional)</span>
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleInputChange}
                placeholder="ramesh.yadav@example.com"
                className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Password */}
            <div>
              <label htmlFor="password" className="block text-xs font-semibold text-gray-700 mb-1.5">
                Password <span className="text-gray-400 text-xs font-normal">(Optional for OTP auth)</span>
              </label>
              <div className="relative">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  value={formData.password}
                  onChange={handleInputChange}
                  placeholder="••••••••"
                  className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all pr-10"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700 text-xs font-medium"
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>

            {/* Preferred Language */}
            <div>
              <label htmlFor="preferredLanguage" className="block text-xs font-semibold text-gray-700 mb-1.5">
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
                className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all cursor-pointer"
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

            {/* Submit Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-6 bg-[#3E7B27] hover:bg-[#32641F] text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition-all duration-200 flex items-center justify-center gap-2 group disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <>
                    <svg className="animate-spin h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <span>Sending OTP...</span>
                  </>
                ) : (
                  <>
                    <span>{t('send_otp')}</span>
                    <span className="group-hover:translate-x-1 transition-transform">→</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Alternative Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200"></div>
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-white px-3 text-gray-400 font-medium">or continue with</span>
            </div>
          </div>

          {/* Social / WhatsApp Login Button */}
          <div>
            <button
              type="button"
              onClick={() => {
                if (formData.mobileNumber.length === 10) {
                  handleSubmit({ preventDefault: () => {} });
                } else {
                  setErrorMessage('Please enter a 10-digit mobile number for WhatsApp OTP Login.');
                }
              }}
              className="w-full py-3 px-4 bg-white border border-gray-300 rounded-xl text-gray-700 text-sm font-semibold hover:bg-gray-50 transition-colors flex items-center justify-center gap-2 shadow-sm"
            >
              <span className="text-lg">💬</span>
              <span>OTP Login (WhatsApp)</span>
            </button>
          </div>

          {/* Footer Link */}
          <div className="text-center mt-6">
            <p className="text-xs text-gray-500">
              Already have an account?{' '}
              <Link to="/login" className="font-bold text-[#3E7B27] hover:underline">
                Log In
              </Link>
            </p>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Register;
