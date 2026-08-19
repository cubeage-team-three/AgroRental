import { useState, useEffect } from 'react';
import { useSearchParams, useParams, useNavigate, Link } from 'react-router-dom';
import { Calendar, MapPin, Truck, AlertCircle, CheckCircle, ArrowLeft, Sprout } from 'lucide-react';
import { equipmentService } from '../../services/equipmentService';
import { bookingService } from '../../services/bookingService';
import { farmService } from '../../services/farmService';
import { getFarmerId } from '../../services/authService';

function BookEquipment() {
  const [searchParams] = useSearchParams();
  const { equipmentId: pathEquipmentId } = useParams();
  const navigate = useNavigate();
  const equipmentId = searchParams.get('equipmentId') || pathEquipmentId;

  const [equipment, setEquipment] = useState(null);
  const [farms, setFarms] = useState([]);
  const [selectedFarmId, setSelectedFarmId] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Form state
  const [farmerId, setFarmerId] = useState(getFarmerId());
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [notes, setNotes] = useState('');

  // Default dates: tomorrow to day after tomorrow
  useEffect(() => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dayAfter = new Date();
    dayAfter.setDate(dayAfter.getDate() + 3);

    setStartDate(tomorrow.toISOString().split('T')[0]);
    setEndDate(dayAfter.toISOString().split('T')[0]);
  }, []);

  useEffect(() => {
    const loadData = async () => {
      try {
        setLoading(true);
        if (equipmentId) {
          const data = await equipmentService.getEquipmentById(equipmentId);
          setEquipment(data);
          if (data.locationAddress) {
            setDeliveryAddress(data.locationAddress);
          }
        }
        
        const farmerFarms = await farmService.getFarmerFarms(farmerId);
        setFarms(farmerFarms || []);
        if (farmerFarms && farmerFarms.length > 0) {
          const defaultFarm = farmerFarms[0];
          setSelectedFarmId(defaultFarm.id);
          const farmAddr = `${defaultFarm.farmName}, ${defaultFarm.village}, ${defaultFarm.taluka}, ${defaultFarm.district}`;
          setDeliveryAddress(farmAddr);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load details.');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [equipmentId, farmerId]);

  const handleFarmChange = (farmIdVal) => {
    setSelectedFarmId(farmIdVal);
    const foundFarm = farms.find((f) => String(f.id) === String(farmIdVal));
    if (foundFarm) {
      const farmAddr = `${foundFarm.farmName}, ${foundFarm.village}, ${foundFarm.taluka}, ${foundFarm.district || foundFarm.state || ''}`;
      setDeliveryAddress(farmAddr);
    }
  };

  // Calculate estimated total cost
  const calculateTotal = () => {
    if (!equipment?.rentalPrice || !startDate || !endDate) return 0;
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return diffDays > 0 ? diffDays * equipment.rentalPrice : equipment.rentalPrice;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!equipmentId) {
      setError('No equipment selected for booking.');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);

      const payload = {
        equipmentId: Number(equipmentId),
        farmerId: Number(farmerId),
        farmId: selectedFarmId ? Number(selectedFarmId) : null,
        startDate,
        endDate,
        deliveryAddress,
        notes,
      };

      const booking = await bookingService.createBooking(payload);
      setSuccess(`Booking reserved successfully! Reservation ID: #${booking.id}`);

      setTimeout(() => {
        navigate('/farmer/my-bookings');
      }, 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to complete equipment booking.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium">Loading equipment booking portal...</p>
        </div>
      </div>
    );
  }

  if (!equipmentId || !equipment) {
    return (
      <div className="mx-auto max-w-3xl p-6">
        <div className="rounded-2xl border border-amber-200 bg-amber-50/70 p-6 text-center shadow-sm">
          <AlertCircle className="mx-auto h-12 w-12 text-amber-600 mb-3" />
          <h2 className="text-xl font-bold text-amber-900 mb-2">No Equipment Selected</h2>
          <p className="text-amber-800 mb-4">Please select an available machine from the marketplace before creating a reservation.</p>
          <Link
            to="/farmer/search-equipment"
            className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-semibold text-white shadow-md transition hover:bg-emerald-700"
          >
            <ArrowLeft className="h-4 w-4" /> Browse Available Equipment
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <Link to="/farmer/search-equipment" className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-700 hover:text-emerald-800 mb-2">
            <ArrowLeft className="h-4 w-4" /> Back to Search
          </Link>
          <h1 className="text-2xl font-bold text-slate-900 sm:text-3xl">Reserve Equipment</h1>
          <p className="text-sm text-slate-600">Complete your machinery rental reservation</p>
        </div>
      </div>

      {error && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="mb-6 flex items-center gap-3 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <CheckCircle className="h-5 w-5 shrink-0 text-emerald-600" />
          <span>{success}</span>
        </div>
      )}

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Machine Summary Card */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-1">
          <h2 className="text-lg font-bold text-slate-900 mb-4">Equipment Summary</h2>
          {equipment.images && equipment.images.length > 0 ? (
            <img
              src={equipment.images.find((img) => img.isPrimary)?.imageUrl || equipment.images[0].imageUrl}
              alt={equipment.name}
              className="h-44 w-full rounded-xl object-cover mb-4"
            />
          ) : (
            <div className="flex h-44 items-center justify-center rounded-xl bg-slate-100 text-slate-400 mb-4">
              No Image Available
            </div>
          )}

          <h3 className="text-lg font-bold text-slate-900">{equipment.name}</h3>
          <p className="text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-2">{equipment.category}</p>

          <div className="space-y-2 text-sm text-slate-600 border-t border-slate-100 pt-3">
            <div className="flex justify-between">
              <span>Brand / Model:</span>
              <span className="font-semibold text-slate-800">{equipment.brand} {equipment.model}</span>
            </div>
            <div className="flex justify-between">
              <span>Rental Rate:</span>
              <span className="font-bold text-emerald-700">₹{equipment.rentalPrice} / day</span>
            </div>
            <div className="flex justify-between">
              <span>Location:</span>
              <span className="font-medium text-slate-800 text-right">{equipment.locationAddress}</span>
            </div>
          </div>
        </div>

        {/* Booking Request Form */}
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm lg:col-span-2">
          <form onSubmit={handleSubmit} className="space-y-5">
            <h2 className="text-lg font-bold text-slate-900 mb-2">Reservation Details</h2>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                <Sprout className="h-4 w-4 text-emerald-600" /> Select Target Farm Parcel
              </label>
              {farms.length > 0 ? (
                <select
                  value={selectedFarmId}
                  onChange={(e) => handleFarmChange(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm transition focus:border-emerald-500 focus:outline-none"
                >
                  {farms.map((f) => (
                    <option key={f.id} value={f.id}>
                      🌱 {f.farmName} ({f.village}, {f.taluka}) - {f.farmArea || 'N/A'} Acres ({f.cropType || 'General'})
                    </option>
                  ))}
                </select>
              ) : (
                <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800 flex items-center justify-between">
                  <span>No registered farms found for your profile.</span>
                  <Link to="/farmer/farms" className="font-bold underline text-amber-900">Add Farm →</Link>
                </div>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <Calendar className="h-4 w-4 text-emerald-600" /> Start Date
                </label>
                <input
                  type="date"
                  required
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm transition focus:border-emerald-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                  <Calendar className="h-4 w-4 text-emerald-600" /> End Date
                </label>
                <input
                  type="date"
                  required
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm transition focus:border-emerald-500 focus:outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5 flex items-center gap-1.5">
                <Truck className="h-4 w-4 text-emerald-600" /> Delivery Address / Field Location
              </label>
              <input
                type="text"
                required
                placeholder="Enter field address or farm location"
                value={deliveryAddress}
                onChange={(e) => setDeliveryAddress(e.target.value)}
                className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm transition focus:border-emerald-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-1.5">Special Instructions / Notes</label>
              <textarea
                rows={3}
                placeholder="Add notes for operator or partner..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full rounded-xl border border-slate-200 px-4 py-2.5 text-sm transition focus:border-emerald-500 focus:outline-none"
              />
            </div>

            {/* Estimated Total Calculation */}
            <div className="rounded-xl bg-slate-50 p-4 border border-slate-200 flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-500 uppercase">Estimated Total Cost</p>
                <p className="text-2xl font-bold text-emerald-700">₹{calculateTotal()}</p>
              </div>
              <button
                type="submit"
                disabled={submitting}
                className="rounded-xl bg-emerald-600 px-6 py-3 text-sm font-bold text-white shadow-lg shadow-emerald-900/10 transition hover:bg-emerald-700 disabled:opacity-50"
              >
                {submitting ? 'Confirming...' : 'Confirm Reservation'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default BookEquipment;
