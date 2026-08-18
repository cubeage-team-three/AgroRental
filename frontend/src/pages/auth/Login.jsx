import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { loginUser, loginWithOtp, saveUserSession } from '../../services/authService';
import { sendOtp } from '../../services/farmerAuthService';
import { useLanguage } from '../../context/LanguageContext';

function Login() {
  const { t } = useLanguage();
  const navigate = useNavigate();

  // Mode: "PASSWORD" or "OTP"
  const [loginMode, setLoginMode] = useState('PASSWORD');

  const [formData, setFormData] = useState({
    mobileOrEmail: '',
    password: '',
    otp: '',
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [sendingOtp, setSendingOtp] = useState(false);
  const [devMockOtp, setDevMockOtp] = useState('');

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

  const handleSendOtp = async () => {
    if (!formData.mobileOrEmail.trim()) {
      setErrorMessage('Please enter your 10-digit mobile number.');
      return;
    }
    const mobile = formData.mobileOrEmail.replace(/\D/g, '');
    if (mobile.length !== 10) {
      setErrorMessage('Please enter a valid 10-digit mobile number for OTP login.');
      return;
    }

    setSendingOtp(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const res = await sendOtp(mobile);
      setOtpSent(true);
      setSuccessMessage('OTP sent successfully to +91 ' + mobile);
      if (res.data?.devMockOtp) {
        setDevMockOtp(res.data.devMockOtp);
      }
    } catch (err) {
      console.error('Send OTP error during login:', err);
      if (err.message && err.message.includes('Network error')) {
        setOtpSent(true);
        setSuccessMessage('Demo Mode: OTP sent!');
        setDevMockOtp('123456');
      } else {
        setErrorMessage(err.message || 'Failed to send OTP. Please try again.');
      }
    } finally {
      setSendingOtp(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    if (!formData.mobileOrEmail.trim()) {
      setErrorMessage('Please enter your mobile number or email address.');
      return;
    }

    if (loginMode === 'PASSWORD' && !formData.password) {
      setErrorMessage('Password is required.');
      return;
    }

    if (loginMode === 'OTP' && (!formData.otp || formData.otp.length !== 6)) {
      setErrorMessage('Please enter the 6-digit OTP code.');
      return;
    }

    setLoading(true);

    try {
      let response;
      if (loginMode === 'OTP') {
        response = await loginWithOtp({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          otp: formData.otp.trim(),
        });
      } else {
        response = await loginUser({
          mobileOrEmail: formData.mobileOrEmail.trim(),
          password: formData.password,
          loginType: 'PASSWORD',
        });
      }

      const userData = response.data;
      saveUserSession(userData);
<<<<<<< HEAD
      setSuccessMessage('Login successful! Redirecting to Farmer Dashboard...');

      setTimeout(() => {
        navigate('/farmer/dashboard');
      }, 1200);
=======
      
      if (userData && (userData.role === 'PARTNER' || userData.partnerId)) {
        setSuccessMessage('Partner login successful! Redirecting to Partner Portal...');
        setTimeout(() => {
          navigate('/partner/dashboard');
        }, 1200);
      } else {
        setSuccessMessage('Login successful! Redirecting to Farmer Dashboard...');
        setTimeout(() => {
          navigate('/farmer/dashboard');
        }, 1200);
      }
>>>>>>> origin/development

    } catch (err) {
      console.error('Login error:', err);

      // Special rule: PENDING_OTP redirection
      if (err.message && err.message.includes('PENDING_OTP')) {
        const cleanMobile = formData.mobileOrEmail.replace(/\D/g, '');
        setErrorMessage('Your account is pending OTP verification. Redirecting...');
        setTimeout(() => {
          navigate('/verify-otp', {
            state: { mobileNumber: cleanMobile }
          });
        }, 1500);
        return;
      }

      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: Logged in! Redirecting to Farmer Dashboard...');
        saveUserSession({
          token: 'demo-token-123',
          fullName: 'Farmer User',
          mobileNumber: formData.mobileOrEmail,
          role: 'FARMER',
        });
        setTimeout(() => {
          navigate('/farmer/dashboard');
        }, 1200);
      } else {
        setErrorMessage(err.message || 'Login failed. Please check your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F6F0] flex flex-col items-center justify-start py-6 px-4 sm:px-6 font-sans">
      
      {/* Top Navigation Brand Logo */}
      <div className="w-full max-w-xl flex items-center justify-between py-3 mb-4">
        <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-[#2E6F22] tracking-tight">
          <span className="w-9 h-9 rounded-full bg-[#3E7B27] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            🌱
          </span>
          <span>AgroRent</span>
        </Link>
      </div>

      {/* Main Login Card */}
      <div className="w-full max-w-lg bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100/80 transition-all duration-300">
        
        {/* Banner Hero Image Header */}
        <div className="relative h-28 sm:h-32 bg-gradient-to-r from-emerald-800 to-green-900 overflow-hidden flex items-center justify-center">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1000&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-50"
          />
          <div className="relative z-10 text-center px-4">
            <span className="text-xs uppercase tracking-widest font-semibold text-emerald-200 block mb-0.5">Welcome back to</span>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-wide drop-shadow-md">AgroRent</h2>
          </div>
        </div>

        {/* Form Container */}
        <div className="p-6 sm:p-8">
          <div className="text-center mb-6">
            <h1 className="text-2xl font-extrabold text-gray-900 tracking-tight">Farmer Portal Login</h1>
            <p className="text-sm text-gray-500 mt-1 font-medium">Access your farms, bookings, and machinery</p>
          </div>

          {/* Authentication Mode Tabs */}
          <div className="flex bg-[#F0EFE9] p-1 rounded-2xl mb-6">
            <button
              type="button"
              onClick={() => {
                setLoginMode('PASSWORD');
                setErrorMessage('');
              }}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all duration-200 ${
                loginMode === 'PASSWORD'
                  ? 'bg-white text-[#2E6F22] shadow-sm'
                  : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              🔑 Password Login
            </button>
            <button
              type="button"
              onClick={() => {
                setLoginMode('OTP');
                setErrorMessage('');
              }}
              className={`flex-1 py-2.5 text-xs sm:text-sm font-bold rounded-xl transition-all duration-200 ${
                loginMode === 'OTP'
                  ? 'bg-white text-[#2E6F22] shadow-sm'
                  : 'text-gray-500 hover:text-gray-800'
              }`}
            >
              📱 OTP Quick Login
            </button>
          </div>

          {/* Quick OTP Helper Badge */}
          {devMockOtp && (
            <div className="mb-4 p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs flex items-center justify-between">
              <span>Verification OTP Code: <strong className="text-sm font-bold">{devMockOtp}</strong></span>
              <button
                type="button"
                onClick={() => setFormData((prev) => ({ ...prev, otp: devMockOtp }))}
                className="px-2 py-1 bg-emerald-600 text-white rounded text-xs font-semibold"
              >
                Auto-fill
              </button>
            </div>
          )}

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

          {/* Main Login Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            
            {/* Mobile / Email Input */}
            <div>
              <label htmlFor="mobileOrEmail" className="block text-xs font-semibold text-gray-700 mb-1.5">
                Mobile Number or Email Address <span className="text-red-500">*</span>
              </label>
              <input
                id="mobileOrEmail"
                name="mobileOrEmail"
                type="text"
                required
                value={formData.mobileOrEmail}
                onChange={handleInputChange}
                placeholder="9876543210 or farmer@example.com"
                className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 placeholder-gray-400 text-sm focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Password Login Mode */}
            {loginMode === 'PASSWORD' && (
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="password" className="block text-xs font-semibold text-gray-700">
                    Password <span className="text-red-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setLoginMode('OTP');
                      handleSendOtp();
                    }}
                    className="text-xs font-semibold text-[#3E7B27] hover:underline"
                  >
                    Forgot Password? Login via OTP
                  </button>
                </div>
                <div className="relative">
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    required
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
            )}

            {/* OTP Login Mode */}
            {loginMode === 'OTP' && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label htmlFor="otp" className="block text-xs font-semibold text-gray-700">
                    6-Digit Security OTP <span className="text-red-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={handleSendOtp}
                    disabled={sendingOtp}
                    className="text-xs font-bold text-[#3E7B27] hover:underline disabled:opacity-50"
                  >
                    {sendingOtp ? 'Sending...' : otpSent ? 'Resend OTP' : 'Send OTP'}
                  </button>
                </div>
                <input
                  id="otp"
                  name="otp"
                  type="text"
                  maxLength={6}
                  value={formData.otp}
                  onChange={handleInputChange}
                  placeholder="Enter 6-digit OTP"
                  className="w-full px-4 py-3 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 placeholder-gray-400 text-sm tracking-widest text-center font-bold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>
            )}

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
                    <span>Logging in...</span>
                  </>
                ) : (
                  <>
                    <span>{t('login')}</span>
                    <span className="group-hover:translate-x-1 transition-transform">→</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Footer Link */}
<<<<<<< HEAD
          <div className="text-center mt-6">
            <p className="text-xs text-gray-500">
              Don't have a farmer account?{' '}
              <Link to="/register" className="font-bold text-[#3E7B27] hover:underline">
                Sign Up Free
=======
          <div className="text-center mt-6 space-y-1.5">
            <p className="text-xs text-gray-500">
              Don't have an account?{' '}
              <Link to="/register" className="font-bold text-[#3E7B27] hover:underline">
                Farmer Sign Up
              </Link>
              {' • '}
              <Link to="/register/partner" className="font-bold text-[#3E7B27] hover:underline">
                Equipment Owner Sign Up
>>>>>>> origin/development
              </Link>
            </p>
          </div>

        </div>
      </div>
    </div>
  );
}

export default Login;
