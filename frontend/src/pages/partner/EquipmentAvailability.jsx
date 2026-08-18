import { useState, useEffect, useCallback } from 'react';
import { equipmentService } from '../../services/equipmentService';
import { AVAILABILITY_STATUSES, getStatusBadgeInfo } from '../../utils/constants';

function EquipmentAvailability() {
  const partnerId = localStorage.getItem('partnerId') || '1';

  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const fetchEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await equipmentService.getPartnerEquipment(partnerId);
      setEquipmentList(data || []);
    } catch (err) {
      console.error('Failed to load availability fleet:', err);
      setError(err.message || 'Failed to load fleet availability');
    } finally {
      setLoading(false);
    }
  }, [partnerId]);

  useEffect(() => {
    fetchEquipment();
  }, [fetchEquipment]);

  const handleStatusChange = async (item, newStatus) => {
    if (newStatus === 'BOOKED') {
      alert('Manual transition to BOOKED state is blocked by backend policy. BOOKED status can only be initiated through the Booking flow.');
      return;
    }

    setActionLoading(item.id);
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
      await fetchEquipment();
    } catch (err) {
      alert(err.message || 'Failed to update equipment availability status');
    } finally {
      setActionLoading(null);
    }
  };

  const handleToggleEnable = async (item) => {
    setActionLoading(item.id);
    try {
      if (item.isDisabled) {
        await equipmentService.enableEquipment(item.id, partnerId);
      } else {
        await equipmentService.disableEquipment(item.id, partnerId);
      }
      await fetchEquipment();
    } catch (err) {
      alert(err.message || 'Failed to toggle equipment enable status');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">
      <div className="border-b border-gray-200 pb-4">
        <h1 className="text-3xl font-bold text-green-800 tracking-tight">Fleet Availability Control</h1>
        <p className="text-gray-600 mt-1">Configure operational availability states and maintenance schedules for your machinery.</p>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button onClick={fetchEquipment} className="underline text-red-800 font-semibold">
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="space-y-4 animate-pulse">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="h-16 bg-gray-200 rounded-xl" />
          ))}
        </div>
      ) : equipmentList.length === 0 ? (
        <div className="bg-white border border-gray-200 rounded-2xl p-12 text-center text-gray-500">
          No machinery listings found in your inventory.
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-gray-50 text-xs font-bold text-gray-500 uppercase tracking-wider border-b border-gray-200">
                <tr>
                  <th className="px-6 py-4">Equipment Name</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Current Status</th>
                  <th className="px-6 py-4">Availability Control</th>
                  <th className="px-6 py-4">Operational Toggle</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {equipmentList.map((item) => {
                  const badge = getStatusBadgeInfo(item.availabilityStatus);
                  const isProcessing = actionLoading === item.id;

                  return (
                    <tr key={item.id} className="hover:bg-gray-50/80 transition">
                      <td className="px-6 py-4 font-bold text-gray-900">
                        {item.name}
                        <div className="text-xs font-normal text-gray-500">{item.brand} {item.model}</div>
                      </td>

                      <td className="px-6 py-4 text-xs font-semibold text-gray-600">
                        {item.category}
                      </td>

                      <td className="px-6 py-4">
                        <span className={`text-xs font-bold px-2.5 py-1 rounded-full border ${badge.badgeClass}`}>
                          {badge.label}
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <select
                          disabled={isProcessing || item.availabilityStatus === 'BOOKED'}
                          value={item.availabilityStatus}
                          onChange={(e) => handleStatusChange(item, e.target.value)}
                          className="border border-gray-300 rounded-lg px-2.5 py-1.5 text-xs font-semibold text-gray-800 bg-white focus:ring-2 focus:ring-green-500 disabled:opacity-50"
                        >
                          {AVAILABILITY_STATUSES.map((st) => (
                            <option key={st.value} value={st.value} disabled={st.value === 'BOOKED'}>
                              {st.label} {st.value === 'BOOKED' ? '(Automated Flow)' : ''}
                            </option>
                          ))}
                        </select>
                      </td>

                      <td className="px-6 py-4">
                        <button
                          disabled={isProcessing}
                          onClick={() => handleToggleEnable(item)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition ${
                            item.isDisabled
                              ? 'bg-red-50 text-red-700 border-red-300 hover:bg-red-100'
                              : 'bg-green-50 text-green-700 border-green-300 hover:bg-green-100'
                          }`}
                        >
                          {isProcessing ? 'Saving...' : item.isDisabled ? 'Disabled' : 'Enabled'}
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
