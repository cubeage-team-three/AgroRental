import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  Plus,
  Send,
  MessageSquare,
  RefreshCw,
  FileText,
  AlertTriangle,
  HelpCircle,
  Filter,
} from 'lucide-react';
import { complaintService } from '../../services/complaintService';
import { bookingService } from '../../services/bookingService';

function FarmerComplaints() {
  const [searchParams] = useSearchParams();
  const preselectedBookingId = searchParams.get('bookingId');

  const [complaints, setComplaints] = useState([]);
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showModal, setShowModal] = useState(!!preselectedBookingId);
  const [error, setError] = useState(null);
  const [successMsg, setSuccessMsg] = useState('');

  // Form State
  const [category, setCategory] = useState('EQUIPMENT_ISSUE');
  const [bookingId, setBookingId] = useState(preselectedBookingId || '');
  const [description, setDescription] = useState('');

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [complaintList, bookingList] = await Promise.all([
        complaintService.getFarmerComplaints(),
        bookingService.getFarmerBookings().catch(() => [])
      ]);
      setComplaints(complaintList || []);
      setBookings(bookingList || []);
    } catch (err) {
      console.error('Failed to load complaints:', err);
      setError('Could not fetch complaint history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!description.trim()) {
      setError('Please provide a description of the issue.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const newComplaint = await complaintService.createComplaint({
        bookingId: bookingId ? Number(bookingId) : null,
        category,
        description: description.trim()
      });

      setComplaints([newComplaint, ...complaints]);
      setSuccessMsg('Complaint submitted successfully! Our support team will investigate.');
      setShowModal(false);
      setDescription('');
      setBookingId('');
    } catch (err) {
      console.error('Failed to submit complaint:', err);
      setError(err.message || 'Failed to submit complaint. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'OPEN':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-black bg-amber-100 text-amber-800 border border-amber-300">
            <Clock className="w-3 h-3" /> OPEN
          </span>
        );
      case 'UNDER_REVIEW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-black bg-blue-100 text-blue-800 border border-blue-300">
            <RefreshCw className="w-3 h-3 animate-spin" /> UNDER REVIEW
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-black bg-emerald-100 text-emerald-800 border border-emerald-300">
            <CheckCircle2 className="w-3 h-3" /> RESOLVED
          </span>
        );
      case 'CLOSED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-black bg-gray-100 text-gray-700 border border-gray-300">
            CLOSED
          </span>
        );
      default:
        return <span className="text-xs font-bold text-gray-500">{status}</span>;
    }
  };

  const formatCategory = (cat) => {
    switch (cat) {
      case 'LATE_ARRIVAL': return '🕒 Late Arrival of Machinery/Operator';
      case 'EQUIPMENT_ISSUE': return '🚜 Equipment Breakdown or Damage';
      case 'PAYMENT_ISSUE': return '💳 Payment or Billing Discrepancy';
      case 'OPERATOR_ISSUE': return '👨‍🌾 Operator Conduct or Skill Issue';
      default: return '❓ General Support Query';
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      
      {/* Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Farmer Complaint Management
          </h1>
          <p className="text-xs text-gray-500 font-medium">
            Report machinery issues, operator disputes, or billing concerns for fast resolution
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-extrabold rounded-xl shadow-md transition"
        >
          <Plus className="w-4 h-4" /> Log New Complaint
        </button>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border-l-4 border-emerald-500 rounded-2xl text-emerald-800 text-xs font-bold flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg('')} className="text-emerald-700 font-black hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-xs font-bold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Complaint Form Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 space-y-6 shadow-2xl border border-gray-100 animate-in fade-in zoom-in duration-200">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-lg font-black text-gray-900 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-500" /> Log Service Ticket
              </h3>
              <button
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 text-lg font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              
              <div className="space-y-1.5">
                <label className="block text-xs font-extrabold text-gray-700">
                  Select Complaint Category
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 p-3 text-xs text-gray-900 font-semibold focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100 outline-none"
                >
                  <option value="EQUIPMENT_ISSUE">🚜 Equipment Breakdown or Damage</option>
                  <option value="LATE_ARRIVAL">🕒 Late Arrival of Machinery/Operator</option>
                  <option value="OPERATOR_ISSUE">👨‍🌾 Operator Conduct / Service Quality</option>
                  <option value="PAYMENT_ISSUE">💳 Payment or Billing Discrepancy</option>
                  <option value="OTHER">❓ Other Concern</option>
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-extrabold text-gray-700">
                  Related Booking Reservation (Optional)
                </label>
                <select
                  value={bookingId}
                  onChange={(e) => setBookingId(e.target.value)}
                  className="w-full rounded-2xl border border-gray-200 p-3 text-xs text-gray-900 font-semibold focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100 outline-none"
                >
                  <option value="">-- General Concern (No Specific Booking) --</option>
                  {bookings.map((b) => (
                    <option key={b.id} value={b.id}>
                      Reservation #{b.id} - {b.equipmentName || 'Machinery'} ({b.status})
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-extrabold text-gray-700">
                  Describe Your Issue in Detail
                </label>
                <textarea
                  rows={4}
                  required
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Provide clear details regarding what went wrong during your rental experience..."
                  className="w-full rounded-2xl border border-gray-200 p-3 text-xs text-gray-900 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100 outline-none"
                />
              </div>

              <div className="pt-2 flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-xl transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex-1 py-3 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-extrabold rounded-xl shadow-md transition flex items-center justify-center gap-2 disabled:opacity-70"
                >
                  {submitting ? (
                    <RefreshCw className="w-4 h-4 animate-spin" />
                  ) : (
                    <>
                      <Send className="w-4 h-4" />
                      <span>Submit Ticket</span>
                    </>
                  )}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

      {/* Complaints List */}
      {loading ? (
        <div className="space-y-4 animate-pulse">
          <div className="h-24 bg-gray-200 rounded-3xl" />
          <div className="h-24 bg-gray-200 rounded-3xl" />
        </div>
      ) : complaints.length === 0 ? (
        <div className="bg-white border border-gray-100 rounded-3xl p-12 text-center space-y-4 shadow-sm">
          <MessageSquare className="w-16 h-16 text-emerald-600/30 mx-auto" />
          <div className="space-y-1">
            <h3 className="text-lg font-black text-gray-900">No Complaints Registered</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto">
              You currently have no active or historical support tickets. If you encounter any issue with a rental, submit a ticket above.
            </p>
          </div>
          <button
            onClick={() => setShowModal(true)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-700 text-white text-xs font-bold rounded-xl hover:bg-emerald-800"
          >
            <Plus className="w-4 h-4" /> File a Complaint
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          {complaints.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm space-y-4 hover:border-emerald-200 transition"
            >
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 pb-3">
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-black text-gray-900">
                      Ticket #{item.id}
                    </span>
                    {item.bookingId && (
                      <span className="text-[10px] font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded-md border border-slate-200">
                        Booking #{item.bookingId}
                      </span>
                    )}
                  </div>
                  <p className="text-xs font-extrabold text-emerald-800">
                    {formatCategory(item.category)}
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  {getStatusBadge(item.status)}
                  <span className="text-[11px] font-medium text-gray-400">
                    {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Recent'}
                  </span>
                </div>
              </div>

              <p className="text-xs text-gray-700 leading-relaxed font-medium">
                "{item.description}"
              </p>

              {item.resolutionNote && (
                <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-1 text-xs">
                  <span className="font-extrabold text-slate-800 flex items-center gap-1 text-[11px] uppercase tracking-wider">
                    <HelpCircle className="w-3.5 h-3.5 text-emerald-600" /> Support Team Response
                  </span>
                  <p className="text-slate-600 font-medium">
                    {item.resolutionNote}
                  </p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

    </div>
  );
}

export default FarmerComplaints;
