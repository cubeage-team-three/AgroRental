import { useState, useEffect, useCallback } from 'react';
import { farmService } from '../../services/farmService';
import { getFarmerId } from '../../services/authService';
import { Plus, MapPin, Sprout, Edit2, Trash2, X, Check, Globe, Layers, Navigation } from 'lucide-react';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function MyFarms() {
  const [farms, setFarms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingFarm, setEditingFarm] = useState(null);
  const [deletingFarmId, setDeletingFarmId] = useState(null);

  // Form State
  const [formData, setFormData] = useState({
    farmName: '',
    village: '',
    taluka: '',
    district: '',
    state: 'Maharashtra',
    farmArea: '',
    cropType: '',
    latitude: '',
    longitude: '',
  });

  const [formSubmitting, setFormSubmitting] = useState(false);

  const fetchFarms = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const activeFarmerId = getFarmerId() || 1;
      const data = await farmService.getFarms(activeFarmerId);
      setFarms(data || []);
    } catch (err) {
      console.error('Error fetching farms:', err);
      setError('Failed to load farms list.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFarms();
  }, [fetchFarms]);

  const handleOpenAddModal = () => {
    setEditingFarm(null);
    setFormData({
      farmName: '',
      village: '',
      taluka: '',
      district: '',
      state: 'Maharashtra',
      farmArea: '',
      cropType: '',
      latitude: '',
      longitude: '',
    });
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (farm) => {
    setEditingFarm(farm);
    setFormData({
      farmName: farm.farmName || '',
      village: farm.village || '',
      taluka: farm.taluka || '',
      district: farm.district || '',
      state: farm.state || 'Maharashtra',
      farmArea: farm.farmArea || '',
      cropType: farm.cropType || '',
      latitude: farm.latitude || '',
      longitude: farm.longitude || '',
    });
    setIsModalOpen(true);
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleGetLocation = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setFormData((prev) => ({
            ...prev,
            latitude: position.coords.latitude.toFixed(6),
            longitude: position.coords.longitude.toFixed(6),
          }));
        },
        (err) => {
          console.warn('Geolocation error:', err);
          alert('Could not retrieve GPS coordinates automatically. Please enter them manually.');
        }
      );
    }
  };

  const handleSubmitForm = async (e) => {
    e.preventDefault();
    setFormSubmitting(true);

    const payload = {
      farmerId: getFarmerId() || 1,
      farmName: formData.farmName,
      village: formData.village,
      taluka: formData.taluka,
      district: formData.district,
      state: formData.state,
      farmArea: parseFloat(formData.farmArea) || 1.0,
      cropType: formData.cropType,
      latitude: formData.latitude ? parseFloat(formData.latitude) : null,
      longitude: formData.longitude ? parseFloat(formData.longitude) : null,
    };

    try {
      if (editingFarm) {
        await farmService.updateFarm(editingFarm.id, payload);
        setActionSuccess('Farm updated successfully!');
      } else {
        await farmService.createFarm(payload);
        setActionSuccess('New farm registered successfully!');
      }
      setIsModalOpen(false);
      fetchFarms();
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      console.error('Save farm error:', err);
      alert(err.message || 'Failed to save farm details. Please verify all required fields (Farm Name, Village, Taluka, District, State, Farm Area).');
    } finally {
      setFormSubmitting(false);
    }
  };

  const handleDeleteFarm = async (id) => {
    try {
      await farmService.deleteFarm(id);
      setDeletingFarmId(null);
      setActionSuccess('Farm removed successfully.');
      setFarms((prev) => prev.filter((f) => f.id !== id));
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      alert('Failed to delete farm.');
    }
  };

  const totalArea = farms.reduce((acc, f) => acc + (parseFloat(f.farmArea) || 0), 0);

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-gradient-to-r from-emerald-800 to-green-700 text-white rounded-2xl p-6 shadow-md">
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-sm h-12 shrink-0">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <Sprout className="h-7 w-7 text-lime-300" />
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">My Farms & Plots</h1>
            </div>
            <p className="text-emerald-100 text-sm mt-1">
              Register and manage your agricultural land parcels to book tailored machinery and services.
            </p>
          </div>
        </div>
        <button
          onClick={handleOpenAddModal}
          className="inline-flex items-center justify-center gap-2 px-5 py-3 bg-lime-400 text-emerald-950 font-bold rounded-xl shadow-lg hover:bg-lime-300 transition-all hover:scale-105 active:scale-95 text-sm"
        >
          <Plus className="h-5 w-5" /> Add New Farm
        </button>
      </div>

      {/* Success Notification */}
      {actionSuccess && (
        <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-4 text-emerald-800 text-sm flex items-center gap-2 animate-in fade-in">
          <Check className="h-5 w-5 text-emerald-600" />
          <span className="font-semibold">{actionSuccess}</span>
        </div>
      )}

      {/* Stats Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4 shadow-sm">
          <div className="p-3 bg-emerald-100 rounded-xl text-emerald-800">
            <Layers className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase">Total Farms Registered</p>
            <p className="text-2xl font-extrabold text-slate-900">{farms.length}</p>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4 shadow-sm">
          <div className="p-3 bg-lime-100 rounded-xl text-lime-800">
            <Globe className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase">Total Cultivable Area</p>
            <p className="text-2xl font-extrabold text-slate-900">{totalArea.toFixed(1)} <span className="text-sm font-semibold text-slate-500">Acres</span></p>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-5 flex items-center gap-4 shadow-sm">
          <div className="p-3 bg-teal-100 rounded-xl text-teal-800">
            <Sprout className="h-6 w-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase">Active Crop Types</p>
            <p className="text-2xl font-extrabold text-slate-900">
              {new Set(farms.map((f) => f.cropType).filter(Boolean)).size}
            </p>
          </div>
        </div>
      </div>

      {/* Farm Cards Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4 animate-pulse">
              <div className="h-6 bg-slate-200 rounded w-2/3" />
              <div className="h-4 bg-slate-200 rounded w-1/2" />
              <div className="h-16 bg-slate-100 rounded-xl" />
            </div>
          ))}
        </div>
      ) : error ? (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-6 text-center text-rose-800 space-y-2">
          <p className="font-bold">{error}</p>
          <button onClick={fetchFarms} className="text-xs underline font-semibold">Try Again</button>
        </div>
      ) : farms.length === 0 ? (
        <div className="bg-white rounded-2xl border-2 border-dashed border-slate-300 p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-emerald-50 text-emerald-700 rounded-full flex items-center justify-center mx-auto text-3xl">
            🌱
          </div>
          <h3 className="text-xl font-bold text-slate-800">No Farms Registered Yet</h3>
          <p className="text-slate-500 text-sm max-w-md mx-auto">
            Add your first farm plot to calculate machinery rental costs and dispatch equipment right to your fields.
          </p>
          <button
            onClick={handleOpenAddModal}
            className="px-6 py-2.5 bg-emerald-700 text-white font-bold rounded-xl hover:bg-emerald-800 transition text-sm"
          >
            + Register Farm Now
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {farms.map((farm) => (
            <div
              key={farm.id}
              className="bg-white rounded-2xl border border-slate-200 shadow-sm hover:shadow-md hover:border-emerald-300 transition flex flex-col justify-between overflow-hidden group"
            >
              <div className="p-6 space-y-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h3 className="text-xl font-extrabold text-slate-900 group-hover:text-emerald-800 transition">
                      {farm.farmName}
                    </h3>
                    <p className="text-xs text-slate-500 mt-1 flex items-center gap-1 font-medium">
                      <MapPin className="h-3.5 w-3.5 text-emerald-600 shrink-0" />
                      {farm.village}, {farm.taluka}, {farm.district}
                    </p>
                  </div>
                  <span className="bg-emerald-100 text-emerald-800 text-xs font-bold px-2.5 py-1 rounded-full shrink-0">
                    {farm.farmArea} Acres
                  </span>
                </div>

                <div className="bg-slate-50 rounded-xl p-3 grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="block text-slate-400 uppercase font-semibold">State</span>
                    <span className="font-bold text-slate-700">{farm.state}</span>
                  </div>
                  <div>
                    <span className="block text-slate-400 uppercase font-semibold">Crop Type</span>
                    <span className="font-bold text-emerald-700">{farm.cropType || 'General Crop'}</span>
                  </div>
                </div>

                {farm.latitude && farm.longitude && (
                  <div className="flex items-center gap-1.5 text-[11px] text-slate-500 bg-emerald-50/60 rounded-lg p-2 font-mono">
                    <Navigation className="h-3.5 w-3.5 text-emerald-600" />
                    GPS: {farm.latitude}, {farm.longitude}
                  </div>
                )}
              </div>

              {/* Card Actions Footer */}
              <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                <button
                  onClick={() => handleOpenEditModal(farm)}
                  className="text-xs font-bold text-slate-600 hover:text-emerald-700 flex items-center gap-1 transition"
                >
                  <Edit2 className="h-3.5 w-3.5" /> Edit Plot
                </button>

                <button
                  onClick={() => setDeletingFarmId(farm.id)}
                  className="text-xs font-bold text-rose-600 hover:text-rose-800 flex items-center gap-1 transition"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Remove
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Farm Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-6 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h2 className="text-xl font-bold text-slate-900">
                {editingFarm ? 'Edit Farm Details' : 'Register New Farm Plot'}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSubmitForm} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Farm / Plot Name *</label>
                <input
                  type="text"
                  name="farmName"
                  required
                  placeholder="e.g. Green Valley Plot #1"
                  value={formData.farmName}
                  onChange={handleFormChange}
                  className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Village *</label>
                  <input
                    type="text"
                    name="village"
                    required
                    placeholder="e.g. Khed"
                    value={formData.village}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Taluka *</label>
                  <input
                    type="text"
                    name="taluka"
                    required
                    placeholder="e.g. Khed"
                    value={formData.taluka}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">District *</label>
                  <input
                    type="text"
                    name="district"
                    required
                    placeholder="e.g. Pune"
                    value={formData.district}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">State *</label>
                  <input
                    type="text"
                    name="state"
                    required
                    placeholder="e.g. Maharashtra"
                    value={formData.state}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Farm Area (Acres) *</label>
                  <input
                    type="number"
                    step="0.1"
                    name="farmArea"
                    required
                    placeholder="e.g. 10.5"
                    value={formData.farmArea}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase mb-1">Crop Type</label>
                  <input
                    type="text"
                    name="cropType"
                    placeholder="e.g. Wheat, Sugarcane"
                    value={formData.cropType}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              {/* Location Coordinates */}
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-700 uppercase flex items-center gap-1">
                    <Navigation className="h-3.5 w-3.5 text-emerald-600" /> GPS Coordinates (Optional)
                  </span>
                  <button
                    type="button"
                    onClick={handleGetLocation}
                    className="text-xs font-bold text-emerald-700 hover:underline"
                  >
                    📍 Detect Location
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <input
                    type="number"
                    step="any"
                    name="latitude"
                    placeholder="Latitude (e.g. 18.8500)"
                    value={formData.latitude}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                  <input
                    type="number"
                    step="any"
                    name="longitude"
                    placeholder="Longitude (e.g. 73.9100)"
                    value={formData.longitude}
                    onChange={handleFormChange}
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-xs focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-5 py-2.5 text-xs font-bold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={formSubmitting}
                  className="px-6 py-2.5 text-xs font-bold text-white bg-emerald-700 rounded-xl hover:bg-emerald-800 transition shadow-md disabled:opacity-50"
                >
                  {formSubmitting ? 'Saving...' : editingFarm ? 'Update Farm' : 'Save Farm'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deletingFarmId && (
        <div className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl space-y-4 text-center">
            <div className="w-12 h-12 bg-rose-100 text-rose-600 rounded-full flex items-center justify-center mx-auto text-xl font-bold">
              ⚠️
            </div>
            <h3 className="text-lg font-bold text-slate-900">Confirm Farm Removal</h3>
            <p className="text-xs text-slate-500">
              Are you sure you want to remove this farm plot? Existing booking records will remain unaffected.
            </p>
            <div className="flex justify-center gap-3 pt-2">
              <button
                onClick={() => setDeletingFarmId(null)}
                className="px-4 py-2 bg-slate-100 text-slate-700 text-xs font-bold rounded-xl hover:bg-slate-200 transition"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDeleteFarm(deletingFarmId)}
                className="px-5 py-2 bg-rose-600 text-white text-xs font-bold rounded-xl hover:bg-rose-700 transition shadow-md"
              >
                Remove Farm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MyFarms;
