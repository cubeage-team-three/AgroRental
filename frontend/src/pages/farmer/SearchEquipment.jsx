import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';
import { equipmentService } from '../../services/equipmentService';
import {
  EQUIPMENT_CATEGORIES,
  DEFAULT_EQUIPMENT_IMAGE,
  getCategoryEquipmentImage,
  formatCategoryLabel,
  getStatusBadgeInfo,
} from '../../utils/constants';

function SearchEquipment() {
  const navigate = useNavigate();

  const [filters, setFilters] = useState({
    category: '',
    minPrice: '',
    maxPrice: '',
    minHp: '',
    locationAddress: '',
    startDate: '',
    endDate: '',
    minRating: '',
    maxDistanceKm: '',
  });

  const [equipmentList, setEquipmentList] = useState([]);
  const [pageMeta, setPageMeta] = useState({
    page: 0,
    size: 9,
    totalPages: 0,
    totalElements: 0,
    first: true,
    last: true,
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchEquipment = useCallback(async (page = 0) => {
    setLoading(true);
    setError(null);

    try {
      const activeFilters = {
        ...(filters.category && { category: filters.category }),
        ...(filters.minPrice && { minPrice: filters.minPrice }),
        ...(filters.maxPrice && { maxPrice: filters.maxPrice }),
        ...(filters.minHp && { minHp: filters.minHp }),
        ...(filters.locationAddress && { locationAddress: filters.locationAddress }),
        ...(filters.startDate && { startDate: filters.startDate }),
        ...(filters.endDate && { endDate: filters.endDate }),
        ...(filters.minRating && { minRating: filters.minRating }),
        ...(filters.maxDistanceKm && { maxDistanceKm: filters.maxDistanceKm, userLat: 18.5204, userLng: 73.8567 }),
      };

      const pageData = await equipmentService.searchEquipmentPage(activeFilters, page, 9);
      
      if (pageData && pageData.content) {
        setEquipmentList(pageData.content);
        setPageMeta({
          page: pageData.number ?? page,
          size: pageData.size ?? 9,
          totalPages: pageData.totalPages ?? 1,
          totalElements: pageData.totalElements ?? pageData.content.length,
          first: pageData.first ?? page === 0,
          last: pageData.last ?? true,
        });
      } else {
        setEquipmentList([]);
      }
    } catch (err) {
      console.error('Failed to search equipment:', err);
      setError(err.message || 'Failed to load equipment listings');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    fetchEquipment(0);
  }, [fetchEquipment]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchEquipment(0);
  };

  const handleResetFilters = () => {
    setFilters({
      category: '',
      minPrice: '',
      maxPrice: '',
      minHp: '',
      locationAddress: '',
      startDate: '',
      endDate: '',
      minRating: '',
      maxDistanceKm: '',
    });
  };

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center overflow-hidden rounded-2xl bg-white px-3 py-1.5 shadow-sm border border-slate-200 h-12 shrink-0">
            <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-green-800 tracking-tight">Explore Rental Machinery</h1>
            <p className="text-gray-600 mt-1">Browse verified farm equipment available for rental in your region.</p>
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <form onSubmit={handleSearchSubmit} className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 sm:p-6 space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Category</label>
            <select
              name="category"
              value={filters.category}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            >
              <option value="">All Categories</option>
              {EQUIPMENT_CATEGORIES.map((cat) => (
                <option key={cat.value} value={cat.value}>
                  {cat.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Location / Address</label>
            <input
              type="text"
              name="locationAddress"
              placeholder="e.g. Pune, Nashik"
              value={filters.locationAddress}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Min Price (₹/day)</label>
            <input
              type="number"
              name="minPrice"
              placeholder="0"
              value={filters.minPrice}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Max Price (₹/day)</label>
            <input
              type="number"
              name="maxPrice"
              placeholder="10000"
              value={filters.maxPrice}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 border-t border-gray-100 pt-3">
          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Available Start Date</label>
            <input
              type="date"
              name="startDate"
              value={filters.startDate}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Available End Date</label>
            <input
              type="date"
              name="endDate"
              value={filters.endDate}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Min Rating (Stars)</label>
            <select
              name="minRating"
              value={filters.minRating}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            >
              <option value="">Any Rating</option>
              <option value="4.5">★ 4.5 & above</option>
              <option value="4.0">★ 4.0 & above</option>
              <option value="3.5">★ 3.5 & above</option>
              <option value="3.0">★ 3.0 & above</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 uppercase mb-1">Max Distance Radius (km)</label>
            <input
              type="number"
              name="maxDistanceKm"
              placeholder="e.g. 50"
              value={filters.maxDistanceKm}
              onChange={handleFilterChange}
              className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-green-500 focus:outline-none"
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={handleResetFilters}
            className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition"
          >
            Reset Filters
          </button>
          <button
            type="submit"
            className="px-6 py-2 text-sm font-semibold text-white bg-green-700 rounded-lg hover:bg-green-800 transition shadow-sm"
          >
            Apply Filters
          </button>
        </div>
      </form>

      {/* Error Banner */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-red-700 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button onClick={() => fetchEquipment(0)} className="underline text-red-800 font-semibold">
            Retry
          </button>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((idx) => (
            <div key={idx} className="bg-white rounded-xl border border-gray-200 p-4 space-y-3 animate-pulse">
              <div className="w-full h-48 bg-gray-200 rounded-lg" />
              <div className="h-5 bg-gray-200 rounded w-3/4" />
              <div className="h-4 bg-gray-200 rounded w-1/2" />
              <div className="h-6 bg-gray-200 rounded w-1/3" />
            </div>
          ))}
        </div>
      ) : equipmentList.length === 0 ? (
        /* Empty State */
        <div className="bg-white border border-dashed border-gray-300 rounded-2xl p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-green-50 text-green-700 rounded-full flex items-center justify-center mx-auto text-2xl">
            🚜
          </div>
          <h3 className="text-xl font-bold text-gray-800">No Machinery Found</h3>
          <p className="text-gray-500 max-w-md mx-auto text-sm">
            We couldn't find any equipment matching your specified filter criteria. Try expanding your search location or price range.
          </p>
          <button
            onClick={handleResetFilters}
            className="px-5 py-2.5 bg-green-700 text-white font-semibold rounded-xl hover:bg-green-800 transition text-sm"
          >
            Clear Filters
          </button>
        </div>
      ) : (
        /* Equipment Cards Grid */
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {equipmentList.map((item) => {
            const badge = getStatusBadgeInfo(item.availabilityStatus);
            const categoryFallback = getCategoryEquipmentImage(item.category);
            const imageSrc = item.primaryImageUrl || (item.images && item.images.length > 0 ? item.images[0].imageUrl : null) || categoryFallback;

            return (
              <div
                key={item.id}
                onClick={() => navigate(`/farmer/equipment/${item.id}`)}
                className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm hover:shadow-md hover:border-green-300 transition cursor-pointer flex flex-col group"
              >
                <div className="relative h-48 w-full bg-gray-100 overflow-hidden">
                  <img
                    src={imageSrc}
                    alt={item.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition duration-300"
                    onError={(e) => {
                      e.target.onerror = null;
                      e.target.src = categoryFallback;
                    }}
                  />
                  <span
                    className={`absolute top-3 right-3 text-xs font-semibold px-2.5 py-1 rounded-full border ${badge.badgeClass}`}
                  >
                    {badge.label}
                  </span>
                  <span className="absolute bottom-3 left-3 bg-black/60 backdrop-blur-md text-white text-xs px-2.5 py-1 rounded-md font-medium">
                    {formatCategoryLabel(item.category)}
                  </span>
                </div>

                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900 group-hover:text-green-700 transition line-clamp-1">
                      {item.name}
                    </h3>
                    <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
                      <span>📍</span> {item.locationAddress}
                    </p>
                  </div>

                  <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
                    <div>
                      <span className="text-2xl font-extrabold text-green-800">₹{Number(item.rentalPrice).toLocaleString('en-IN')}</span>
                      <span className="text-xs text-gray-500"> / day</span>
                    </div>
                    <span className="text-xs font-semibold text-green-700 hover:underline">
                      View Details →
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls */}
      {!loading && pageMeta.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-gray-200 pt-4">
          <p className="text-xs text-gray-500">
            Showing Page <span className="font-semibold text-gray-800">{pageMeta.page + 1}</span> of{' '}
            <span className="font-semibold text-gray-800">{pageMeta.totalPages}</span> ({pageMeta.totalElements} items)
          </p>

          <div className="flex gap-2">
            <button
              disabled={pageMeta.first}
              onClick={() => fetchEquipment(pageMeta.page - 1)}
              className="px-3.5 py-1.5 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              ← Previous
            </button>
            <button
              disabled={pageMeta.last}
              onClick={() => fetchEquipment(pageMeta.page + 1)}
              className="px-3.5 py-1.5 border border-gray-300 rounded-lg text-xs font-semibold text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              Next →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default SearchEquipment;
