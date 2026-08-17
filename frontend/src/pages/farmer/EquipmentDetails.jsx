import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { equipmentService } from '../../services/equipmentService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
  formatFuelTypeLabel,
  getStatusBadgeInfo,
} from '../../utils/constants';

function EquipmentDetails() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [equipment, setEquipment] = useState(null);
  const [activeImage, setActiveImage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadEquipmentDetails() {
      setLoading(true);
      setError(null);

      try {
        const data = await equipmentService.getEquipmentById(id);
        setEquipment(data);
        if (data && data.images && data.images.length > 0) {
          const primary = data.images.find((img) => img.isPrimary) || data.images[0];
          setActiveImage(primary.imageUrl);
        } else {
          setActiveImage(DEFAULT_EQUIPMENT_IMAGE);
        }
      } catch (err) {
        console.error('Failed to load equipment details:', err);
        setError(err.message || 'Equipment listing not found');
      } finally {
        setLoading(false);
      }
    }

    if (id) {
      loadEquipmentDetails();
    }
  }, [id]);

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6 space-y-6 animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="h-96 bg-gray-200 rounded-2xl" />
          <div className="space-y-4">
            <div className="h-6 bg-gray-200 rounded w-3/4" />
            <div className="h-4 bg-gray-200 rounded w-1/2" />
            <div className="h-10 bg-gray-200 rounded w-1/3" />
            <div className="h-24 bg-gray-200 rounded" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !equipment) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white border border-gray-200 rounded-2xl shadow-sm text-center space-y-4">
        <div className="text-4xl">⚠️</div>
        <h2 className="text-2xl font-bold text-gray-800">Equipment Not Found</h2>
        <p className="text-gray-600 text-sm">{error || 'The requested equipment listing could not be retrieved.'}</p>
        <button
          onClick={() => navigate('/farmer/equipment')}
          className="px-5 py-2.5 bg-green-700 text-white font-semibold rounded-xl hover:bg-green-800 transition text-sm"
        >
          Back to Equipment List
        </button>
      </div>
    );
  }

  const badge = getStatusBadgeInfo(equipment.availabilityStatus);
  const isAvailable = equipment.availabilityStatus === 'AVAILABLE' && !equipment.isDisabled;

  return (
    <div className="max-w-6xl mx-auto p-4 sm:p-6 space-y-8">
      {/* Navigation Breadcrumb */}
      <button
        onClick={() => navigate('/farmer/equipment')}
        className="text-sm font-semibold text-green-700 hover:text-green-800 flex items-center gap-1 transition"
      >
        ← Back to Machinery Search
      </button>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Left Column: Image Gallery */}
        <div className="space-y-4">
          <div className="relative h-96 w-full bg-gray-100 rounded-2xl overflow-hidden border border-gray-200 shadow-sm">
            <img
              src={activeImage || DEFAULT_EQUIPMENT_IMAGE}
              alt={equipment.name}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = DEFAULT_EQUIPMENT_IMAGE;
              }}
            />
            <span
              className={`absolute top-4 right-4 text-xs font-bold px-3 py-1 rounded-full border shadow-sm ${badge.badgeClass}`}
            >
              {badge.label}
            </span>
          </div>

          {/* Thumbnail Strip */}
          {equipment.images && equipment.images.length > 1 && (
            <div className="flex gap-3 overflow-x-auto pb-2">
              {equipment.images.map((img) => (
                <button
                  key={img.id}
                  onClick={() => setActiveImage(img.imageUrl)}
                  className={`relative w-20 h-20 rounded-xl overflow-hidden border-2 flex-shrink-0 transition ${
                    activeImage === img.imageUrl ? 'border-green-600 ring-2 ring-green-200' : 'border-gray-200 opacity-70 hover:opacity-100'
                  }`}
                >
                  <img src={img.imageUrl} alt="Thumbnail" className="w-full h-full object-cover" />
                  {img.isPrimary && (
                    <span className="absolute bottom-0 inset-x-0 bg-green-700 text-white text-[9px] font-bold text-center py-0.5">
                      PRIMARY
                    </span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Specification & Action */}
        <div className="bg-white rounded-2xl border border-gray-200 p-6 sm:p-8 space-y-6 shadow-sm">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="bg-green-100 text-green-800 text-xs font-semibold px-2.5 py-0.5 rounded-md">
                {formatCategoryLabel(equipment.category)}
              </span>
              {equipment.isDisabled && (
                <span className="bg-red-100 text-red-800 text-xs font-semibold px-2.5 py-0.5 rounded-md">
                  Administratively Disabled
                </span>
              )}
            </div>
            <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">{equipment.name}</h1>
            <p className="text-sm text-gray-500 mt-1 flex items-center gap-1">
              📍 {equipment.locationAddress} ({equipment.latitude.toFixed(4)}, {equipment.longitude.toFixed(4)})
            </p>
          </div>

          {/* Price Box */}
          <div className="bg-green-50/70 border border-green-200 rounded-xl p-4 flex items-center justify-between">
            <div>
              <span className="text-3xl font-black text-green-800">
                ₹{Number(equipment.rentalPrice).toLocaleString('en-IN')}
              </span>
              <span className="text-sm font-medium text-gray-600"> / day</span>
            </div>
            <div className="text-right text-xs text-green-700 font-semibold">
              Owner: {equipment.partnerName || `Partner #${equipment.partnerId}`}
            </div>
          </div>

          {/* Key Specifications Grid */}
          <div className="grid grid-cols-2 gap-4 border-y border-gray-100 py-4">
            <div>
              <span className="block text-xs text-gray-500 font-medium uppercase">Brand & Model</span>
              <span className="text-sm font-semibold text-gray-800">{equipment.brand} {equipment.model}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500 font-medium uppercase">Manufacturing Year</span>
              <span className="text-sm font-semibold text-gray-800">{equipment.manufacturingYear}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500 font-medium uppercase">Capacity / Power</span>
              <span className="text-sm font-semibold text-gray-800">{equipment.capacity}</span>
            </div>
            <div>
              <span className="block text-xs text-gray-500 font-medium uppercase">Fuel Type</span>
              <span className="text-sm font-semibold text-gray-800">{formatFuelTypeLabel(equipment.fuelType)}</span>
            </div>
          </div>

          {/* Description */}
          <div>
            <h3 className="text-sm font-bold text-gray-800 uppercase tracking-wide mb-2">Description</h3>
            <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-line">{equipment.description}</p>
          </div>

          {/* Action Button */}
          <div className="pt-2">
            {isAvailable ? (
              <button
                onClick={() => navigate(`/farmer/book/${equipment.id}`)}
                className="w-full py-3.5 bg-green-700 text-white font-bold rounded-xl hover:bg-green-800 transition shadow-md text-base"
              >
                Proceed to Book Machinery
              </button>
            ) : (
              <button
                disabled
                className="w-full py-3.5 bg-gray-200 text-gray-500 font-bold rounded-xl cursor-not-allowed text-base"
              >
                Currently Unavailable for Booking
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default EquipmentDetails;
