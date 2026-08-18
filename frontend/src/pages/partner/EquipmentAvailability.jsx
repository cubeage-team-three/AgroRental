import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  ToggleLeft,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  Tractor,
  SlidersHorizontal,
  PlusCircle,
  MapPin,
  Calendar,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { equipmentService } from '../../services/equipmentService';
import {
  AVAILABILITY_STATUSES,
  getStatusBadgeInfo,
  formatCategoryLabel,
} from '../../utils/constants';

function EquipmentAvailability() {
  const partnerId = getPartnerId();

  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [successToast, setSuccessToast] = useState('');

  const fetchEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await equipmentService.getPartnerEquipment(partnerId);
      setEquipmentList(data || []);
    } catch (err) {
      console.error('Failed to load fleet availability:', err);
      setError(err.message || 'Failed to load fleet availability data.');
    } finally {
      setLoading(false);
    }
  }, [partnerId]);

  useEffect(() => {
    fetchEquipment();
  }, [fetchEquipment]);

  const handleStatusChange = async (item, newStatus) => {
    if (newStatus === 'BOOKED') {
      alert('Manual transition to BOOKED state is prohibited. BOOKED status is managed automatically through the Rental Booking lifecycle.');
      return;
    }

    setActionLoading(item.id);
    setSuccessToast('');

    try {
      const payload = {
        name: item.name,
        category: item.category,
        brand: item.brand,
        model: item.model,
        manufacturingYear: item.manufacturingYear,
        capacity: item.capacity,
        rentalPrice: item.rentalPrice,
        fuelType: item.fuelType,
        description: item.description,
        locationAddress: item.locationAddress,
        latitude: item.latitude,
        longitude: item.longitude,
        availabilityStatus: newStatus,
        maintenanceNotes: item.maintenanceNotes,
        images: item.images || [],
      };

      await equipmentService.updateEquipment(item.id, payload, partnerId);
      setSuccessToast(`✓ Availability status updated for "${item.name}"`);
      await fetchEquipment();
    } catch (err) {
      alert(err.message || 'Failed to update equipment availability status');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleEnable = async (item) => {
    setActionLoading(item.id);
    setSuccessToast('');

    try {
      if (item.isDisabled) {
        await equipmentService.enableEquipment(item.id, partnerId);
        setSuccessToast(`✓ "${item.name}" re-enabled for rental discovery.`);
      } else {
        await equipmentService.disableEquipment(item.id, partnerId);
        setSuccessToast(`✓ "${item.name}" disabled from rental discovery.`);
      }
      await fetchEquipment();
    } catch (err) {
      alert(err.message || 'Failed to toggle equipment status');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Fleet Operational & Availability Control
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Configure machine availability states, maintenance schedules, and operational search visibility.
          </p>
        </div>

        <Link
          to="/partner/equipment/add"
          className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-extrabold rounded-xl shadow-md transition-all"
        >
          <PlusCircle className="w-4 h-4" />
          <span>+ Add Machine</span>
        </Link>
      </div>

      {/* Feedback Alerts */}
      {successToast && (
        <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-800 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button type="button" onClick={() => setSuccessToast('')} className="text-emerald-700 font-bold ml-2">✕</button>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchEquipment} className="text-xs font-bold text-red-800 underline">
            Retry
          </button>
        </div>
      )}

      {/* Table Card */}
      {loading ? (
        <div className="space-y-4 animate-pulse">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="h-16 bg-gray-200 rounded-2xl" />
          ))}
        </div>
      ) : equipmentList.length === 0 ? (
        <div className="bg-white border border-gray-100 rounded-3xl p-12 text-center text-gray-500 space-y-3">
          <Tractor className="w-10 h-10 text-gray-300 mx-auto" />
          <p className="text-sm font-bold text-gray-700">No machinery listings in your fleet.</p>
          <Link
            to="/partner/equipment/add"
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#3E7B27] text-white text-xs font-bold rounded-xl"
          >
            Add Equipment
          </Link>
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-gray-100 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-[#F8FAF8] text-[11px] font-black text-gray-500 uppercase tracking-wider border-b border-gray-200/80">
                <tr>
                  <th className="px-6 py-4">Machinery Details</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Current Status</th>
                  <th className="px-6 py-4">Availability State Control</th>
                  <th className="px-6 py-4">Search Visibility Toggle</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {equipmentList.map((item) => {
                  const badge = getStatusBadgeInfo(item.availabilityStatus);
                  const isProcessing = actionLoading === item.id;

                  return (
                    <tr key={item.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="px-6 py-4">
                        <div className="font-extrabold text-gray-900">{item.name}</div>
                        <div className="text-xs text-gray-500 font-medium">
                          {item.brand} {item.model} • ₹{Number(item.rentalPrice).toLocaleString('en-IN')}/day
                        </div>
                      </td>

                      <td className="px-6 py-4 text-xs font-bold text-[#3E7B27]">
                        {formatCategoryLabel(item.category)}
                      </td>

                      <td className="px-6 py-4">
                        <span className={`text-[10px] font-black uppercase px-2.5 py-1 rounded-full border shadow-2xs ${badge.badgeClass}`}>
                          {badge.label}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <select
                          disabled={isProcessing || item.availabilityStatus === 'BOOKED'}
                          value={item.availabilityStatus}
                          onChange={(e) => handleStatusChange(item, e.target.value)}
                          className="border border-gray-200 rounded-xl px-3 py-1.5 text-xs font-bold text-gray-800 bg-[#F0EFE9] focus:ring-2 focus:ring-[#3E7B27] focus:bg-white disabled:opacity-50 transition-all cursor-pointer"
                        >
                          {AVAILABILITY_STATUSES.map((st) => (
                            <option key={st.value} value={st.value} disabled={st.value === 'BOOKED'}>
                              {st.label} {st.value === 'BOOKED' ? '(Auto)' : ''}
                            </option>
                          ))}
                        </select>
                      </td>

                      <td className="px-6 py-4">
                        <button
                          disabled={isProcessing}
                          onClick={() => handleToggleEnable(item)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-xl border transition-all ${
                            item.isDisabled
                              ? 'bg-red-50 text-red-700 border-red-200 hover:bg-red-100'
                              : 'bg-emerald-50 text-emerald-800 border-emerald-200 hover:bg-emerald-100'
                          }`}
                        >
                          {isProcessing ? 'Updating...' : item.isDisabled ? 'Listing Disabled' : '✓ Active & Enabled'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}

export default EquipmentAvailability;
