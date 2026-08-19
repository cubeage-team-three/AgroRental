import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  HardHat,
  Calendar,
  CheckCircle2,
  Clock,
  MapPin,
  Tractor,
  ArrowRight,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import { getOperatorId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { DEFAULT_EQUIPMENT_IMAGE, formatCategoryLabel } from '../../utils/constants';

function OperatorDashboard() {
  const operatorId = getOperatorId();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchJobs = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await bookingService.getBookingsByOperator(operatorId);
      setJobs(data || []);
    } catch (err) {
      console.error('Failed to load operator dashboard:', err);
      setError(err.message || 'Failed to retrieve operator tasks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchJobs();
  }, [operatorId]);

  const activeJobs = jobs.filter((j) => j.status === 'CONFIRMED' || j.status === 'PENDING');
  const completedJobs = jobs.filter((j) => j.status === 'COMPLETED');

  return (
    <div className="max-w-7xl mx-auto space-y-6 font-sans">
      
      {/* Welcome Banner */}
      <div className="bg-linear-to-r from-[#142E1C] via-[#1F4529] to-[#2E6F22] rounded-3xl p-6 sm:p-8 text-white shadow-lg relative overflow-hidden">
        <div className="relative z-10 space-y-2">
          <span className="px-3 py-1 bg-emerald-500/20 border border-emerald-400/30 text-emerald-300 rounded-full text-xs font-extrabold uppercase tracking-wider">
            Operator Console
          </span>
          <h1 className="text-2xl sm:text-4xl font-black tracking-tight">
            Machinery Operations Hub
          </h1>
          <p className="text-xs sm:text-sm text-emerald-100/80 max-w-xl">
            Track your assigned equipment deployments, inspect field locations, and log completed jobs.
          </p>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        
        <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-[#3E7B27] flex items-center justify-center font-black text-xl shrink-0">
            <HardHat className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-400 font-bold uppercase tracking-wider block">Total Tasks</span>
            <span className="text-2xl font-black text-gray-900">{jobs.length}</span>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-50 text-amber-600 flex items-center justify-center font-black text-xl shrink-0">
            <Clock className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-400 font-bold uppercase tracking-wider block">Active Scheduled</span>
            <span className="text-2xl font-black text-gray-900">{activeJobs.length}</span>
          </div>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-gray-100 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center font-black text-xl shrink-0">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <span className="text-xs text-gray-400 font-bold uppercase tracking-wider block">Completed Jobs</span>
            <span className="text-2xl font-black text-gray-900">{completedJobs.length}</span>
          </div>
        </div>

      </div>

      {/* Active Jobs Strip */}
      <div className="bg-white rounded-3xl border border-gray-100 p-6 space-y-4 shadow-sm">
        <div className="flex items-center justify-between border-b border-gray-100 pb-3">
          <div className="flex items-center gap-2">
            <Tractor className="w-5 h-5 text-[#3E7B27]" />
            <h2 className="text-base font-black text-[#142E1C] uppercase tracking-wider">
              Active Assigned Field Deployments
            </h2>
          </div>
          <Link
            to="/operator/jobs"
            className="text-xs font-bold text-[#3E7B27] hover:text-[#2E6F22] flex items-center gap-1"
          >
            <span>View All Jobs</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {loading ? (
          <div className="p-6 text-center text-xs text-gray-400 animate-pulse">
            Loading assigned field deployments...
          </div>
        ) : activeJobs.length === 0 ? (
          <div className="p-8 text-center space-y-2 bg-[#F8FAF8] rounded-2xl border border-dashed border-gray-200">
            <p className="text-xs text-gray-500 font-semibold">No active field deployments scheduled.</p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100">
            {activeJobs.map((job) => (
              <div key={job.id} className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-16 h-16 rounded-2xl overflow-hidden bg-gray-100 shrink-0">
                    <img
                      src={job.primaryImageUrl || DEFAULT_EQUIPMENT_IMAGE}
                      alt="Equipment"
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        e.target.onerror = null;
                        e.target.src = DEFAULT_EQUIPMENT_IMAGE;
                      }}
                    />
                  </div>
                  <div>
                    <span className="text-[10px] font-black uppercase text-[#3E7B27]">
                      Job #{job.id} • {formatCategoryLabel(job.equipmentCategory)}
                    </span>
                    <h3 className="text-sm font-extrabold text-gray-900 line-clamp-1">
                      {job.equipmentName || `Machine #${job.equipmentId}`}
                    </h3>
                    <p className="text-xs text-gray-500 flex items-center gap-1 mt-0.5">
                      <MapPin className="w-3 h-3 text-gray-400" />
                      <span>{job.deliveryAddress || 'Field Location Specified'}</span>
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 justify-end">
                  <div className="text-right text-xs">
                    <span className="font-bold text-gray-900 block">{job.startDate} → {job.endDate}</span>
                    <span className="text-emerald-700 font-extrabold">₹{Number(job.totalCost || 0).toLocaleString('en-IN')}</span>
                  </div>
                  <Link
                    to={`/operator/jobs/${job.id}`}
                    className="px-3.5 py-2 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-bold rounded-xl transition-colors shadow-2xs"
                  >
                    Manage Job
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}

export default OperatorDashboard;

