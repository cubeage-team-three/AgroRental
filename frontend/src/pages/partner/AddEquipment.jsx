import { useState, useEffect, useRef } from 'react';
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
  Upload,
  Camera,
  UploadCloud,
  X,
  FileImage,
  Check,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { equipmentService } from '../../services/equipmentService';
import { EQUIPMENT_CATEGORIES, FUEL_TYPES, DEFAULT_EQUIPMENT_IMAGE, API_BASE_URL } from '../../utils/constants';

// Sample presets for quick demo image selection
const SAMPLE_PRESET_IMAGES = [
  { label: 'Tractor 50HP', url: 'https://images.unsplash.com/photo-1592878904946-b3cd8ae243d0?auto=format&fit=crop&w=800&q=80' },
  { label: 'Rotavator / Tiller', url: 'https://images.unsplash.com/photo-1589876735500-b6f75607d722?auto=format&fit=crop&w=800&q=80' },
  { label: 'Combined Harvester', url: 'https://images.unsplash.com/photo-1595273670150-bd0c3c392e46?auto=format&fit=crop&w=800&q=80' },
  { label: 'Seeder & Planter', url: 'https://images.unsplash.com/photo-1628352081506-83c43123ed6d?auto=format&fit=crop&w=800&q=80' },
];

function resolveImageUrl(url) {
  if (!url) return DEFAULT_EQUIPMENT_IMAGE;
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('blob:')) {
    return url;
  }
  if (url.startsWith('/uploads/')) {
    const baseUrl = API_BASE_URL.endsWith('/api') ? API_BASE_URL.slice(0, -4) : API_BASE_URL;
    return `${baseUrl}${url}`;
  }
  return url;
}

