import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { equipmentService } from '../../services/equipmentService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
  getStatusBadgeInfo,
} from '../../utils/constants';

function MyEquipment() {
  const navigate = useNavigate();
  const partnerId = localStorage.getItem('partnerId') || '1';

  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  const fetchPartnerEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await equipmentService.getPartnerEquipment(partnerId);
      setEquipmentList(data || []);
    } catch (err) {
      console.error('Failed to load partner equipment:', err);
      setError(err.message || 'Failed to load your machinery listings');
    } finally {
      setLoading(false);
    }
  }, [partnerId]);

  useEffect(() => {
    fetchPartnerEquipment();
  }, [fetchPartnerEquipment]);

  const handleToggleEnable = async (id, currentIsDisabled) => {
    setActionLoading(id);

    try {
      if (currentIsDisabled) {
        await equipmentService.enableEquipment(id, partnerId);
      } else {
        await equipmentService.disableEquipment(id, partnerId);
      }

      await fetchPartnerEquipment();
    } catch (err) {
      alert(err.message || 'Failed to update equipment status');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete "${name}"? This action cannot be undone.`)) {
      return;
    }

    setActionLoading(id);

    try {
      await equipmentService.deleteEquipment(id, partnerId);
      await fetchPartnerEquipment();
    } catch (err) {
      alert(err.message || 'Failed to delete equipment');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-3xl font-bold text-green-800 tracking-tight">My Equipment Inventory</h1>
          <p className="text-gray-600 mt-1">Manage your machinery listings, rental pricing, and availability status.</p>
        </div>

        <button
          onClick={() => navigate('/partner/equipment/add')}
          className="px-5 py-2.5 bg-green-700 text-white font-bold rounded-xl hover:bg-green-800 transition shadow-sm text-sm flex items-center justify-center gap-2"
        >
          <span>➕</span> Add New Equipment
        </button>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button onClick={fetchPartnerEquipment} className="underline text-red-800 font-semibold">
            Retry
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="bg-white rounded-xl border border-gray-200 p-4 space-y-3 animate-pulse">
              <div className="w-full h-44 bg-gray-200 rounded-lg" />
              <div className="h-5 bg-gray-200 rounded w-3/4" />
              <div className="h-4 bg-gray-200 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : equipmentList.length === 0 ? (
        /* Empty State */
        <div className="bg-white border border-dashed border-gray-300 rounded-2xl p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-green-50 text-green-700 rounded-full flex items-center justify-center mx-auto text-2xl">
            🚜
          </div>
          <h3 className="text-xl font-bold text-gray-800">No Equipment Listed Yet</h3>
          <p className="text-gray-500 max-w-md mx-auto text-sm">
            You haven't listed any farm machinery for rental. Add your first piece of equipment to start earning.
          </p>
          <button
            onClick={() => navigate('/partner/equipment/add')}
            className="px-5 py-2.5 bg-green-700 text-white font-semibold rounded-xl hover:bg-green-800 transition text-sm"
          >
            Add Your First Equipment
          </button>
        </div>
      ) : (
        /* Partner Equipment Grid */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {equipmentList.map((item) => {
            const badge = getStatusBadgeInfo(item.availabilityStatus);
            const imageSrc = item.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;
            const isProcessing = actionLoading === item.id;

            return (
              <div
                key={item.id}
                className={`bg-white rounded-xl border ${
                  item.isDisabled ? 'border-red-200 bg-red-50/20' : 'border-gray-200'
                } overflow-hidden shadow-sm hover:shadow-md transition flex flex-col justify-between`}
              >
                <div>
                  <div className="relative h-44 w-full bg-gray-100 overflow-hidden">
                    <img
                      src={imageSrc}
                      alt={item.name}
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                      }}
                    />

                    <span
                      className={`absolute top-3 right-3 text-xs font-semibold px-2.5 py-1 rounded-full border shadow-sm ${badge.badgeClass}`}
                    >
                      {badge.label}
                    </span>

                    {item.isDisabled && (
                      <span className="absolute top-3 left-3 bg-red-600 text-white text-xs font-bold px-2.5 py-1 rounded-md">
                        DISABLED
                      </span>
                    )}
                  </div>

                  <div className="p-5 space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <span className="text-xs font-semibold text-green-700 uppercase tracking-wide">
                          {formatCategoryLabel(item.category)}
                        </span>
                        <h3 className="text-lg font-bold text-gray-900 line-clamp-1">{item.name}</h3>
                      </div>
                    </div>

                    <p className="text-xs text-gray-500 flex items-center gap-1">
                      📍 {item.locationAddress}
                    </p>

                    <div className="flex items-baseline justify-between pt-2 border-t border-gray-100">
                      <div>
                        <span className="text-xl font-extrabold text-green-800">
                          ₹{Number(item.rentalPrice).toLocaleString('en-IN')}
                        </span>
                        <span className="text-xs text-gray-500"> / day</span>
                      </div>
                      <span className="text-xs text-gray-500 font-medium">{item.brand} {item.model}</span>
                    </div>
                  </div>
                </div>

                {/* Actions Footer */}
                <div className="bg-gray-50 px-5 py-3 border-t border-gray-100 flex items-center justify-between gap-2 text-xs">
                  <button
                    disabled={isProcessing}
                    onClick={() => handleToggleEnable(item.id, item.isDisabled)}
                    className={`font-semibold px-3 py-1.5 rounded-lg border transition ${
                      item.isDisabled
                        ? 'bg-green-50 text-green-700 border-green-300 hover:bg-green-100'
                        : 'bg-amber-50 text-amber-700 border-amber-300 hover:bg-amber-100'
                    }`}
                  >
                    {isProcessing ? 'Processing...' : item.isDisabled ? 'Re-enable' : 'Disable'}
                  </button>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => navigate(`/partner/equipment/add?edit=${item.id}`)}
                      className="px-3 py-1.5 font-semibold text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-100 transition"
                    >
                      Edit
                    </button>

                    <button
                      disabled={isProcessing}
                      onClick={() => handleDelete(item.id, item.name)}
                      className="px-3 py-1.5 font-semibold text-red-600 bg-white border border-red-200 rounded-lg hover:bg-red-50 transition"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default MyEquipment;