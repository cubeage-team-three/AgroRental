import { useEffect, useState } from 'react';

function MyEquipment() {
  const [equipment, setEquipment] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Testing sathi Ramesh Yadav cha partner ID
  const partnerId = 1;

  useEffect(() => {
    fetchEquipment();
  }, []);

  const fetchEquipment = async () => {
    try {
      setLoading(true);
      setError('');

      const response = await fetch(
        `http://localhost:8080/api/equipment/partner/${partnerId}`
      );

      if (!response.ok) {
        throw new Error('Failed to fetch equipment');
      }

      const data = await response.json();
      setEquipment(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const getCategoryLabel = (category) => {
    return category
      ? category
          .toLowerCase()
          .replace('_', ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase())
      : '-';
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-6xl">

        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-800">
              My Equipment
            </h1>
            <p className="mt-1 text-gray-500">
              Manage your listed agricultural equipment.
            </p>
          </div>

          <a
            href="/partner/equipment/add"
            className="rounded-lg bg-green-700 px-5 py-3 font-medium text-white hover:bg-green-800"
          >
            + Add Equipment
          </a>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
            {error}
          </div>
        )}

        {/* Loading */}
        {loading && (
          <div className="rounded-xl bg-white p-10 text-center shadow-sm">
            <p className="text-gray-500">
              Loading equipment...
            </p>
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && equipment.length === 0 && (
          <div className="rounded-xl border border-gray-100 bg-white p-12 text-center shadow-sm">
            <div className="text-5xl">🚜</div>

            <h2 className="mt-4 text-xl font-semibold text-gray-700">
              No Equipment Added
            </h2>

            <p className="mt-2 text-gray-500">
              Add your first agricultural equipment to start receiving bookings.
            </p>

            <a
              href="/partner/equipment/add"
              className="mt-5 inline-block rounded-lg bg-green-700 px-5 py-3 font-medium text-white hover:bg-green-800"
            >
              Add Equipment
            </a>
          </div>
        )}

        {/* Equipment Cards */}
        {!loading && equipment.length > 0 && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {equipment.map((item) => (
              <div
                key={item.id}
                className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm"
              >
                {/* Equipment Image Placeholder */}
                <div className="flex h-40 items-center justify-center bg-green-50 text-6xl">
                  🚜
                </div>

                <div className="p-5">

                  <div className="flex items-start justify-between gap-3">
                    <h2 className="text-xl font-bold text-gray-800">
                      {item.name}
                    </h2>

                    <span
                      className={`rounded-full px-3 py-1 text-xs font-medium ${
                        item.availabilityStatus === 'AVAILABLE'
                          ? 'bg-green-100 text-green-700'
                          : 'bg-yellow-100 text-yellow-700'
                      }`}
                    >
                      {item.availabilityStatus || 'AVAILABLE'}
                    </span>
                  </div>

                  <p className="mt-2 text-sm text-gray-500">
                    {item.brand} {item.model}
                  </p>

                  <div className="mt-4 space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-500">
                        Category
                      </span>
                      <span className="font-medium text-gray-700">
                        {getCategoryLabel(item.category)}
                      </span>
                    </div>

                    <div className="flex justify-between">
                      <span className="text-gray-500">
                        Capacity
                      </span>
                      <span className="font-medium text-gray-700">
                        {item.capacity}
                      </span>
                    </div>

                    <div className="flex justify-between">
                      <span className="text-gray-500">
                        Fuel
                      </span>
                      <span className="font-medium text-gray-700">
                        {getCategoryLabel(item.fuelType)}
                      </span>
                    </div>

                    <div className="flex justify-between">
                      <span className="text-gray-500">
                        Rental Price
                      </span>
                      <span className="font-bold text-green-700">
                        ₹{item.rentalPrice}
                      </span>
                    </div>
                  </div>

                  <div className="mt-4 border-t border-gray-100 pt-4">
                    <p className="text-sm text-gray-500">
                      📍 {item.locationAddress}
                    </p>
                  </div>

                  <div className="mt-5">
                    <button
                      type="button"
                      className="w-full rounded-lg border border-green-700 px-4 py-2 font-medium text-green-700 hover:bg-green-50"
                    >
                      View Details
                    </button>
                  </div>

                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  );
}

export default MyEquipment;