function AddEquipment() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const editId = searchParams.get('edit');
  const partnerId = getPartnerId();

  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);

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
    availabilityStatus: 'AVAILABLE',
    isDisabled: false,
    maintenanceNotes: '',
  });

  const [loading, setLoading] = useState(false);
  const [fetchingEditData, setFetchingEditData] = useState(false);
  const [error, setError] = useState(null);
  const [conflictWarning, setConflictWarning] = useState(null);

  // File Upload states
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [localPreviewUrl, setLocalPreviewUrl] = useState(null);
  const [selectedFileDetails, setSelectedFileDetails] = useState(null);

  // Clean up object URLs to prevent memory leaks
  useEffect(() => {
    return () => {
      if (localPreviewUrl) {
        URL.revokeObjectURL(localPreviewUrl);
      }
    };
  }, [localPreviewUrl]);

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
          availabilityStatus: data.availabilityStatus || 'AVAILABLE',
          isDisabled: data.isDisabled ?? false,
          maintenanceNotes: data.maintenanceNotes || '',
        });
      })
      .catch((err) => {
        console.error('Failed to fetch equipment for editing:', err);
        setError('Failed to load existing equipment details.');
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

  // Image Upload Processing & Validation
  const processFile = async (file) => {
    if (!file) return;

    // Validation 1: Allowed image types
    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!validTypes.includes(file.type.toLowerCase())) {
      setUploadError('Invalid file type. Only JPEG, PNG, and WebP images are supported.');
      return;
    }

    // Validation 2: Max 10MB file size
    if (file.size > 10 * 1024 * 1024) {
      setUploadError('File size exceeds limit. Maximum allowed image size is 10 MB.');
      return;
    }

    setUploadError(null);

    // Create local instant preview
    if (localPreviewUrl) {
      URL.revokeObjectURL(localPreviewUrl);
    }
    const objectUrl = URL.createObjectURL(file);
    setLocalPreviewUrl(objectUrl);
    setSelectedFileDetails({
      name: file.name,
      size: (file.size / (1024 * 1024)).toFixed(2) + ' MB',
    });

    // Upload file to backend server
    try {
      setUploadingImage(true);
      const result = await equipmentService.uploadEquipmentImage(file);
      if (result && result.url) {
        setFormData((prev) => ({ ...prev, imageUrl: result.url }));
        if (error) setError(null);
      }
    } catch (err) {
      console.error('Image upload failed:', err);
      setUploadError(err.message || 'Image upload failed. Please try again.');
    } finally {
      setUploadingImage(false);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files && e.target.files[0];
    if (file) processFile(file);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files && e.dataTransfer.files[0];
    if (file) processFile(file);
  };

  const handleRemoveImage = () => {
    if (localPreviewUrl) {
      URL.revokeObjectURL(localPreviewUrl);
    }
    setLocalPreviewUrl(null);
    setSelectedFileDetails(null);
    setUploadError(null);
    setFormData((prev) => ({ ...prev, imageUrl: '' }));
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (cameraInputRef.current) cameraInputRef.current.value = '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (uploadingImage) {
      setError('Please wait until image upload finishes before submitting.');
      return;
    }

    setLoading(true);
    setError(null);
    setConflictWarning(null);

    // Form Validations
    if (!formData.name.trim()) {
      setError('Equipment name is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.category) {
      setError('Equipment category is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.brand.trim()) {
      setError('Brand is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.model.trim()) {
      setError('Model is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.manufacturingYear || isNaN(Number(formData.manufacturingYear))) {
      setError('Manufacturing year is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.capacity.trim()) {
      setError('Capacity is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.rentalPrice || Number(formData.rentalPrice) <= 0) {
      setError('Daily rental price must be greater than zero.');
      setLoading(false);
      return;
    }
    if (!formData.fuelType) {
      setError('Fuel type is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.locationAddress.trim()) {
      setError('Location address is mandatory');
      setLoading(false);
      return;
    }
    if (formData.latitude === undefined || formData.latitude === null || isNaN(Number(formData.latitude))) {
      setError('Latitude is mandatory');
      setLoading(false);
      return;
    }
    if (formData.longitude === undefined || formData.longitude === null || isNaN(Number(formData.longitude))) {
      setError('Longitude is mandatory');
      setLoading(false);
      return;
    }
    if (!formData.imageUrl.trim()) {
      setError('At least one equipment image is required. Please upload an image.');
      setLoading(false);
      return;
    }
    if (!formData.description.trim()) {
      setError('Description is mandatory');
      setLoading(false);
      return;
    }
    if (!partnerId) {
      setError('Partner ID is mandatory');
      setLoading(false);
      return;
    }

    if (editId && !formData.availabilityStatus) {
      setError('Availability status is mandatory');
      setLoading(false);
      return;
    }
    if (editId && (formData.isDisabled === undefined || formData.isDisabled === null)) {
      setError('Disabled status is mandatory');
      setLoading(false);
      return;
    }

    try {
      const payload = {
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
        images: [
          {
            imageUrl: formData.imageUrl.trim(),
            isPrimary: true,
            displayOrder: 1,
          },
        ],
      };

      if (editId) {
        payload.availabilityStatus = formData.availabilityStatus;
        payload.isDisabled = formData.isDisabled;
        payload.maintenanceNotes = formData.maintenanceNotes ? formData.maintenanceNotes.trim() : null;
        await equipmentService.updateEquipment(editId, payload, partnerId);
      } else {
        payload.partnerId = Number(partnerId);
        await equipmentService.createEquipment(payload, partnerId);
      }

      navigate('/partner/equipment');
    } catch (err) {
      console.error('Equipment submit error:', err);
      if (err.status === 409) {
        setConflictWarning(
          err.message || 'This equipment was updated by another session. Please refresh and try again.'
        );
      } else if (err.status === 400 && err.data?.data) {
        const fieldErrors = Object.values(err.data.data).join(', ');
        setError(fieldErrors || 'Validation Error: Please check all fields.');
      } else {
        setError(err.message || 'Failed to save equipment. Please check all fields.');
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

  const effectiveDisplayImage = localPreviewUrl || resolveImageUrl(formData.imageUrl);

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
              {editId ? 'Edit Equipment Listing' : 'Add New Equipment'}
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            {editId
              ? 'Update specifications, rental rates, and location details.'
              : 'List new farm equipment to make it available for regional rental.'}
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

        {/* Section 1: Equipment Identification */}
        <div className="space-y-4">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <Tractor className="w-5 h-5 text-[#3E7B27]" />
            <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
              1. Equipment Identification & Specs
            </h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">

            {/* Title / Name */}
            <div className="sm:col-span-2">
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Equipment Title / Listing Name <span className="text-red-500">*</span>
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

            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Equipment Category <span className="text-red-500">*</span>
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

            {/* Latitude */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Latitude</label>
              <input
                type="number"
                step="any"
                name="latitude"
                value={formData.latitude}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

            {/* Longitude */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">Longitude</label>
              <input
                type="number"
                step="any"
                name="longitude"
                value={formData.longitude}
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
              3. Equipment Photos & Operational Notes
            </h2>
          </div>

          <div className="space-y-4">

            {/* Hidden File Inputs for Desktop Gallery & Mobile Camera */}
            <input
              type="file"
              ref={fileInputRef}
              accept="image/jpeg,image/jpg,image/png,image/webp"
              onChange={handleFileChange}
              className="hidden"
            />
            <input
              type="file"
              ref={cameraInputRef}
              accept="image/jpeg,image/jpg,image/png,image/webp"
              capture="environment"
              onChange={handleFileChange}
              className="hidden"
            />

            <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider">
              Equipment Primary Image <span className="text-red-500">*</span>
            </label>

            {/* Upload Error Banner */}
            {uploadError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-xl text-xs font-semibold text-red-700 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
                  <span>{uploadError}</span>
                </div>
                <button
                  type="button"
                  onClick={() => setUploadError(null)}
                  className="text-red-500 hover:text-red-700 text-xs font-bold"
                >
                  Dismiss
                </button>
              </div>
            )}

            {/* Upload Area / Dropzone / Active Image Preview */}
            {!formData.imageUrl && !localPreviewUrl ? (
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`relative border-2 border-dashed rounded-3xl p-8 text-center transition-all cursor-pointer ${
                  isDragging
                    ? 'border-[#3E7B27] bg-emerald-50/60 scale-[1.01]'
                    : 'border-gray-300 bg-[#F0EFE9]/50 hover:bg-[#F0EFE9] hover:border-gray-400'
                }`}
                onClick={() => fileInputRef.current?.click()}
              >
                <div className="mx-auto w-14 h-14 rounded-2xl bg-white border border-gray-200 flex items-center justify-center shadow-xs mb-3">
                  <UploadCloud className="w-7 h-7 text-[#3E7B27]" />
                </div>
                <p className="text-sm font-extrabold text-[#142E1C] mb-1">
                  Drag & drop machinery image here
                </p>
                <p className="text-xs text-gray-500 mb-4">
                  or choose from gallery / capture photo on mobile
                </p>

                <div className="flex items-center justify-center gap-3 flex-wrap">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      fileInputRef.current?.click();
                    }}
                    className="px-4 py-2 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl transition-all flex items-center gap-1.5 shadow-xs"
                  >
                    <Upload className="w-3.5 h-3.5" />
                    <span>Choose Image</span>
                  </button>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      cameraInputRef.current?.click();
                    }}
                    className="px-4 py-2 bg-white hover:bg-gray-100 border border-gray-300 text-gray-700 text-xs font-bold rounded-xl transition-all flex items-center gap-1.5 shadow-xs sm:hidden"
                  >
                    <Camera className="w-3.5 h-3.5 text-gray-600" />
                    <span>Take Photo</span>
                  </button>
                </div>

                <p className="text-[11px] text-gray-400 font-medium mt-3">
                  Supports: JPG, PNG, WebP • Maximum file size: 10MB
                </p>
              </div>
            ) : (
              /* Selected / Uploaded Image Preview Box */
              <div className="p-4 bg-[#F8FAF8] rounded-3xl border border-gray-200/80 space-y-4">
                <div className="flex items-start sm:items-center justify-between gap-4 flex-col sm:flex-row">
                  <div className="flex items-center gap-4">
                    <div className="relative w-24 h-24 rounded-2xl overflow-hidden bg-gray-200 shrink-0 border border-gray-200 shadow-xs">
                      <img
                        src={effectiveDisplayImage}
                        alt="Equipment Preview"
                        className="w-full h-full object-cover"
                        onError={(e) => {
                          e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                        }}
                      />
                      {uploadingImage && (
                        <div className="absolute inset-0 bg-black/50 backdrop-blur-xs flex items-center justify-center">
                          <RefreshCw className="w-6 h-6 text-white animate-spin" />
                        </div>
                      )}
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <FileImage className="w-4 h-4 text-gray-600" />
                        <span className="text-sm font-black text-[#142E1C] truncate max-w-[200px]">
                          {selectedFileDetails?.name || 'Equipment Photo'}
                        </span>
                      </div>

                      {selectedFileDetails?.size && (
                        <span className="text-xs text-gray-500 font-medium block">
                          Size: {selectedFileDetails.size}
                        </span>
                      )}

                      {/* Status Badge */}
                      {uploadingImage ? (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-50 text-amber-700 border border-amber-200 text-xs font-bold">
                          <RefreshCw className="w-3 h-3 animate-spin" />
                          <span>Uploading image...</span>
                        </div>
                      ) : formData.imageUrl ? (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold">
                          <Check className="w-3 h-3 text-emerald-600" />
                          <span>Image uploaded & verified</span>
                        </div>
                      ) : (
                        <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-red-50 text-red-700 border border-red-200 text-xs font-bold">
                          <AlertCircle className="w-3 h-3 text-red-500" />
                          <span>Upload failed</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploadingImage}
                      className="px-3.5 py-2 bg-white hover:bg-gray-50 border border-gray-300 text-gray-700 text-xs font-bold rounded-xl transition-all disabled:opacity-50"
                    >
                      Change Image
                    </button>
                    <button
                      type="button"
                      onClick={handleRemoveImage}
                      disabled={uploadingImage}
                      className="p-2 bg-red-50 hover:bg-red-100 text-red-600 rounded-xl transition-all disabled:opacity-50"
                      title="Remove image"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Quick Presets Fallback Option */}
            <div className="flex items-center gap-2 flex-wrap text-xs pt-1">
              <span className="text-gray-400 font-bold text-[11px] uppercase">Quick Demo Presets:</span>
              {SAMPLE_PRESET_IMAGES.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    handleRemoveImage();
                    setFormData((prev) => ({ ...prev, imageUrl: preset.url }));
                  }}
                  className="px-2.5 py-1 bg-gray-100 hover:bg-emerald-100 text-gray-700 hover:text-emerald-900 rounded-lg font-semibold transition-colors"
                >
                  📷 {preset.label}
                </button>
              ))}
            </div>

            {/* Detailed Description */}
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                Detailed Condition & Attachments Description <span className="text-red-500">*</span>
              </label>
              <textarea
                name="description"
                required
                rows={3}
                placeholder="Describe machine condition, attachments included, and operational instructions..."
                value={formData.description}
                onChange={handleChange}
                className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
              />
            </div>

          </div>
        </div>

        {/* Section 4: Status & Maintenance (Edit mode only) */}
        {editId && (
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <AlertCircle className="w-5 h-5 text-[#3E7B27]" />
              <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
                4. Availability & Status Settings
              </h2>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Availability Status */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Availability Status <span className="text-red-500">*</span>
                </label>
                <select
                  name="availabilityStatus"
                  value={formData.availabilityStatus}
                  onChange={handleChange}
                  required
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all cursor-pointer"
                >
                  <option value="AVAILABLE">Available</option>
                  <option value="BOOKED">Booked</option>
                  <option value="UNDER_MAINTENANCE">Under Maintenance</option>
                  <option value="INACTIVE">Inactive</option>
                </select>
              </div>

              {/* Is Disabled */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Listing Status <span className="text-red-500">*</span>
                </label>
                <select
                  name="isDisabled"
                  value={formData.isDisabled.toString()}
                  onChange={(e) => {
                    setFormData((prev) => ({
                      ...prev,
                      isDisabled: e.target.value === 'true',
                    }));
                  }}
                  required
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all cursor-pointer"
                >
                  <option value="false">Active / Visible</option>
                  <option value="true">Disabled / Hidden</option>
                </select>
              </div>

              {/* Maintenance Notes */}
              <div className="sm:col-span-2">
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                  Maintenance / Administrative Notes
                </label>
                <textarea
                  name="maintenanceNotes"
                  rows={2}
                  placeholder="Enter maintenance updates, reason for disabling, or administrative notes..."
                  value={formData.maintenanceNotes}
                  onChange={handleChange}
                  className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
                />
              </div>
            </div>
          </div>
        )}

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
            disabled={loading || uploadingImage}
            className="px-6 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-extrabold rounded-xl shadow-md hover:shadow-lg transition-all flex items-center gap-2 disabled:opacity-70"
          >
            {loading || uploadingImage ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>{uploadingImage ? 'Uploading Image...' : 'Saving Equipment...'}</span>
              </>
            ) : (
              <>
                <Save className="w-4 h-4" />
                <span>{editId ? 'Update Equipment' : 'Publish Listing'}</span>
              </>
            )}
          </button>
        </div>

      </form>

    </div>
  );
}

export default AddEquipment;
