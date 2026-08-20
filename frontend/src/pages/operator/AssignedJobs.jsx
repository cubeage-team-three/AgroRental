import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  MapPin,
  Clock,
  AlertCircle,
  Search,
  Tractor,
  Eye,
  RefreshCw,
  FileText,
  ShieldCheck,
} from 'lucide-react';
import { operatorService } from '../../services/operatorService';
import { DEFAULT_EQUIPMENT_IMAGE, formatCategoryLabel } from '../../utils/constants';

function AssignedJobs() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchJobs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await operatorService.getAssignedJobs({
        page,
        size: 9,
      });

      if (response && response.content) {
        setJobs(response.content);
        setTotalPages(response.totalPages || 1);
        setTotalElements(response.totalElements || response.content.length);
      } else if (Array.isArray(response)) {
        setJobs(response);
        setTotalElements(response.length);
      } else {
        setJobs([]);
        setTotalElements(0);
      }
    } catch (err) {
      console.error('Failed to load operator jobs:', err);
      setError(err.message || 'Failed to retrieve assigned tasks.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchJobs();
  }, [fetchJobs]);

  const filteredJobs = jobs.filter((job) => {
    const matchesSearch =
      searchTerm === '' ||
      job.equipmentName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      job.deliveryAddress?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      String(job.bookingId).includes(searchTerm) ||
      String(job.assignmentId).includes(searchTerm);

    const matchesStatus =
      statusFilter === 'ALL' || job.assignmentStatus === statusFilter;

    return matchesSearch && matchesStatus;
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ASSIGNED':
        return 'bg-amber-100 text-amber-800 border-amber-300';
      case 'ACCEPTED':
        return 'bg-blue-100 text-blue-800 border-blue-300';
      case 'TRAVELING':
        return 'bg-indigo-100 text-indigo-800 border-indigo-300';
      case 'REACHED':
        return 'bg-teal-100 text-teal-800 border-teal-300';
      case 'IN_PROGRESS':
        return 'bg-emerald-100 text-emerald-800 border-emerald-300';
      case 'PAUSED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'COMPLETED':
        return 'bg-green-100 text-green-900 border-green-400';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-300';
      case 'CANCELLED':
        return 'bg-gray-100 text-gray-800 border-gray-300';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-emerald-100 text-emerald-800">
              <HardHat className="w-6 h-6 text-[#3E7B27]" />
            </span>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Assigned Field Jobs
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 mt-1">
            Manage your fieldwork tasks, machinery dispatch, and live operational status.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchJobs}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors shadow-2xs"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
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
              placeholder="Search by task ID, booking reference, machine title, or farm location..."
              className="w-full pl-10 pr-4 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white transition-all"
            />
          </div>

          <div className="w-full md:w-56">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#F0EFE9] border border-transparent rounded-xl text-xs sm:text-sm font-bold text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#3E7B27] focus:bg-white"
            >
              <option value="ALL">All Statuses ({totalElements})</option>
              <option value="ASSIGNED">New Assignment (ASSIGNED)</option>
              <option value="ACCEPTED">Accepted / Scheduled</option>
              <option value="TRAVELING">En Route (TRAVELING)</option>
              <option value="REACHED">Arrived (REACHED)</option>
              <option value="IN_PROGRESS">Working (IN_PROGRESS)</option>
              <option value="PAUSED">Paused</option>
              <option value="COMPLETED">Completed</option>
              <option value="REJECTED">Declined (REJECTED)</option>
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
            className="text-xs font-bold text-red-800 underline hover:text-red-900"
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
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-4 shadow-xs">
          <div className="w-16 h-16 bg-amber-50 text-amber-600 rounded-3xl flex items-center justify-center mx-auto text-3xl shadow-xs">
            <HardHat className="w-8 h-8 text-[#3E7B27]" />
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
            const imageSrc = job.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE;

            return (
              <div
                key={job.assignmentId}
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
                      className={`absolute top-3 right-3 text-[10px] font-black uppercase px-2.5 py-1 rounded-full border shadow-xs ${getStatusBadge(
                        job.assignmentStatus
                      )}`}
                    >
                      {job.assignmentStatus}
                    </span>

                    <span className="absolute top-3 left-3 bg-[#142E1C] text-white text-[10px] font-black px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-xs">
                      Task #{job.assignmentId}
                    </span>
                  </div>

                  <div className="p-5 space-y-3">
                    <div>
                      <span className="text-[10px] font-black uppercase tracking-wider text-[#3E7B27]">
                        {formatCategoryLabel(job.equipmentCategory)}
                      </span>
                      <h3 className="text-base font-extrabold text-gray-900 line-clamp-1 mt-0.5">
                        {job.equipmentName || `Machinery #${job.equipmentId || job.bookingId}`}
                      </h3>
                      <p className="text-[11px] text-gray-400 font-semibold">
                        Booking Reference: #{job.bookingId}
                      </p>
                    </div>

                    <p className="text-xs text-gray-500 flex items-center gap-1.5 truncate">
                      <MapPin className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                      <span>{job.deliveryAddress || 'Farm delivery address specified in booking'}</span>
                    </p>

                    <div className="grid grid-cols-2 gap-2 pt-2 border-t border-gray-100 text-[11px] text-gray-600 font-semibold">
                      <span className="flex items-center gap-1 truncate">
                        <Calendar className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>Start: {job.startDate || 'Scheduled'}</span>
                      </span>
                      <span className="flex items-center gap-1 truncate">
                        <Calendar className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                        <span>End: {job.endDate || 'Scheduled'}</span>
                      </span>
                    </div>

                    {job.notes && (
                      <div className="p-2.5 bg-[#F0EFE9] rounded-xl text-[11px] text-gray-700 font-medium">
                        <span className="font-bold text-gray-900">Notes: </span>
                        <span>{job.notes}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-[#F8FAF8] px-5 py-3.5 border-t border-gray-100 flex items-center justify-between gap-2 text-xs">
                  <div className="text-[11px] text-gray-500 font-medium flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-gray-400" />
                    <span>Assigned {job.assignedAt ? new Date(job.assignedAt).toLocaleDateString() : ''}</span>
                  </div>

                  <Link
                    to={`/operator/jobs/${job.assignmentId}`}
                    className="px-3.5 py-1.5 font-bold text-white bg-[#3E7B27] hover:bg-[#2E6F22] rounded-xl transition-all shadow-xs flex items-center gap-1.5"
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>Manage Task</span>
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 pt-4">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            className="px-4 py-2 bg-white border border-gray-200 text-gray-700 text-xs font-bold rounded-xl disabled:opacity-50 hover:bg-gray-50 transition-colors"
          >
            Previous
          </button>
          <span className="text-xs font-bold text-gray-600">
            Page {page + 1} of {totalPages} ({totalElements} total)
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((prev) => prev + 1)}
            className="px-4 py-2 bg-white border border-gray-200 text-gray-700 text-xs font-bold rounded-xl disabled:opacity-50 hover:bg-gray-50 transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}

export default AssignedJobs;
