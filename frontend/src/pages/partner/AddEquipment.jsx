import { useState } from 'react';

function AddEquipment() {
  const [formData, setFormData] = useState({
    name: '',
    category: '',
    brand: '',
    model: '',
    manufacturingYear: '',
    capacity: '',
    rentalPrice: '',
    fuelType: '',
    description: '',
    locationAddress: '',
    latitude: '',
    longitude: '',
    maintenanceNotes: '',
    partnerId: 1,
  });

  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    setMessage('');
    setError('');

    try {
      const response = await fetch('http://localhost:8080/api/equipment', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          ...formData,
          manufacturingYear: Number(formData.manufacturingYear),
          rentalPrice: Number(formData.rentalPrice),
          latitude: Number(formData.latitude),
          longitude: Number(formData.longitude),
          partnerId: Number(formData.partnerId),
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => null);
        throw new Error(
          errorData?.message || 'Failed to add equipment'
        );
      }

      await response.json();

      setMessage('Equipment added successfully!');

      setFormData({
        name: '',
        category: '',
        brand: '',
        model: '',
        manufacturingYear: '',
        capacity: '',
        rentalPrice: '',
        fuelType: '',
        description: '',
        locationAddress: '',
        latitude: '',
        longitude: '',
        maintenanceNotes: '',
        partnerId: 1,
      });
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-4xl">
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-800">
            Add Equipment
          </h1>
          <p className="mt-1 text-gray-500">
            Add your agricultural equipment for rental.
          </p>
        </div>

        {message && (
          <div className="mb-5 rounded-lg border border-green-200 bg-green-50 p-4 text-green-700">
            {message}
          </div>
        )}

        {error && (
          <div className="mb-5 rounded-lg border border-red-200 bg-red-50 p-4 text-red-700">
            {error}
          </div>
        )}

        <form
          onSubmit={handleSubmit}
          className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm"
        >
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2">

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Equipment Name
              </label>
              <input
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="e.g. John Deere Tractor"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Category
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              >
                <option value="">Select Category</option>
                <option value="TRACTOR">Tractor</option>
                <option value="HARVESTER">Harvester</option>
                <option value="TILLER">Tiller</option>
                <option value="IRRIGATION">Irrigation</option>
                <option value="SEEDER">Seeder</option>
                <option value="SPRAYER">Sprayer</option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Brand
              </label>
              <input
                name="brand"
                value={formData.brand}
                onChange={handleChange}
                placeholder="e.g. John Deere"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Model
              </label>
              <input
                name="model"
                value={formData.model}
                onChange={handleChange}
                placeholder="e.g. 5310"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Manufacturing Year
              </label>
              <input
                type="number"
                name="manufacturingYear"
                value={formData.manufacturingYear}
                onChange={handleChange}
                placeholder="2024"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Capacity
              </label>
              <input
                name="capacity"
                value={formData.capacity}
                onChange={handleChange}
                placeholder="e.g. 50 HP"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Rental Price
              </label>
              <input
                type="number"
                step="0.01"
                name="rentalPrice"
                value={formData.rentalPrice}
                onChange={handleChange}
                placeholder="e.g. 1500"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Fuel Type
              </label>
              <select
                name="fuelType"
                value={formData.fuelType}
                onChange={handleChange}
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              >
                <option value="">Select Fuel Type</option>
                <option value="DIESEL">Diesel</option>
                <option value="PETROL">Petrol</option>
                <option value="ELECTRIC">Electric</option>
                <option value="HYBRID">Hybrid</option>
                <option value="MANUAL_HUMAN_POWERED">
                  Manual / Human Powered
                </option>
                <option value="OTHER">Other</option>
              </select>
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Description
              </label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Describe your equipment..."
                required
                rows="4"
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Location Address
              </label>
              <input
                name="locationAddress"
                value={formData.locationAddress}
                onChange={handleChange}
                placeholder="e.g. Pune, Maharashtra"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Latitude
              </label>
              <input
                type="number"
                step="any"
                name="latitude"
                value={formData.latitude}
                onChange={handleChange}
                placeholder="18.5204"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Longitude
              </label>
              <input
                type="number"
                step="any"
                name="longitude"
                value={formData.longitude}
                onChange={handleChange}
                placeholder="73.8567"
                required
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium text-gray-700">
                Maintenance Notes
              </label>
              <textarea
                name="maintenanceNotes"
                value={formData.maintenanceNotes}
                onChange={handleChange}
                placeholder="Optional maintenance information"
                rows="3"
                className="w-full rounded-lg border border-gray-300 px-3 py-2"
              />
            </div>
          </div>

          <div className="mt-6 flex justify-end">
            <button
              type="submit"
              className="rounded-lg bg-green-700 px-6 py-3 font-medium text-white hover:bg-green-800"
            >
              Add Equipment
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default AddEquipment;