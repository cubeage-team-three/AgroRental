import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  Tractor,
  PlusCircle,
  Search,
  Filter,
  MapPin,
  Fuel,
  Gauge,
  Calendar,
  AlertCircle,
  CheckCircle2,
  Trash2,
  Edit3,
  Power,
  RefreshCw,
  SlidersHorizontal,
} from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { equipmentService } from '../../services/equipmentService';
import {
  DEFAULT_EQUIPMENT_IMAGE,
  formatCategoryLabel,
  getStatusBadgeInfo,
  EQUIPMENT_CATEGORIES,
} from '../../utils/constants';

function MyEquipment() {
  const navigate = useNavigate();
  const partnerId = getPartnerId();

  const [equipmentList, setEquipmentList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);

  // Search & Filter State
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  // Delete Confirmation Modal State
  const [deleteModalItem, setDeleteModalItem] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const fetchPartnerEquipment = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const data = await equipmentService.getPartnerEquipment(partnerId);
      setEquipmentList(data || []);
    } catch (err) {
      console.error('Failed to load partner equipment:', err);
      setError(err.message || 'Failed to load your machinery listings. Please ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  }, [partnerId]);

  useEffect(() => {
    fetchPartnerEquipment();
  }, [fetchPartnerEquipment]);

  // Handle Enable / Disable Toggle
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

  // Handle Delete Confirmation
  const confirmDelete = async () => {
    if (!deleteModalItem) return;
    setDeleting(true);

    try {
      await equipmentService.deleteEquipment(deleteModalItem.id, partnerId);
      setDeleteModalItem(null);
      await fetchPartnerEquipment();
    } catch (err) {
      alert(err.message || 'Failed to delete machinery listing');
    } finally {
      setDeleting(false);
    }
  };

  // Filtered Equipment List
  const filteredList = equipmentList.filter((item) => {
    const matchesSearch =
      searchTerm === '' ||
      item.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (item.brand && item.brand.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (item.model && item.model.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (item.locationAddress && item.locationAddress.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesCategory =
      selectedCategory === 'ALL' || item.category === selectedCategory;

    const matchesStatus =
      selectedStatus === 'ALL' ||
      (selectedStatus === 'DISABLED' ? item.isDisabled : !item.isDisabled && item.availabilityStatus === selectedStatus);

    return matchesSearch && matchesCategory && matchesStatus;
  });

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            My Equipment Fleet
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Manage your registered machinery listings, pricing rates, and operational rental states.
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/partner/equipment/add')}
          className="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs sm:text-sm font-extrabold rounded-xl shadow-md transition-all duration-150"
        >
          <PlusCircle className="w-4 h-4" />
          <span>+ Add New Equipment</span>
        </button>
      </div>

      {/* Search & Category Filter Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 shadow-sm border border-gray-100 space-y-3">
        
        <div className="flex flex-col md:flex-row items-center gap-3">
          
          {/* Search Input */}
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by machinery title, brand, model or location..."
              className="w-full pl-10 pr-4 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
            />
          </div>

          {/* Status Dropdown */}
          <div className="w-full md:w-48">
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
            >
              <option value="ALL">All Statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="BOOKED">Booked</option>
              <option value="UNDER_MAINTENANCE">Under Maintenance</option>
              <option value="DISABLED">Disabled Listings</option>
            </select>
          </div>

        </div>

        {/* Category Pills Strip */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
          <button
            type="button"
            onClick={() => setSelectedCategory('ALL')}
            className={`px-3 py-1.5 rounded-lg font-extrabold whitespace-nowrap transition-all ${
              selectedCategory === 'ALL'
                ? 'bg-[#142E1C] text-white shadow-xs'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            All Categories ({equipmentList.length})
          </button>

          {EQUIPMENT_CATEGORIES.map((cat) => {
            const count = equipmentList.filter((e) => e.category === cat.value).length;
            return (
              <button
                key={cat.value}
                type="button"
                onClick={() => setSelectedCategory(cat.value)}
                className={`px-3 py-1.5 rounded-lg font-bold whitespace-nowrap transition-all ${
                  selectedCategory === cat.value
                    ? 'bg-[#3E7B27] text-white shadow-xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                {cat.label} {count > 0 ? `(${count})` : ''}
              </button>
            );
          })}
        </div>

      </div>

      {/* Feedback Banner */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={fetchPartnerEquipment}
            className="text-xs font-bold text-red-800 underline hover:no-underline"
          >
            Retry Connection
          </button>
        </div>
      )}

      {/* Main Content Area */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((idx) => (
            <div key={idx} className="bg-white rounded-3xl p-4 space-y-3 animate-pulse border border-gray-100">
              <div className="w-full h-44 bg-gray-200 rounded-2xl" />
              <div className="h-5 bg-gray-200 rounded w-3/4" />
              <div className="h-4 bg-gray-200 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : filteredList.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-emerald-50 text-[#3E7B27] rounded-3xl flex items-center justify-center mx-auto text-3xl shadow-xs">
            <Tractor className="w-8 h-8" />
          </div>
          <div className="space-y-1 max-w-md mx-auto">
            <h3 className="text-lg font-black text-gray-900">No Machinery Listings Found</h3>
            <p className="text-xs text-gray-500">
              {searchTerm || selectedCategory !== 'ALL' || selectedStatus !== 'ALL'
                ? 'No machinery matches your active search filters. Try resetting the filters.'
                : "You haven't listed any farm machinery for rental yet. Add your first piece of equipment to start earning."}
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              setSearchTerm('');
              setSelectedCategory('ALL');
              setSelectedStatus('ALL');
              if (equipmentList.length === 0) navigate('/partner/equipment/add');
            }}
            className="px-5 py-2.5 bg-[#3E7B27] text-white text-xs font-bold rounded-xl hover:bg-[#2E6F22] transition-colors"
          >
            {equipmentList.length === 0 ? '+ List Your First Equipment' : 'Reset Search Filters'}
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredList.map((item) => {
            const badge = getStatusBadgeInfo(item.availabilityStatus);
            const imageSrc = item.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;
            const isProcessing = actionLoading === item.id;

            return (
              <div
                key={item.id}
                className={`bg-white rounded-3xl border ${
                  item.isDisabled ? 'border-red-200 bg-red-50/10' : 'border-gray-100'
                } overflow-hidden shadow-sm hover:shadow-lg transition-all flex flex-col justify-between group`}
              >
                <div>
                  
                  {/* Card Thumbnail Hero Image */}
                  <div className="relative h-48 w-full bg-gray-100 overflow-hidden">
                    <img
                      src={imageSrc}
                      alt={item.name}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                      }}
                    />

                    {/* Status Badge */}
                    <span
                      className={`absolute top-3 right-3 text-[10px] font-black uppercase px-2.5 py-1 rounded-full border shadow-xs ${badge.badgeClass}`}
                    >
                      {badge.label}
                    </span>

                    {/* Disabled Marker */}
                    {item.isDisabled && (
                      <span className="absolute top-3 left-3 bg-red-600 text-white text-[10px] font-black px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-xs">
                        DISABLED
                      </span>
                    )}
                  </div>

                  {/* Card Content Details */}
                  <div className="p-5 space-y-3">
                    
                    <div>
                      <span className="text-[10px] font-black uppercase tracking-wider text-[#3E7B27]">
                        {formatCategoryLabel(item.category)}
                      </span>
                      <h3 className="text-base font-extrabold text-gray-900 line-clamp-1 mt-0.5">
                        {item.name}
                      </h3>
                    </div>

                    <p className="text-xs text-gray-500 flex items-center gap-1 truncate">
                      <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{item.locationAddress}</span>
                    </p>

                    {/* Quick Specs Strip */}
                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 text-[11px] text-gray-600 font-semibold">
                      <span className="flex items-center gap-1 truncate">
                        <Gauge className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>{item.capacity || 'Standard'}</span>
                      </span>
                      <span className="flex items-center gap-1 truncate">
                        <Calendar className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>Year {item.manufacturingYear}</span>
                      </span>
                    </div>

                    {/* Pricing */}
                    <div className="pt-2 flex items-baseline justify-between border-t border-gray-100">
                      <div>
                        <span className="text-xl font-black text-[#142E1C]">
                          ₹{Number(item.rentalPrice).toLocaleString('en-IN')}
                        </span>
                        <span className="text-xs text-gray-500 font-medium"> / day</span>
                      </div>
                      <span className="text-xs font-bold text-gray-500">{item.brand} {item.model}</span>
                    </div>

                  </div>

                </div>

                {/* Card Action Buttons Footer */}
                <div className="bg-[#F8FAF8] px-5 py-3 border-t border-gray-100 flex items-center justify-between gap-2 text-xs">
                  
                  {/* Enable / Disable Toggle Button */}
                  <button
                    disabled={isProcessing}
                    onClick={() => handleToggleEnable(item.id, item.isDisabled)}
                    className={`font-bold px-3 py-1.5 rounded-xl border transition-all ${
                      item.isDisabled
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100'
                        : 'bg-amber-50 text-amber-800 border-amber-300 hover:bg-amber-100'
                    }`}
                  >
                    {isProcessing ? (
                      <span>Processing...</span>
                    ) : item.isDisabled ? (
                      'Re-enable'
                    ) : (
                      'Disable'
                    )}
                  </button>

                  <div className="flex items-center gap-2">
                    
                    {/* Edit Button */}
                    <button
                      onClick={() => navigate(`/partner/equipment/add?edit=${item.id}`)}
                      className="px-3 py-1.5 font-bold text-gray-700 bg-white border border-gray-200 hover:border-gray-300 rounded-xl transition-all shadow-2xs flex items-center gap-1"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                      <span>Edit</span>
                    </button>

                    {/* Delete Button */}
                    <button
                      disabled={isProcessing}
                      onClick={() => setDeleteModalItem(item)}
                      className="px-2.5 py-1.5 font-bold text-red-600 bg-white border border-red-200 hover:bg-red-50 rounded-xl transition-all shadow-2xs"
                      title="Delete Equipment"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>

                  </div>

                </div>

              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteModalItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white w-full max-w-md rounded-3xl shadow-2xl p-6 sm:p-7 border border-gray-100 space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center mx-auto text-xl">
              <Trash2 className="w-6 h-6" />
            </div>

            <div className="text-center space-y-1">
              <h3 className="text-lg font-black text-gray-900">Delete Machinery Listing?</h3>
              <p className="text-xs text-gray-500 leading-relaxed">
                Are you sure you want to remove <strong className="text-gray-800 font-bold">"{deleteModalItem.name}"</strong>?
                This will soft-deactivate the machine listing from regional rental searches.
              </p>
            </div>

            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={() => setDeleteModalItem(null)}
                className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-xs rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={confirmDelete}
                className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl transition-colors shadow-md disabled:opacity-70"
              >
                {deleting ? 'Deleting...' : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

export default MyEquipment;