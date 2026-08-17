import { useState, useEffect, useCallback } from 'react';
import { equipmentService } from '../../services/equipmentService';
import { formatCategoryLabel, getStatusBadgeInfo } from '../../utils/constants';

function ManageEquipment() {
  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [actionLoading, setActionLoading] = useState(null);

  const fetchAllEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const pageData = await equipmentService.searchEquipmentPage(
        { locationAddress: searchQuery },
        0,
        50
      );
      setEquipmentList(pageData?.content || []);
    } catch (err) {
      console.error('Failed to load admin equipment list:', err);
      setError(err.message || 'Failed to load equipment catalog');
    } finally {
      setLoading(false);
    }
  }, [searchQuery]);

  useEffect(() => {
    fetchAllEquipment();
  }, [fetchAllEquipment]);

  const handleToggleEnable = async (item) => {
    setActionLoading(item.id);
    try {
      if (item.isDisabled) {
        await equipmentService.enableEquipment(item.id, item.partnerId || 1);
      } else {
        await equipmentService.disableEquipment(item.id, item.partnerId || 1);
      }
      await fetchAllEquipment();
    } catch (err) {
      alert(err.message || 'Failed to toggle administrative equipment state');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-3xl font-bold text-green-800 tracking-tight">System Equipment Administration</h1>
          <p className="text-gray-600 mt-1">Global catalog inspection and administrative lockout management.</p>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button onClick={fetchAllEquipment} className="underline text-red-800 font-semibold">
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((idx) => (
            <div key={idx} className="h-16 bg-gray-200 rounded-xl" />
          ))}
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-gray-50 text-xs font-bold text-gray-500 uppercase tracking-wider border-b border-gray-200">
                <tr>
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Equipment Name</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Daily Rate</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Admin Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {equipmentList.map((item) => {
                  const badge = getStatusBadgeInfo(item.availabilityStatus);
                  const isProcessing = actionLoading === item.id;

                  return (
                    <tr key={item.id} className="hover:bg-gray-50/80 transition">
                      <td className="px-6 py-4 text-xs font-mono text-gray-400">#{item.id}</td>
                      <td className="px-6 py-4 font-bold text-gray-900">
                        {item.name}
                        <div className="text-xs font-normal text-gray-500">📍 {item.locationAddress}</div>
                      </td>
                      <td className="px-6 py-4 text-xs font-semibold text-gray-600">
                        {formatCategoryLabel(item.category)}
                      </td>
                      <td className="px-6 py-4 font-extrabold text-green-800">
                        ₹{Number(item.rentalPrice).toLocaleString('en-IN')}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`text-xs font-bold px-2.5 py-1 rounded-full border ${badge.badgeClass}`}>
                          {badge.label}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <button
                          disabled={isProcessing}
                          onClick={() => handleToggleEnable(item)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition ${
                            item.isDisabled
                              ? 'bg-green-50 text-green-700 border-green-300 hover:bg-green-100'
                              : 'bg-red-50 text-red-700 border-red-300 hover:bg-red-100'
                          }`}
                        >
                          {isProcessing ? 'Updating...' : item.isDisabled ? 'Re-enable Listing' : 'Admin Disable'}
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

export default ManageEquipment;
