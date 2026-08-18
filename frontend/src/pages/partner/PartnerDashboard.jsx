import { useEffect, useState } from 'react';

function PartnerDashboard() {
  const [partner, setPartner] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Temporary partner ID
  // Later login madhun dynamic ID gheu
  const partnerId = 1;

  useEffect(() => {
    fetch(`http://localhost:8080/api/partners/${partnerId}`)
      .then(async (response) => {
        if (!response.ok) {
          throw new Error('Failed to load partner data');
        }

        return response.json();
      })
      .then((data) => {
        setPartner(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError('Unable to load partner information.');
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <p className="text-gray-600">Loading partner dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <p className="text-red-600">{error}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800">
          Welcome, {partner.fullName}
        </h1>

        <p className="mt-1 text-gray-500">
          Manage your equipment, bookings and earnings.
        </p>
      </div>

      {/* Partner Information */}
      <div className="mb-8 rounded-xl bg-white p-6 shadow-sm border border-gray-100">
        <h2 className="text-lg font-semibold text-gray-800 mb-4">
          Partner Information
        </h2>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">

          <div>
            <p className="text-sm text-gray-500">Full Name</p>
            <p className="font-medium text-gray-800">
              {partner.fullName}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Business Name</p>
            <p className="font-medium text-gray-800">
              {partner.businessName || 'Not provided'}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Mobile Number</p>
            <p className="font-medium text-gray-800">
              {partner.mobileNumber}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Email</p>
            <p className="font-medium text-gray-800">
              {partner.email || 'Not provided'}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Address</p>
            <p className="font-medium text-gray-800">
              {partner.address || 'Not provided'}
            </p>
          </div>

          <div>
            <p className="text-sm text-gray-500">Verification Status</p>
            <span className="inline-block mt-1 rounded-full bg-yellow-100 px-3 py-1 text-sm font-medium text-yellow-700">
              {partner.verificationStatus}
            </span>
          </div>

        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-4">

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
          <p className="text-sm font-medium text-gray-500">
            Total Equipment
          </p>

          <h2 className="mt-2 text-3xl font-bold text-gray-800">
            0
          </h2>

          <p className="mt-1 text-xs text-gray-400">
            Equipment listed
          </p>
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
          <p className="text-sm font-medium text-gray-500">
            Active Bookings
          </p>

          <h2 className="mt-2 text-3xl font-bold text-gray-800">
            0
          </h2>

          <p className="mt-1 text-xs text-gray-400">
            Currently active
          </p>
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
          <p className="text-sm font-medium text-gray-500">
            Total Earnings
          </p>

          <h2 className="mt-2 text-3xl font-bold text-gray-800">
            ₹0
          </h2>

          <p className="mt-1 text-xs text-gray-400">
            Total revenue
          </p>
        </div>

        <div className="rounded-xl bg-white p-5 shadow-sm border border-gray-100">
          <p className="text-sm font-medium text-gray-500">
            Pending Requests
          </p>

          <h2 className="mt-2 text-3xl font-bold text-gray-800">
            0
          </h2>

          <p className="mt-1 text-xs text-gray-400">
            Need your action
          </p>
        </div>

      </div>

      {/* Quick Actions */}
      <div className="mt-8 rounded-xl bg-white shadow-sm border border-gray-100">

        <div className="border-b border-gray-100 px-6 py-4">
          <h2 className="text-lg font-semibold text-gray-800">
            Quick Actions
          </h2>
        </div>

        <div className="grid grid-cols-1 gap-4 p-6 sm:grid-cols-2 lg:grid-cols-4">

          <a
            href="/partner/equipment/add"
            className="rounded-lg border border-gray-200 p-4 hover:border-green-500 hover:bg-green-50"
          >
            <h3 className="font-semibold text-gray-700">
              Add Equipment
            </h3>

            <p className="mt-1 text-sm text-gray-500">
              List new farm equipment
            </p>
          </a>

          <a
            href="/partner/bookings"
            className="rounded-lg border border-gray-200 p-4 hover:border-green-500 hover:bg-green-50"
          >
            <h3 className="font-semibold text-gray-700">
              Booking Requests
            </h3>

            <p className="mt-1 text-sm text-gray-500">
              View pending requests
            </p>
          </a>

          <a
            href="/partner/earnings"
            className="rounded-lg border border-gray-200 p-4 hover:border-green-500 hover:bg-green-50"
          >
            <h3 className="font-semibold text-gray-700">
              View Earnings
            </h3>

            <p className="mt-1 text-sm text-gray-500">
              Track your revenue
            </p>
          </a>

          <a
            href="/partner/profile"
            className="rounded-lg border border-gray-200 p-4 hover:border-green-500 hover:bg-green-50"
          >
            <h3 className="font-semibold text-gray-700">
              My Profile
            </h3>

            <p className="mt-1 text-sm text-gray-500">
              Manage your profile
            </p>
          </a>

        </div>
      </div>

    </div>
  );
}

export default PartnerDashboard;