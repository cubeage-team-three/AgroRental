import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import {
  Tractor,
  Image as ImageIcon,
  DollarSign,
  MapPin,
  FileText,
  AlertCircle,
  CheckCircle2,
  ArrowLeft,
  Save,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { equipmentService } from '../../services/equipmentService';
import { EQUIPMENT_CATEGORIES, FUEL_TYPES, DEFAULT_EQUIPMENT_IMAGE } from '../../utils/constants';

// Sample presets for quick demo image selection
const SAMPLE_PRESET_IMAGES = [
  { label: 'Tractor 50HP', url: 'https://images.unsplash.com/photo-1592878904946-b3cd8ae243d0?auto=format&fit=crop&w=800&q=80' },
  { label: 'Rotavator / Tiller', url: 'https://images.unsplash.com/photo-1589876735500-b6f75607d722?auto=format&fit=crop&w=800&q=80' },
  { label: 'Combined Harvester', url: 'https://images.unsplash.com/photo-1595273670150-bd0c3c392e46?auto=format&fit=crop&w=800&q=80' },
  { label: 'Seeder & Planter', url: 'https://images.unsplash.com/photo-1628352081506-83c43123ed6d?auto=format&fit=crop&w=800&q=80' },
];

function AddEquipment() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const editId = searchParams.get('edit');
  const partnerId = getPartnerId();

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

  // Load equipment details when in Edit mode
  useEffect(() => {
    if (!editId) return;

    setFetchingEditData(true);
    equipmentService
      .getEquipmentById(editId)
      .then((data) => {
        if (!data) return;

        const primaryImage =
          data.images && data.images.length > 0
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
          imageUrl: primaryImage,
        });
      })
      .catch((err) => {
        console.error('Failed to fetch equipment for editing:', err);
        setError('Failed to load existing machinery details.');
      })
      .finally(() => {
        setFetchingEditData(false);
      });
  }, [editId]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (error) setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setConflictWarning(null);

    // Form Validations
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
        setConflictWarning(
          err.message || 'This machinery listing was modified in another session. Please refresh.'
        );
      } else {
        setError(err.message || 'Failed to save machinery listing. Please verify all required fields.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (fetchingEditData) {
    return (
      <div className="max-w-4xl mx-auto p-8 space-y-4 animate-pulse">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="h-96 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/partner/equipment"
              className="p-1.5 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              {editId ? 'Edit Machinery Listing' : 'List New Farm Equipment'}
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            {editId
              ? 'Update technical specifications, rental tariffs, and base location details.'
              : 'Add your tractor, harvester, tiller, or implement to regional farmer discovery.'}
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/partner/equipment')}
          className="text-xs font-bold text-gray-500 hover:text-gray-900 transition-colors"
        >
          Cancel & Return
        </button>
      </div>

      {/* Error & Warning Banners */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
        </div>
      )}

      {conflictWarning && (
        <div className="p-4 bg-amber-50 border-l-4 border-amber-500 rounded-2xl text-amber-800 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-amber-600" />
            <span>{conflictWarning}</span>
          </div>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="px-3 py-1 bg-amber-700 text-white font-bold text-xs rounded-lg hover:bg-amber-800"
          >
            Refresh
          </button>
        </div>
      )}

      {/* Main Multi-Section Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-3xl border border-gray-100 p-6 sm:p-8 shadow-sm space-y-8">
        
        {/* Section 1: Machinery Identification */}
        <div className="space-y-4">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <Tractor className="w-5 h-5 text-[#3E7B27]" />
            <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
              1. Machinery Identification & Specs
            </h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Title / Name */}
            <div className="sm:col-span-2">
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Machinery Title / Listing Name <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="name"
                required
                placeholder="e.g. John Deere 5050D 50HP 4WD Tractor"
                value={formData.name}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Category */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Machinery Category <span className="text-red-500">*</span>
              </label>
              <select
                name="category"
                value={formData.category}
                onChange={handleChange}
                required
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all cursor-pointer"
              >
                {EQUIPMENT_CATEGORIES.map((cat) => (
                  <option key={cat.value} value={cat.value}>
                    {cat.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Fuel Type */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Fuel / Power Source <span className="text-red-500">*</span>
              </label>
              <select
                name="fuelType"
                value={formData.fuelType}
                onChange={handleChange}
                required
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all cursor-pointer"
              >
                {FUEL_TYPES.map((fuel) => (
                  <option key={fuel.value} value={fuel.value}>
                    {fuel.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Brand */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Brand / Manufacturer <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="brand"
                required
                placeholder="e.g. Mahindra, John Deere, Sonalika"
                value={formData.brand}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Model */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Model Number / Variant <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="model"
                required
                placeholder="e.g. 575 DI / 5050D"
                value={formData.model}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Manufacturing Year */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
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
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Capacity / Power Specs */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Power / Engine Capacity Specs <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="capacity"
                required
                placeholder="e.g. 50 HP, 6 Feet Cut, 5 Ton"
                value={formData.capacity}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

          </div>
        </div>

        {/* Section 2: Pricing & Location */}
        <div className="space-y-4">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <DollarSign className="w-5 h-5 text-[#3E7B27]" />
            <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
              2. Rental Pricing & Operational Hub
            </h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            
            {/* Daily Rental Price */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Daily Rental Price (₹/day) <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500 font-bold text-sm">₹</span>
                <input
                  type="number"
                  name="rentalPrice"
                  required
                  min="1"
                  step="0.01"
                  placeholder="e.g. 2500"
                  value={formData.rentalPrice}
                  onChange={handleChange}
                  className="w-full pl-8 pr-4 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-base font-black text-[#142E1C] focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>
            </div>

            {/* Location Address */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Base Hub Location Address <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="locationAddress"
                required
                placeholder="e.g. Talegaon Dabhade, Pune, MH 410506"
                value={formData.locationAddress}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

          </div>
        </div>

        {/* Section 3: Photos & Description */}
        <div className="space-y-4">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <ImageIcon className="w-5 h-5 text-[#3E7B27]" />
            <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
              3. Machinery Photos & Operational Notes
            </h2>
          </div>

          <div className="space-y-4">
            
            {/* Image URL with Live Preview */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Primary Machinery Photo URL
              </label>
              <input
                type="url"
                name="imageUrl"
                placeholder="https://images.unsplash.com/..."
                value={formData.imageUrl}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Sample Quick Selector */}
            <div className="flex items-center gap-2 flex-wrap text-xs">
              <span className="text-gray-400 font-bold text-[11px] uppercase">Quick Presets:</span>
              {SAMPLE_PRESET_IMAGES.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setFormData((prev) => ({ ...prev, imageUrl: preset.url }))}
                  className="px-2.5 py-1 bg-gray-100 hover:bg-emerald-100 text-gray-700 hover:text-emerald-900 rounded-lg font-semibold transition-colors"
                >
                  📷 {preset.label}
                </button>
              ))}
            </div>

            {/* Live Preview Thumbnail */}
            {formData.imageUrl && (
              <div className="p-3 bg-[#F8FAF8] rounded-2xl border border-gray-200/80 flex items-center gap-4">
                <div className="w-20 h-20 rounded-xl overflow-hidden bg-gray-200 shrink-0">
                  <img
                    src={formData.imageUrl}
                    alt="Preview"
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                    }}
                  />
                </div>
                <div className="text-xs text-gray-600">
                  <span className="font-bold text-gray-900 block">Live Photo Preview</span>
                  <span className="text-[11px] text-emerald-700">Valid thumbnail loaded</span>
                </div>
              </div>
            )}

            {/* Detailed Description */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Detailed Condition & Attachments Description <span className="text-red-500">*</span>
              </label>
              <textarea
                name="description"
                required
                rows={3}
                placeholder="Describe operational readiness, tyre condition, implements included (e.g. trolley, rotavator blades), and driver requirement..."
                value={formData.description}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

          </div>
        </div>

        {/* Form Action Footer */}
        <div className="flex items-center justify-end gap-3 pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate('/partner/equipment')}
            className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors"
          >
            Cancel
          </button>

          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-extrabold rounded-xl shadow-md hover:shadow-lg transition-all flex items-center gap-2 disabled:opacity-70"
          >
            {loading ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Saving Machinery Listing...</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>{editId ? 'Update Machinery' : 'Publish Listing'}</span>
              </>
            )}
          </button>
        </div>

      </form>

    </div>
  );
}

export default AddEquipment;
