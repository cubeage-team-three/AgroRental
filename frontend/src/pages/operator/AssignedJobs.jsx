import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  MapPin,
  Clock,
  CheckCircle2,
  AlertCircle,
  Search,
  Tractor,
  Eye,
  RefreshCw,
  XCircle,
} from 'lucide-react';
import { getOperatorId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { DEFAULT_EQUIPMENT_IMAGE, formatCategoryLabel } from '../../utils/constants';

function AssignedJobs() {
  const navigate = useNavigate();
  const operatorId = getOperatorId();

  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [actionLoading, setActionLoading] = useState(null);

  const fetchJobs = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await bookingService.getBookingsByOperator(operatorId);
      setJobs(data || []);
    } catch (err) {
      console.error('Failed to load operator jobs:', err);
      setError(err.message || 'Failed to retrieve assigned tasks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [operatorId]);

  const handleStatusUpdate = async (jobId, newStatus) => {
    setActionLoading(jobId);
    try {
      await bookingService.updateBookingStatus(jobId, {
        status: newStatus,
        operatorId,
      });
      await fetchJobs();
    } catch (err) {
      alert(err.message || 'Failed to update job status');
    } finally {
      setActionLoading(null);
    }
  };

  const filteredJobs = jobs.filter((job) => {
    const matchesSearch =
      searchTerm === '' ||
      job.equipmentName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      job.deliveryAddress?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      String(job.id).includes(searchTerm);

    const matchesStatus =
      statusFilter === 'ALL' || job.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Assigned Field Jobs
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Manage your scheduled machinery operations, field tasks, and job completions.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchJobs}
          className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Refresh Tasks</span>
        </button>
      </div>

      {/* Filter & Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 shadow-sm border border-gray-100 space-y-3">
        <div className="flex flex-col md:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by job ID, machinery title, or delivery address..."
              className="w-full pl-10 pr-4 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
            />
          </div>

          <div className="w-full md:w-48">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
            >
              <option value="ALL">All Statuses</option>
              <option value="CONFIRMED">Confirmed / Scheduled</option>
              <option value="PENDING">Pending Approval</option>
              <option value="COMPLETED">Completed Jobs</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 shrink-0 text-red-500" />
            <span>{error}</span>
          </div>
          <button
            type="button"
            onClick={fetchJobs}
            className="text-xs font-bold text-red-800 underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* Jobs Grid / List */}
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
      ) : filteredJobs.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-4">
          <div className="w-16 h-16 bg-amber-50 text-amber-600 rounded-3xl flex items-center justify-center mx-auto text-3xl shadow-xs">
            <HardHat className="w-8 h-8" />
          </div>
          <div className="space-y-1 max-w-md mx-auto">
            <h3 className="text-lg font-black text-gray-900">No Assigned Jobs Found</h3>
            <p className="text-xs text-gray-500">
              {searchTerm || statusFilter !== 'ALL'
                ? 'No assigned jobs match your active search filters.'
                : 'You currently have no equipment operation tasks assigned by equipment partners.'}
            </p>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredJobs.map((job) => {
            const isUpdating = actionLoading === job.id;
            const imageSrc = job.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

            return (
              <div
                key={job.id}
                className="bg-white rounded-3xl border border-gray-100 overflow-hidden shadow-sm hover:shadow-lg transition-all flex flex-col justify-between group"
              >
                <div>
                  <div className="relative h-44 w-full bg-gray-100 overflow-hidden">
                    <img
                      src={imageSrc}
                      alt={job.equipmentName || 'Equipment'}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                      }}
                    />

                    <span
                      className={`absolute top-3 right-3 text-[10px] font-black uppercase px-2.5 py-1 rounded-full border shadow-xs ${
                        job.status === 'CONFIRMED'
                          ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                          : job.status === 'COMPLETED'
                          ? 'bg-blue-100 text-blue-800 border-blue-300'
                          : job.status === 'CANCELLED'
                          ? 'bg-red-100 text-red-800 border-red-300'
                          : 'bg-amber-100 text-amber-800 border-amber-300'
                      }`}
                    >
                      {job.status}
                    </span>

                    <span className="absolute top-3 left-3 bg-[#142E1C] text-white text-[10px] font-black px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-xs">
                      Job #{job.id}
                    </span>
                  </div>

                  <div className="p-5 space-y-3">
                    <div>
                      <span className="text-[10px] font-black uppercase tracking-wider text-[#3E7B27]">
                        {formatCategoryLabel(job.equipmentCategory)}
                      </span>
                      <h3 className="text-base font-extrabold text-gray-900 line-clamp-1 mt-0.5">
                        {job.equipmentName || `Machine #${job.equipmentId}`}
                      </h3>
                    </div>

                    <p className="text-xs text-gray-500 flex items-center gap-1 truncate">
                      <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{job.deliveryAddress || 'Field Address Specified'}</span>
                    </p>

                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 text-[11px] text-gray-600 font-semibold">
                      <span className="flex items-center gap-1 truncate">
                        <Calendar className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>Start: {job.startDate}</span>
                      </span>
                      <span className="flex items-center gap-1 truncate">
                        <Calendar className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>End: {job.endDate}</span>
                      </span>
                    </div>

                    <div className="pt-2 flex items-baseline justify-between border-t border-gray-100">
                      <div>
                        <span className="text-lg font-black text-[#142E1C]">
                          ₹{Number(job.totalCost || 0).toLocaleString('en-IN')}
                        </span>
                        <span className="text-xs text-gray-500 font-medium"> total</span>
                      </div>
                      <span className="text-xs font-bold text-gray-500">Farmer #{job.farmerId}</span>
                    </div>
                  </div>
                </div>

                <div className="bg-[#F8FAF8] px-5 py-3 border-t border-gray-100 flex items-center justify-between gap-2 text-xs">
                  <Link
                    to={`/operator/jobs/${job.id}`}
                    className="px-3 py-1.5 font-bold text-gray-700 bg-white border border-gray-200 hover:border-gray-300 rounded-xl transition-all shadow-2xs flex items-center gap-1"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>View Details</span>
                  </Link>

                  {job.status === 'CONFIRMED' && (
                    <button
                      type="button"
                      disabled={isUpdating}
                      onClick={() => handleStatusUpdate(job.id, 'COMPLETED')}
                      className="px-3 py-1.5 font-bold text-white bg-[#3E7B27] hover:bg-[#2E6F22] rounded-xl transition-all shadow-xs disabled:opacity-70 flex items-center gap-1"
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>{isUpdating ? 'Updating...' : 'Mark Completed'}</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default AssignedJobs;

