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
      const pageData = await equipmentService.getAllEquipment(0, 50);
      let content = pageData?.content || [];
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        content = content.filter(
          (item) =>
            item.name?.toLowerCase().includes(q) ||
            item.locationAddress?.toLowerCase().includes(q) ||
            item.brand?.toLowerCase().includes(q) ||
            item.category?.toLowerCase().includes(q)
        );
      }
      setEquipmentList(content);
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

  const handleDeleteEquipment = async (item) => {
    if (!window.confirm(`Are you sure you want to permanently remove "${item.name}" from the system catalog?`)) {
      return;
    }
    setActionLoading(item.id);
    try {
      await equipmentService.deleteEquipment(item.id, item.partnerId || 1);
      await fetchAllEquipment();
    } catch (err) {
      alert(err.message || 'Failed to delete equipment listing');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 space-y-6 font-sans">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">System Equipment Administration</h1>
          <p className="text-xs sm:text-sm text-slate-500 font-medium mt-1">Global catalog inspection, administrative lockouts, and fleet maintenance management.</p>
        </div>
      </div>

      {/* Admin Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-6 border border-slate-200 shadow-xs flex flex-col sm:flex-row gap-3">
        <input
          type="text"
          placeholder="Filter equipment by location or keyword..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="flex-1 bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-xs font-bold text-slate-900 placeholder-slate-400 focus:bg-white focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 focus:outline-none"
        />
        <button
          onClick={fetchAllEquipment}
          className="px-6 py-2.5 bg-emerald-700 text-white font-bold rounded-xl hover:bg-emerald-800 transition text-xs shadow-xs cursor-pointer"
        >
          Search Catalog
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-2xl p-4 text-red-700 text-xs font-bold flex items-center justify-between shadow-xs">
          <span>{error}</span>
          <button onClick={fetchAllEquipment} className="underline text-red-800 font-bold cursor-pointer">
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((idx) => (
            <div key={idx} className="h-16 bg-slate-100 rounded-2xl border border-slate-200" />
          ))}
        </div>
      ) : equipmentList.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-3xl p-12 text-center text-slate-400 font-medium">
          No machinery listings found in the system catalog.
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-[10px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-100">
                <tr>
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Equipment Name</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Daily Rate</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4">Admin Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {equipmentList.map((item) => {
                  const badge = getStatusBadgeInfo(item.availabilityStatus);
                  const isProcessing = actionLoading === item.id;

                  return (
                    <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="px-6 py-4 text-xs font-mono text-slate-400">#{item.id}</td>
                      <td className="px-6 py-4 font-bold text-slate-900">
                        {item.name}
                        <div className="text-xs font-normal text-slate-500 mt-0.5">📍 {item.locationAddress}</div>
                      </td>
                      <td className="px-6 py-4 text-xs font-semibold text-slate-600">
                        {formatCategoryLabel(item.category)}
                      </td>
                      <td className="px-6 py-4 font-extrabold text-emerald-700 text-sm">
                        ₹{Number(item.rentalPrice).toLocaleString('en-IN')}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`text-[10px] font-bold px-2.5 py-1 rounded-full border ${badge.badgeClass}`}>
                          {badge.label}
                        </span>
                      </td>
                      <td className="px-6 py-4 flex items-center gap-2">
                        <button
                          disabled={isProcessing}
                          onClick={() => handleToggleEnable(item)}
                          className={`text-xs font-bold px-3 py-1.5 rounded-xl border transition cursor-pointer ${
                            item.isDisabled
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100'
                              : 'bg-amber-50 text-amber-700 border-amber-300 hover:bg-amber-100'
                          }`}
                        >
                          {isProcessing ? 'Updating...' : item.isDisabled ? 'Re-enable' : 'Disable'}
                        </button>
                        <button
                          disabled={isProcessing || item.availabilityStatus === 'BOOKED'}
                          onClick={() => handleDeleteEquipment(item)}
                          className="text-xs font-bold px-3 py-1.5 rounded-xl border bg-red-50 text-red-700 border-red-300 hover:bg-red-100 disabled:opacity-50 transition cursor-pointer"
                        >
                          Remove
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
