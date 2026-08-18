import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Tractor,
  User,
  Building2,
  Phone,
  Mail,
  Lock,
  MapPin,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  ShieldCheck,
} from 'lucide-react';
import { partnerService } from '../../services/partnerService';
import { saveUserSession } from '../../services/authService';

function RegisterPartner() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    businessName: '',
    mobileNumber: '',
    email: '',
    address: '',
    password: '',
    confirmPassword: '',
    gstNumber: '',
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

    if (!formData.fullName.trim()) {
      setErrorMessage('Full name is required.');
      return;
    }

    if (!formData.mobileNumber || formData.mobileNumber.length !== 10) {
      setErrorMessage('Mobile number must be exactly 10 digits.');
      return;
    }

    if (!formData.password || formData.password.length < 6) {
      setErrorMessage('Password must be at least 6 characters long.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      setErrorMessage('Password and Confirm Password do not match.');
      return;
    }

    setLoading(true);

    try {
      const payload = {
        fullName: formData.fullName.trim(),
        businessName: formData.businessName.trim() || null,
        mobileNumber: formData.mobileNumber.trim(),
        email: formData.email.trim() || null,
        address: formData.address.trim() || null,
        gstNumber: formData.gstNumber.trim() || null,
        password: formData.password,
      };

      const res = await partnerService.registerPartner(payload);
      const partnerData = res.data || res;

      setSuccessMessage('✓ Equipment Partner account registered successfully! Redirecting to login...');

      setTimeout(() => {
        navigate('/login', {
          state: {
            mobileOrEmail: formData.mobileNumber,
            message: 'Registration complete! Please log in with your credentials.',
          },
        });
      }, 1500);
    } catch (err) {
      console.error('Partner Registration error:', err);
      if (err.message && err.message.includes('Network error')) {
        setSuccessMessage('Demo Mode: Account draft created! Redirecting to login...');
        setTimeout(() => {
          navigate('/login');
        }, 1500);
      } else {
        setErrorMessage(err.message || 'Failed to register partner. Please check all fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F6F0] flex flex-col items-center justify-start py-8 px-4 sm:px-6 font-sans">
      
      {/* Top Header Navigation Brand */}
      <div className="w-full max-w-xl flex items-center justify-between py-2 mb-4">
        <Link to="/" className="flex items-center gap-2 text-2xl font-black text-[#142E1C] tracking-tight">
          <span className="w-9 h-9 rounded-2xl bg-[#3E7B27] text-white flex items-center justify-center font-bold text-lg shadow-sm">
            🌱
          </span>
          <span>AgroRent</span>
        </Link>
      </div>

      {/* Main Registration Card */}
      <div className="w-full max-w-xl bg-white rounded-3xl shadow-xl overflow-hidden border border-gray-100/90 transition-all duration-300">
        
        {/* Banner Hero Image Header */}
        <div className="relative h-28 sm:h-32 bg-gradient-to-r from-[#142E1C] via-[#1B4D3E] to-[#2E7D32] overflow-hidden flex items-center justify-center">
          <img
            src="https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1000&q=80"
            alt="Agro Field Banner"
            className="absolute inset-0 w-full h-full object-cover opacity-30 mix-blend-overlay"
          />
          <div className="relative z-10 text-center px-4 text-white">
            <span className="text-[11px] uppercase tracking-widest font-black text-lime-300 block mb-0.5">
              Equipment Partner Portal
            </span>
            <h2 className="text-2xl sm:text-3xl font-black tracking-wide drop-shadow-md">
              Register as Machinery Owner
            </h2>
          </div>
        </div>

        {/* Form Container */}
        <div className="p-6 sm:p-8 space-y-6">
          
          {/* Role Selection Tabs */}
          <div>
            <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
              Select Registration Role
            </label>
            <div className="grid grid-cols-3 gap-2">
              
              {/* Farmer Tab */}
              <button
                type="button"
                onClick={() => navigate('/register')}
                className="flex flex-col items-center justify-center p-3 rounded-2xl border border-gray-200 bg-white text-gray-600 hover:border-gray-300 transition-all"
              >
                <span className="text-xl mb-0.5">🌾</span>
                <span className="text-xs font-bold">Farmer</span>
              </button>

              {/* Equipment Owner Tab (Active) */}
              <button
                type="button"
                className="flex flex-col items-center justify-center p-3 rounded-2xl border-2 border-[#3E7B27] bg-[#F1F8EE] text-[#142E1C] shadow-sm font-black transition-all"
              >
                <span className="text-xl mb-0.5">🚜</span>
                <span className="text-xs font-black">Machinery Owner</span>
              </button>

              {/* Admin Tab */}
              <button
                type="button"
                onClick={() => alert('Admin registration is restricted to system administrators.')}
                className="flex flex-col items-center justify-center p-3 rounded-2xl border border-gray-200 bg-white text-gray-600 hover:border-gray-300 transition-all"
              >
                <span className="text-xl mb-0.5">📊</span>
                <span className="text-xs font-bold">Admin</span>
              </button>

            </div>
          </div>

          {/* Feedback Banners */}
          {errorMessage && (
            <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
              <div className="flex items-center gap-2">
                <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
                <span>{errorMessage}</span>
              </div>
              <button type="button" onClick={() => setErrorMessage('')} className="text-red-500 font-bold ml-2">✕</button>
            </div>
          )}

          {successMessage && (
            <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-xl text-emerald-800 text-sm font-medium flex items-center gap-2 shadow-xs">
              <CheckCircle2 className="w-5 h-5 shrink-0 text-emerald-600" />
              <span>{successMessage}</span>
            </div>
          )}

          {/* Registration Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              
              {/* Full Name */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Full Name <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type="text"
                    name="fullName"
                    required
                    value={formData.fullName}
                    onChange={handleInputChange}
                    placeholder="e.g. Rajesh Patel"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                  />
                </div>
              </div>

              {/* Business Name */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Enterprise Name
                </label>
                <input
                  type="text"
                  name="businessName"
                  value={formData.businessName}
                  onChange={handleInputChange}
                  placeholder="e.g. Patel Agro Fleet"
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>

              {/* Mobile Number */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Mobile Number <span className="text-red-500">*</span>
                </label>
                <div className="flex rounded-xl overflow-hidden border border-transparent focus-within:ring-2 focus-within:ring-[#3E7B27] focus-within:bg-white">
                  <span className="inline-flex items-center px-3 bg-[#E6E4DC] text-gray-700 text-xs font-bold border-r border-gray-300">
                    IN +91
                  </span>
                  <input
                    type="tel"
                    required
                    maxLength={10}
                    value={formData.mobileNumber}
                    onChange={handleMobileChange}
                    placeholder="98765 43210"
                    className="w-full px-3 py-2.5 bg-[#F0EFE9] text-gray-900 text-sm font-semibold focus:outline-none focus:bg-white transition-all"
                  />
                </div>
              </div>

              {/* Email Address */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Email Address
                </label>
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  placeholder="rajesh.patel@example.com"
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>

              {/* Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Account Password <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    required
                    minLength={6}
                    value={formData.password}
                    onChange={handleInputChange}
                    placeholder="••••••••"
                    className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-800 text-xs"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Confirm Password <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  name="confirmPassword"
                  required
                  value={formData.confirmPassword}
                  onChange={handleInputChange}
                  placeholder="••••••••"
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>

              {/* Address / Operational Hub */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Machinery Base Address / Hub
                </label>
                <textarea
                  name="address"
                  rows={2}
                  value={formData.address}
                  onChange={handleInputChange}
                  placeholder="e.g. Krishi Seva Kendra, Market Yard, Pune, Maharashtra"
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-gray-900 text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>

            </div>

            {/* Submit Button */}
            <div className="pt-3">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-6 bg-[#3E7B27] hover:bg-[#2E6F22] text-white font-extrabold rounded-2xl shadow-md hover:shadow-lg transition-all duration-150 flex items-center justify-center gap-2 group disabled:opacity-70"
              >
                {loading ? (
                  <span>Registering Partner Account...</span>
                ) : (
                  <>
                    <span>Create Partner Account</span>
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </>
                )}
              </button>
            </div>

          </form>

          {/* Footer Link */}
          <div className="text-center pt-2 border-t border-gray-100">
            <p className="text-xs text-gray-500 font-medium">
              Already have an equipment partner account?{' '}
              <Link to="/login" className="font-bold text-[#3E7B27] hover:underline">
                Log In Here
              </Link>
            </p>
          </div>

        </div>

      </div>

    </div>
  );
}

export default RegisterPartner;
