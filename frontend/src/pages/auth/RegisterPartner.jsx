import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function RegisterPartner() {
  const navigate = useNavigate();

  const [role, setRole] = useState('partner');
  const [fullName, setFullName] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');

  const handleSendOtp = (e) => {
    e.preventDefault();

    if (!fullName.trim()) {
      alert('Please enter your full name');
      return;
    }

    if (!/^[0-9]{10}$/.test(mobileNumber)) {
      alert('Please enter a valid 10 digit mobile number');
      return;
    }

    // OTP screen la user details pathavto.
    navigate('/verify-otp', {
      state: {
        role,
        fullName,
        mobileNumber,
      },
    });
  };

  const handleWhatsAppOtp = () => {
    if (!/^[0-9]{10}$/.test(mobileNumber)) {
      alert('Please enter a valid 10 digit mobile number');
      return;
    }

    alert('WhatsApp OTP option selected');
  };

  return (
    <div className="min-h-screen bg-[#f5f0e6] py-10 px-4">

      {/* Welcome Banner */}
      <div className="mx-auto max-w-[420px] h-[135px] rounded-2xl overflow-hidden relative bg-gradient-to-r from-[#3f7625] to-[#6f913e]">

        <div className="absolute inset-0 opacity-20">
          <div className="w-full h-full bg-[radial-gradient(circle_at_20%_30%,white_0,transparent_35%),radial-gradient(circle_at_80%_70%,#183d10_0,transparent_45%)]" />
        </div>

        <div className="relative z-10 p-6 text-white">
          <p className="text-sm font-medium">Welcome to</p>
          <h2 className="text-3xl font-bold leading-tight">
            AgroRent
          </h2>
        </div>
      </div>

      {/* Registration Card */}
      <div className="mx-auto mt-7 max-w-[420px] rounded-2xl bg-white border border-gray-200 shadow-sm px-7 py-8">

        <h1 className="text-[25px] font-bold text-gray-900">
          Create your account
        </h1>

        <p className="mt-1 text-sm text-gray-500">
          Join 50,000+ farmers on AgroRent
        </p>

        {/* Role */}
        <div className="mt-7">
          <label className="block text-sm font-semibold text-gray-900 mb-3">
            I am a
          </label>

          <div className="grid grid-cols-3 gap-2">

            {/* Farmer */}
            <button
              type="button"
              onClick={() => navigate('/register/farmer')}
              className="h-[72px] rounded-xl border border-gray-200 bg-white hover:border-[#4c842d] transition"
            >
              <div className="text-xl">🌾</div>
              <div className="text-xs mt-1 text-gray-700">
                Farmer
              </div>
            </button>

            {/* Equipment Owner */}
            <button
              type="button"
              onClick={() => setRole('partner')}
              className={`h-[72px] rounded-xl transition ${
                role === 'partner'
                  ? 'border-2 border-[#4c842d] bg-[#f8fbf5]'
                  : 'border border-gray-200 bg-white'
              }`}
            >
              <div className="text-xl">🚜</div>
              <div className="text-xs mt-1 text-gray-700">
                Equipment Owner
              </div>
            </button>

            {/* Admin */}
            <button
              type="button"
              onClick={() => alert('Admin registration is not available here')}
              className="h-[72px] rounded-xl border border-gray-200 bg-white hover:border-[#4c842d] transition"
            >
              <div className="text-xl">📊</div>
              <div className="text-xs mt-1 text-gray-700">
                Admin
              </div>
            </button>

          </div>
        </div>

        {/* Form */}
        <form onSubmit={handleSendOtp}>

          {/* Full Name */}
          <div className="mt-6">
            <label className="block text-sm font-semibold text-gray-900 mb-2">
              Full Name
            </label>

            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Ramesh Yadav"
              className="w-full h-[44px] rounded-xl border border-gray-300 bg-[#f8f5ed] px-4 text-sm outline-none focus:border-[#4c842d] focus:ring-1 focus:ring-[#4c842d]"
            />
          </div>

          {/* Mobile */}
          <div className="mt-5">
            <label className="block text-sm font-semibold text-gray-900 mb-2">
              Mobile Number
            </label>

            <div className="flex h-[44px] rounded-xl border border-gray-300 bg-[#f8f5ed] overflow-hidden focus-within:border-[#4c842d]">

              <div className="flex items-center px-3 border-r border-gray-300 text-sm text-gray-600">
                IN +91
              </div>

              <input
                type="tel"
                maxLength={10}
                value={mobileNumber}
                onChange={(e) =>
                  setMobileNumber(
                    e.target.value.replace(/\D/g, '')
                  )
                }
                placeholder="98765 43210"
                className="flex-1 bg-transparent px-3 text-sm outline-none"
              />

            </div>
          </div>

          {/* Send OTP */}
          <button
            type="submit"
            className="w-full mt-5 h-[47px] rounded-xl bg-[#4b822d] hover:bg-[#3f7026] text-white font-semibold text-sm transition"
          >
            Send OTP →
          </button>

        </form>

        {/* Divider */}
        <div className="flex items-center gap-3 my-5">
          <div className="h-px bg-gray-200 flex-1" />
          <span className="text-xs text-gray-500">
            or continue with
          </span>
          <div className="h-px bg-gray-200 flex-1" />
        </div>

        {/* WhatsApp OTP */}
        <button
          type="button"
          onClick={handleWhatsAppOtp}
          className="w-full h-[47px] rounded-xl border border-gray-200 bg-white hover:bg-gray-50 text-sm font-semibold text-gray-800 transition"
        >
          📱 &nbsp; OTP Login (WhatsApp)
        </button>

        {/* Login */}
        <p className="text-center text-sm text-gray-500 mt-7">
          Already have an account?{' '}
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="text-[#4b822d] font-semibold hover:underline"
          >
            Log In
          </button>
        </p>

      </div>
    </div>
  );
}

export default RegisterPartner;

