import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { equipmentService } from '../../services/equipmentService';
import { EQUIPMENT_CATEGORIES, FUEL_TYPES } from '../../utils/constants';

function AddEquipment() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const editId = searchParams.get('edit');
  const partnerId = localStorage.getItem('partnerId') || '1';

  const [formData, setFormData] = useState({
    name: '',
    category: 'TRACTOR',
    brand: '',
    model: '',
    manufacturingYear: new Date().getFullYear(),
    capacity: '',
    rentalPrice: '',
    fuelType: 'DIESEL',
    description: '',
    locationAddress: '',
    latitude: 18.5204,
    longitude: 73.8567,
    imageUrl: '',
  });

  const [loading, setLoading] = useState(false);
  const [fetchingEditData, setFetchingEditData] = useState(false);
  const [error, setError] = useState(null);
  const [conflictWarning, setConflictWarning] = useState(null);

  useEffect(() => {
    if (editId) {
      setFetchingEditData(true);
      equipmentService
        .getEquipmentById(editId)
        .then((data) => {
          if (data) {
            const primaryImg = data.images && data.images.length > 0
              ? (data.images.find((img) => img.isPrimary) || data.images[0]).imageUrl
              : '';

            setFormData({
              name: data.name || '',
              category: data.category || 'TRACTOR',
              brand: data.brand || '',
              model: data.model || '',
              manufacturingYear: data.manufacturingYear || new Date().getFullYear(),
              capacity: data.capacity || '',
              rentalPrice: data.rentalPrice || '',
              fuelType: data.fuelType || 'DIESEL',
              description: data.description || '',
              locationAddress: data.locationAddress || '',
              latitude: data.latitude || 18.5204,
              longitude: data.longitude || 73.8567,
              imageUrl: primaryImg,
            });
          }
        })
        .catch((err) => {
          console.error('Failed to fetch equipment for editing:', err);
          setError('Failed to load existing equipment details');
        })
        .finally(() => setFetchingEditData(false));
    }
  }, [editId]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setConflictWarning(null);

    // Form Client Validation
    if (!formData.name.trim() || !formData.brand.trim() || !formData.model.trim()) {
      setError('Please fill in all required machinery identification fields.');
      setLoading(false);
      return;
    }

    if (Number(formData.rentalPrice) <= 0) {
      setError('Daily rental price must be greater than zero.');
      setLoading(false);
      return;
    }

    try {
      const payload = {
        partnerId: Number(partnerId),
        name: formData.name.trim(),
        category: formData.category,
        brand: formData.brand.trim(),
        model: formData.model.trim(),
        manufacturingYear: Number(formData.manufacturingYear),
        capacity: formData.capacity.trim(),
        rentalPrice: Number(formData.rentalPrice),
        fuelType: formData.fuelType,
        description: formData.description.trim(),
        locationAddress: formData.locationAddress.trim(),
        latitude: Number(formData.latitude),
        longitude: Number(formData.longitude),
        images: formData.imageUrl.trim()
          ? [
              {
                imageUrl: formData.imageUrl.trim(),
                isPrimary: true,
                displayOrder: 1,
              },
            ]
          : [],
      };

      if (editId) {
        await equipmentService.updateEquipment(editId, payload, partnerId);
      } else {
        await equipmentService.createEquipment(payload, partnerId);
      }

      navigate('/partner/equipment');
    } catch (err) {
      console.error('Equipment submit error:', err);
      if (err.status === 409) {
        setConflictWarning(err.message || 'This equipment was updated by another session. Please refresh and try again.');
      } else {
        setError(err.message || 'Failed to save equipment. Please check all fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (fetchingEditData) {
    return (
      <div className="max-w-3xl mx-auto p-6 space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="h-64 bg-gray-200 rounded-xl" />
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-3xl font-bold text-green-800 tracking-tight">
            {editId ? 'Edit Machinery Listing' : 'Add New Equipment'}
          </h1>
          <p className="text-gray-600 mt-1">
            {editId
              ? 'Update specifications, rental rates, and location details.'
              : 'List new farm machinery to make it available for regional rental.'}
          </p>
        </div>
        <button
          onClick={() => navigate('/partner/equipment')}
          className="text-sm font-semibold text-gray-600 hover:text-gray-800 transition"
        >
          Cancel
        </button>
      </div>

      {/* Warnings & Alerts */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm">
          ⚠️ {error}
        </div>
      )}

      {conflictWarning && (
        <div className="bg-amber-50 border border-amber-300 rounded-xl p-4 text-amber-800 text-sm flex items-center justify-between">
          <span>⚠️ {conflictWarning}</span>
          <button
            onClick={() => window.location.reload()}
            className="px-3 py-1 bg-amber-700 text-white font-semibold rounded-lg hover:bg-amber-800 text-xs"
          >
            Refresh Latest Data
          </button>
        </div>
      )}

      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 space-y-6 shadow-sm">
        {/* Basic Details */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-green-800 uppercase tracking-wide border-b border-gray-100 pb-2">
            1. Machinery Identification
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="md:col-span-2">
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Equipment Title / Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="name"
                required
                placeholder="e.g. John Deere 5050D Tractor"
                value={formData.name}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Category <span className="text-red-500">*</span>
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              >
                {EQUIPMENT_CATEGORIES.map((cat) => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Fuel Type <span className="text-red-500">*</span>
              </label>
              <select
                name="fuelType"
                value={formData.fuelType}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              >
                {FUEL_TYPES.map((fuel) => (
                  <option key={fuel.value} value={fuel.value}>
                    {fuel.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Brand / Manufacturer <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="brand"
                required
                placeholder="e.g. Mahindra, John Deere"
                value={formData.brand}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Model Number <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="model"
                required
                placeholder="e.g. 5050D"
                value={formData.model}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Manufacturing Year <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="manufacturingYear"
                required
                min="1990"
                max={new Date().getFullYear()}
                value={formData.manufacturingYear}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Power / Capacity Specs <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="capacity"
                required
                placeholder="e.g. 50 HP, 5 Tonnes"
                value={formData.capacity}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>
          </div>
        </div>

        {/* Pricing & Location */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-green-800 uppercase tracking-wide border-b border-gray-100 pb-2">
            2. Pricing & Operational Location
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Daily Rental Price (₹/day) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                name="rentalPrice"
                required
                min="1"
                step="0.01"
                placeholder="e.g. 2500"
                value={formData.rentalPrice}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm font-bold text-green-800 focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
                Location Address / Hub <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="locationAddress"
                required
                placeholder="e.g. Talegaon, Pune, MH"
                value={formData.locationAddress}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Latitude</label>
              <input
                type="number"
                step="any"
                name="latitude"
                value={formData.latitude}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Longitude</label>
              <input
                type="number"
                step="any"
                name="longitude"
                value={formData.longitude}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
              />
            </div>
          </div>
        </div>

        {/* Media & Description */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold text-green-800 uppercase tracking-wide border-b border-gray-100 pb-2">
            3. Media & Operational Description
          </h2>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Primary Image URL</label>
            <input
              type="url"
              name="imageUrl"
              placeholder="https://images.unsplash.com/photo-..."
              value={formData.imageUrl}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
            <p className="text-[11px] text-gray-500 mt-1">
              Provide a valid HTTP/HTTPS image URL. If left empty, a default fallback image will be rendered.
            </p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">
              Detailed Description <span className="text-red-500">*</span>
            </label>
            <textarea
              name="description"
              required
              rows={4}
              placeholder="Describe machine condition, attachments included, and operational instructions..."
              value={formData.description}
              onChange={handleChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Submit */}
        <div className="flex justify-end gap-4 border-t border-gray-100 pt-4">
          <button
            type="button"
            onClick={() => navigate('/partner/equipment')}
            className="px-5 py-2.5 text-sm font-semibold text-gray-700 bg-gray-100 rounded-xl hover:bg-gray-200 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 text-sm font-bold text-white bg-green-700 rounded-xl hover:bg-green-800 disabled:opacity-50 transition shadow-md flex items-center gap-2"
          >
            {loading ? 'Saving Machinery...' : editId ? 'Update Machinery' : 'Publish Listing'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default AddEquipment;
