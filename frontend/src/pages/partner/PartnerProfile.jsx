import { useEffect, useState } from 'react';

function PartnerProfile() {
  const [partner, setPartner] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const partnerId = 1;

  useEffect(() => {
    fetch(`http://localhost:8080/api/partners/${partnerId}`)
      .then((response) => {
        if (!response.ok) {
          throw new Error('Failed to fetch partner profile');
        }
        return response.json();
      })
      .then((data) => {
        setPartner(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError('Unable to load profile');
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="p-6">
        <p className="text-gray-600">Loading profile...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          Business Profile
        </h1>
        <p className="mt-1 text-gray-500">
          View your partner account and business information.
        </p>
      </div>

      <div className="max-w-4xl rounded-xl bg-white shadow-sm border border-gray-100">
        <div className="border-b border-gray-100 px-6 py-5">
          <h2 className="text-xl font-semibold text-gray-800">
            Partner Information
          </h2>
        </div>

        <div className="grid grid-cols-1 gap-6 p-6 md:grid-cols-2">
          <div>
            <p className="text-sm text-gray-500">Full Name</p>
            <p className="mt-1 font-medium text-gray-800">
              {partner.fullName}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Business Name</p>
            <p className="mt-1 font-medium text-gray-800">
              {partner.businessName || 'Not provided'}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Mobile Number</p>
            <p className="mt-1 font-medium text-gray-800">
              {partner.mobileNumber}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Email</p>
            <p className="mt-1 font-medium text-gray-800">
              {partner.email || 'Not provided'}
            </p>
          </div>

          <div className="md:col-span-2">
            <p className="text-sm text-gray-500">Address</p>
            <p className="mt-1 font-medium text-gray-800">
              {partner.address || 'Not provided'}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">OTP Verification</p>
            <span
              className={`mt-1 inline-block rounded-full px-3 py-1 text-sm font-medium ${
                partner.otpVerified
                  ? 'bg-green-100 text-green-700'
                  : 'bg-yellow-100 text-yellow-700'
              }`}
            >
              {partner.otpVerified ? 'Verified' : 'Not Verified'}
            </span>
          </div>

          <div>
            <p className="text-sm text-gray-500">Verification Status</p>
            <span
              className={`mt-1 inline-block rounded-full px-3 py-1 text-sm font-medium ${
                partner.verificationStatus === 'APPROVED'
                  ? 'bg-green-100 text-green-700'
                  : partner.verificationStatus === 'REJECTED'
                    ? 'bg-red-100 text-red-700'
                    : 'bg-yellow-100 text-yellow-700'
              }`}
            >
              {partner.verificationStatus}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PartnerProfile;

