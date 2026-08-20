import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  MapPin,
  Tractor,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  DollarSign,
  User,
  ShieldCheck,
  RefreshCw,
  Sprout,
} from 'lucide-react';
import { getOperatorId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { equipmentService } from '../../services/equipmentService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
  formatFuelTypeLabel,
} from '../../utils/constants';

function JobDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const operatorId = getOperatorId();

  const [booking, setBooking] = useState(null);
  const [equipment, setEquipment] = useState(null);
  const [activeImage, setActiveImage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadJobDetails() {
      if (!id) return;
      setLoading(true);
      setError(null);

      try {
        const bookingData = await bookingService.getBookingById(id);
        setBooking(bookingData);

        if (bookingData && bookingData.equipmentId) {
          const equipData = await equipmentService.getEquipmentById(bookingData.equipmentId);
          setEquipment(equipData);
          if (equipData && equipData.images && equipData.images.length > 0) {
            const primary = equipData.images.find((img) => img.isPrimary) || equipData.images[0];
            setActiveImage(primary.imageUrl);
          } else {
            setActiveImage(equipData?.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE);
          }
        }
      } catch (err) {
        console.error('Failed to load job details:', err);
        setError(err.message || 'Job assignment not found.');
      } finally {
        setLoading(false);
      }
    }

    loadJobDetails();
  }, [id]);

  const handleStatusUpdate = async (newStatus) => {
    if (!booking) return;
    setUpdating(true);
    try {
      const updated = await bookingService.updateBookingStatus(booking.id, {
        status: newStatus,
        operatorId,
      });
      setBooking(updated);
    } catch (err) {
      alert(err.message || 'Failed to update job status');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6 space-y-6 animate-pulse font-sans">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="h-96 bg-gray-200 rounded-3xl" />
          <div className="space-y-4">
            <div className="h-6 bg-gray-200 rounded w-3/4" />
            <div className="h-4 bg-gray-200 rounded w-1/2" />
            <div className="h-24 bg-gray-200 rounded-2xl" />
          </div>
        </div>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white border border-gray-100 rounded-3xl shadow-sm text-center space-y-4 font-sans">
        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
        <h2 className="text-xl font-extrabold text-gray-900">Job Assignment Not Found</h2>
        <p className="text-gray-500 text-xs">{error || 'The requested job assignment details could not be retrieved.'}</p>
        <Link
          to="/operator/jobs"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#3E7B27] text-white text-xs font-bold rounded-xl hover:bg-[#2E6F22] transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Assigned Jobs
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6 font-sans">

      {/* Navigation Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <Link
              to="/operator/jobs"
              className="p-1.5 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Job Assignment #{booking.id}
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500">
            Inspect machine technical specifications, field operational notes, and update task progress.
          </p>
        </div>

        <span
          className={`text-xs font-black uppercase px-3 py-1.5 rounded-full border shadow-xs ${booking.status === 'CONFIRMED'
            ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
            : booking.status === 'COMPLETED'
              ? 'bg-blue-100 text-blue-800 border-blue-300'
              : booking.status === 'CANCELLED'
                ? 'bg-red-100 text-red-800 border-red-300'
                : 'bg-amber-100 text-amber-800 border-amber-300'
            }`}
        >
          {booking.status}
        </span>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

        {/* Left Column: Machinery Photo & Specification Gallery */}
        <div className="space-y-4">
          <div className="relative h-96 w-full bg-gray-100 rounded-3xl overflow-hidden border border-gray-100 shadow-sm">
            <img
              src={activeImage || DEFAULT_EQUIPMENT_IMAGE}
              alt={booking.equipmentName || 'Equipment'}
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = DEFAULT_EQUIPMENT_IMAGE;
              }}
            />
            <span className="absolute top-4 left-4 bg-[#142E1C] text-white text-[10px] font-black px-3 py-1 rounded-xl uppercase tracking-wider shadow-xs">
              {formatCategoryLabel(booking.equipmentCategory)}
            </span>
          </div>

          {/* Thumbnail Gallery */}
          {equipment?.images && equipment.images.length > 1 && (
            <div className="flex gap-3 overflow-x-auto pb-2">
              {equipment.images.map((img) => (
                <button
                  key={img.id}
                  onClick={() => setActiveImage(img.imageUrl)}
                  className={`relative w-20 h-20 rounded-2xl overflow-hidden border-2 shrink-0 transition-all ${activeImage === img.imageUrl ? 'border-[#3E7B27] ring-2 ring-emerald-100' : 'border-gray-200 opacity-70 hover:opacity-100'
                    }`}
                >
                  <img src={img.imageUrl} alt="Thumbnail" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}

          {/* Machine Specifications Box */}
          {equipment && (
            <div className="bg-white rounded-3xl border border-gray-100 p-6 space-y-4 shadow-sm">
              <h3 className="text-sm font-black text-[#142E1C] uppercase tracking-wider border-b border-gray-100 pb-2">
                Machine Specifications
              </h3>

              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="block text-gray-400 font-bold uppercase text-[10px]">Brand / Model</span>
                  <span className="font-extrabold text-gray-900">{equipment.brand} {equipment.model}</span>
                </div>
                <div>
                  <span className="block text-gray-400 font-bold uppercase text-[10px]">Manufacturing Year</span>
                  <span className="font-extrabold text-gray-900">{equipment.manufacturingYear}</span>
                </div>
                <div>
                  <span className="block text-gray-400 font-bold uppercase text-[10px]">Power / Capacity</span>
                  <span className="font-extrabold text-gray-900">{equipment.capacity || 'Standard'}</span>
                </div>
                <div>
                  <span className="block text-gray-400 font-bold uppercase text-[10px]">Fuel / Power Type</span>
                  <span className="font-extrabold text-gray-900">{formatFuelTypeLabel(equipment.fuelType)}</span>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Operational Assignment & Action Controls */}
        <div className="bg-white rounded-3xl border border-gray-100 p-6 sm:p-8 space-y-6 shadow-sm">

          <div>
            <h2 className="text-2xl font-black text-gray-900 tracking-tight">
              {booking.equipmentName || `Machine #${booking.equipmentId}`}
            </h2>
            {booking.farmName && (
              <div className="mt-2 inline-flex items-center gap-1.5 px-3 py-1 bg-emerald-50 border border-emerald-200 rounded-xl text-xs font-bold text-emerald-800">
                <Sprout className="w-4 h-4 text-emerald-600 shrink-0" />
                <span>Target Farm: {booking.farmName} {booking.farmLocation ? `(${booking.farmLocation})` : ''}</span>
              </div>
            )}
            <p className="text-xs font-bold text-gray-500 mt-2 flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5 text-gray-400" />
              <span>{booking.deliveryAddress || 'Field Location Specified'}</span>
            </p>
          </div>

          {/* Schedule & Financial Box */}
          <div className="bg-[#F8FAF8] border border-gray-200/80 rounded-2xl p-4 grid grid-cols-2 gap-4 text-xs">
            <div>
              <span className="text-gray-400 font-bold uppercase text-[10px] block">Schedule Duration</span>
              <span className="font-black text-gray-900 block mt-0.5">
                {booking.startDate} → {booking.endDate}
              </span>
            </div>
            <div>
              <span className="text-gray-400 font-bold uppercase text-[10px] block">Total Rental Tariff</span>
              <span className="font-black text-[#142E1C] text-base block mt-0.5">
                ₹{Number(booking.totalCost || 0).toLocaleString('en-IN')}
              </span>
            </div>
          </div>

          {/* Operational Notes */}
          {booking.notes && (
            <div className="p-4 bg-amber-50/70 border border-amber-200/80 rounded-2xl space-y-1">
              <span className="text-amber-900 font-bold text-xs uppercase tracking-wider block">
                Partner / Farmer Notes:
              </span>
              <p className="text-xs text-amber-800 leading-relaxed">{booking.notes}</p>
            </div>
          )}

          {/* Action Task Control Footer */}
          <div className="pt-4 border-t border-gray-100 space-y-3">
            <h3 className="text-xs font-black text-gray-700 uppercase tracking-wider">
              Operator Task Controls
            </h3>

            {booking.status === 'CONFIRMED' && (
              <button
                type="button"
                disabled={updating}
                onClick={() => handleStatusUpdate('COMPLETED')}
                className="w-full py-3 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-extrabold rounded-xl shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-70"
              >
                {updating ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Updating Status...</span>
                  </>
                ) : (
                  <>
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Mark Job Completed & Release Equipment</span>
                  </>
                )}
              </button>
            )}

            {booking.status === 'COMPLETED' && (
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-2xl text-center space-y-1">
                <CheckCircle2 className="w-6 h-6 text-blue-600 mx-auto" />
                <span className="font-bold text-xs text-blue-900 block">Job Successfully Completed</span>
                <span className="text-[11px] text-blue-700 block">Equipment availability has synchronized back to Available.</span>
              </div>
            )}
          </div>

        </div>

      </div>

    </div>
  );
}

export default JobDetails;

