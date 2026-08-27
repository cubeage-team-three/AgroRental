import { useState, useEffect, useCallback } from 'react';
import { complaintService } from '../../services/complaintService';
import {
  ShieldCheck,
  AlertCircle,
  CheckCircle2,
  Clock,
  RefreshCw,
  Search,
  MessageSquareWarning,
  User,
  Phone,
  Calendar,
  X,
} from 'lucide-react';

const STATUS_OPTIONS = ['ALL', 'OPEN', 'UNDER_REVIEW', 'RESOLVED', 'CLOSED'];

const STATUS_STYLES = {
  OPEN: 'bg-red-100 text-red-800 border-red-300',
  UNDER_REVIEW: 'bg-amber-100 text-amber-800 border-amber-300',
  RESOLVED: 'bg-emerald-100 text-emerald-800 border-emerald-300',
  CLOSED: 'bg-gray-100 text-gray-600 border-gray-300',
};

const CATEGORY_LABELS = {
  LATE_ARRIVAL: 'Late Arrival',
  EQUIPMENT_ISSUE: 'Equipment Issue',
  PAYMENT_ISSUE: 'Payment Issue',
  OPERATOR_ISSUE: 'Operator Issue',
  OTHER: 'Other',
};

function StatusBadge({ status }) {
  return (
    <span
      className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-black uppercase tracking-wide border shadow-2xs ${
        STATUS_STYLES[status] || 'bg-gray-100 text-gray-600 border-gray-300'
      }`}
    >
      {status === 'RESOLVED' && <CheckCircle2 className="w-3.5 h-3.5" />}
      {status === 'OPEN' && <AlertCircle className="w-3.5 h-3.5" />}
      {status === 'UNDER_REVIEW' && <Clock className="w-3.5 h-3.5" />}
      <span>{status?.replace('_', ' ') || 'OPEN'}</span>
    </span>
  );
}

function ResolveModal({ complaint, onClose, onSubmit, submitting }) {
  const [status, setStatus] = useState(complaint.status === 'OPEN' ? 'UNDER_REVIEW' : 'RESOLVED');
  const [note, setNote] = useState(complaint.resolutionNote || '');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="w-full max-w-lg rounded-3xl bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-lg font-black text-gray-900">Update Complaint #{complaint.id}</h3>
            <p className="mt-0.5 text-xs text-gray-500">
              Filed by {complaint.farmerName || `Farmer #${complaint.farmerId}`}
            </p>
          </div>
          <button type="button" onClick={onClose} className="text-gray-400 hover:text-gray-700">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="mt-4 rounded-2xl bg-gray-50 border border-gray-200 p-3.5 text-sm text-gray-700">
          {complaint.description}
        </div>

        <div className="mt-5">
          <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-gray-500">
            Status
          </label>
          <div className="flex flex-wrap gap-2">
            {['OPEN', 'UNDER_REVIEW', 'RESOLVED', 'CLOSED'].map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => setStatus(s)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
                  status === s
                    ? 'bg-emerald-700 text-white border-emerald-700'
                    : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
                }`}
              >
                {s.replace('_', ' ')}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-4">
          <label className="mb-1.5 block text-xs font-bold uppercase tracking-wide text-gray-500">
            Resolution Note (visible to the farmer)
          </label>
          <textarea
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={4}
            placeholder="Explain what was done, or the next step..."
            className="w-full rounded-2xl border border-gray-300 p-3 text-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
          />
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-100"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={submitting}
            onClick={() => onSubmit(status, note)}
            className="px-5 py-2 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-sm font-bold shadow-sm transition-colors disabled:opacity-50"
          >
            {submitting ? 'Saving...' : 'Save Update'}
          </button>
        </div>
      </div>
    </div>
  );
}

function ManageComplaints() {
  const [complaints, setComplaints] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successToast, setSuccessToast] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [activeComplaint, setActiveComplaint] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchComplaints = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await complaintService.getAllComplaints();
      setComplaints(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load complaints for admin:', err);
      setError(err.message || 'Failed to fetch complaints');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchComplaints();
  }, [fetchComplaints]);

  const handleUpdateStatus = async (status, resolutionNote) => {
    setSubmitting(true);
    setError(null);
    try {
      await complaintService.updateComplaintStatus(activeComplaint.id, status, resolutionNote);
      setSuccessToast(`✓ Complaint #${activeComplaint.id} updated to ${status.replace('_', ' ')}.`);
      setActiveComplaint(null);
      await fetchComplaints();
    } catch (err) {
      setError(err.message || `Failed to update complaint #${activeComplaint.id}`);
    } finally {
      setSubmitting(false);
    }
  };

  const filteredComplaints = complaints.filter((c) => {
    const matchesStatus = statusFilter === 'ALL' || c.status === statusFilter;
    const query = searchQuery.toLowerCase().trim();
    const matchesSearch =
      !query ||
      (c.farmerName && c.farmerName.toLowerCase().includes(query)) ||
      (c.description && c.description.toLowerCase().includes(query)) ||
      (c.category && c.category.toLowerCase().includes(query)) ||
      String(c.id).includes(query);

    return matchesStatus && matchesSearch;
  });

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 space-y-6 font-sans">

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-black text-green-900 tracking-tight">
              Complaint & Dispute Resolution
            </h1>
            <span className="px-2.5 py-0.5 bg-emerald-700 text-white rounded-md text-[10px] font-black uppercase">
              Admin Console
            </span>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Review farmer-filed complaints and update their resolution status.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchComplaints}
          className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition-colors shrink-0"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh List</span>
        </button>
      </div>

      {/* Success Toast */}
      {successToast && (
        <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-900 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button type="button" onClick={() => setSuccessToast('')} className="text-emerald-700 font-bold ml-2">✕</button>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchComplaints} className="text-xs font-bold text-red-800 underline">
            Retry
          </button>
        </div>
      )}

      {/* Filters & Search */}
      <div className="bg-white rounded-2xl p-4 border border-gray-200 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by farmer, category, description..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-emerald-500 focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto text-xs">
          {STATUS_OPTIONS.map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-3.5 py-1.5 rounded-xl font-bold transition-all shrink-0 ${
                statusFilter === st
                  ? 'bg-green-800 text-white shadow-xs'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              }`}
            >
              {st.replace('_', ' ')} ({st === 'ALL' ? complaints.length : complaints.filter((c) => c.status === st).length})
            </button>
          ))}
        </div>
      </div>

      {/* Complaints Table */}
      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((idx) => (
            <div key={idx} className="h-16 bg-gray-200 rounded-2xl" />
          ))}
        </div>
      ) : filteredComplaints.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-3 shadow-xs">
          <ShieldCheck className="w-12 h-12 text-gray-300 mx-auto" />
          <h3 className="text-base font-bold text-gray-800">No Complaints Found</h3>
          <p className="text-xs text-gray-500 max-w-sm mx-auto">
            {searchQuery || statusFilter !== 'ALL'
              ? 'No complaints match the selected search or filter criteria.'
              : 'No complaints have been filed yet.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-gray-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-700">
              <thead className="bg-gray-50 text-[11px] font-black text-gray-500 uppercase tracking-wider border-b border-gray-200">
                <tr>
                  <th className="px-6 py-4">ID</th>
                  <th className="px-6 py-4">Farmer</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Description</th>
                  <th className="px-6 py-4">Filed On</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 font-medium">
                {filteredComplaints.map((c) => (
                  <tr key={c.id} className="hover:bg-gray-50/80 transition-colors align-top">
                    <td className="px-6 py-4 text-xs font-mono font-bold text-gray-500">
                      #{c.id}
                      {c.bookingId && (
                        <div className="mt-0.5 text-[10px] font-normal text-gray-400">Booking #{c.bookingId}</div>
                      )}
                    </td>

                    <td className="px-6 py-4">
                      <div className="font-extrabold text-gray-900 flex items-center gap-1.5">
                        <User className="w-3.5 h-3.5 text-emerald-700 shrink-0" />
                        <span>{c.farmerName || `Farmer #${c.farmerId}`}</span>
                      </div>
                      {c.farmerMobile && (
                        <div className="text-[11px] text-gray-400 flex items-center gap-1 mt-0.5">
                          <Phone className="w-3 h-3 shrink-0" />
                          <span>+91 {c.farmerMobile}</span>
                        </div>
                      )}
                    </td>

                    <td className="px-6 py-4">
                      <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-800">
                        <MessageSquareWarning className="w-3.5 h-3.5 shrink-0" />
                        {CATEGORY_LABELS[c.category] || c.category}
                      </span>
                    </td>

                    <td className="px-6 py-4 text-xs text-gray-600 max-w-xs">
                      <p className="line-clamp-2">{c.description}</p>
                      {c.resolutionNote && (
                        <p className="mt-1 text-[11px] text-emerald-700 italic line-clamp-1">
                          ↳ {c.resolutionNote}
                        </p>
                      )}
                    </td>

                    <td className="px-6 py-4 text-[11px] text-gray-500">
                      <span className="inline-flex items-center gap-1">
                        <Calendar className="w-3 h-3 shrink-0" />
                        {c.createdAt ? new Date(c.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'}
                      </span>
                    </td>

                    <td className="px-6 py-4">
                      <StatusBadge status={c.status} />
                    </td>

                    <td className="px-6 py-4 text-right">
                      <button
                        type="button"
                        onClick={() => setActiveComplaint(c)}
                        className="px-3 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-2xs transition-colors"
                      >
                        {c.status === 'RESOLVED' || c.status === 'CLOSED' ? 'View' : 'Resolve'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {activeComplaint && (
        <ResolveModal
          complaint={activeComplaint}
          onClose={() => setActiveComplaint(null)}
          onSubmit={handleUpdateStatus}
          submitting={submitting}
        />
      )}
    </div>
  );
}

export default ManageComplaints;
