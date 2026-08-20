import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HardHat,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldCheck,
  FileText,
  User,
  Phone,
  Mail,
  MapPin,
  Award,
  AlertCircle,
  RefreshCw,
  Eye,
  ChevronRight,
  Loader2,
  Check,
  X,
} from 'lucide-react';
import { adminService } from '../../services/adminService';

function ManageOperators() {
  const [operators, setOperators] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState('');

  // Filtering & Pagination
  const [activeTab, setActiveTab] = useState('PENDING'); // ALL, PENDING, APPROVED, REJECTED
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Review & Verification Modal
  const [selectedOperatorId, setSelectedOperatorId] = useState(null);
  const [operatorDetail, setOperatorDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // Rejection Dialog inside Modal
  const [showRejectBox, setShowRejectBox] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  useEffect(() => {
    loadOperators();
  }, [activeTab, page]);

  const loadOperators = async () => {
    setLoading(true);
    setError(null);

    try {
      const params = {
        page,
        size: 10,
      };
      if (activeTab !== 'ALL') {
        params.status = activeTab;
      }

      const response = await adminService.getOperators(params);
      if (response && response.content) {
        setOperators(response.content);
        setTotalPages(response.totalPages || 1);
        setTotalElements(response.totalElements || 0);
      } else if (Array.isArray(response)) {
        setOperators(response);
        setTotalPages(1);
        setTotalElements(response.length);
      } else {
        setOperators([]);
      }
    } catch (err) {
      console.error('Failed to load operators:', err);
      setError(err.message || 'Failed to retrieve operators list.');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenReview = async (id) => {
    setSelectedOperatorId(id);
    setDetailLoading(true);
    setShowRejectBox(false);
    setRejectionReason('');

    try {
      const detail = await adminService.getOperatorById(id);
      setOperatorDetail(detail);
    } catch (err) {
      console.error('Failed to load operator detail:', err);
      alert('Failed to load operator verification profile.');
      setSelectedOperatorId(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleCloseReview = () => {
    setSelectedOperatorId(null);
    setOperatorDetail(null);
    setShowRejectBox(false);
    setRejectionReason('');
  };

  const handleVerify = async (status) => {
    if (!selectedOperatorId) return;

    if (status === 'APPROVED' && operatorDetail && !operatorDetail.mobileVerified) {
      alert('Cannot approve operator: Mobile number must be verified first.');
      return;
    }

    if (status === 'REJECTED' && !rejectionReason.trim()) {
      alert('Please specify a reason for rejecting the operator application.');
      return;
    }

    setActionLoading(true);

    try {
      await adminService.verifyOperator(selectedOperatorId, {
        status,
        rejectionReason: status === 'REJECTED' ? rejectionReason.trim() : null,
      });

      setSuccessMessage(
        status === 'APPROVED'
          ? `Operator #${selectedOperatorId} has been APPROVED successfully!`
          : `Operator #${selectedOperatorId} application has been REJECTED.`
      );

      handleCloseReview();
      loadOperators();
    } catch (err) {
      console.error('Verification update error:', err);
      alert(err.message || 'Failed to update operator status.');
    } finally {
      setActionLoading(false);
    }
  };

  const filteredOperators = operators.filter((op) => {
    if (!searchQuery.trim()) return true;
    const query = searchQuery.toLowerCase();
    return (
      op.fullName?.toLowerCase().includes(query) ||
      op.mobileNumber?.includes(query) ||
      op.email?.toLowerCase().includes(query) ||
      op.skills?.toLowerCase().includes(query)
    );
  });

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 space-y-6 font-sans">

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div className="space-y-1">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-[#142E1C] text-[#C1FF72] shadow-xs">
              <HardHat className="w-6 h-6" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
              Operator Verification & Safety
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-gray-500 font-medium">
            Review equipment operating credentials, government KYC documentation, and approve verified operators.
          </p>
        </div>

        <button
          type="button"
          onClick={loadOperators}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs font-bold text-gray-700 hover:bg-gray-50 transition shadow-xs self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Data</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMessage && (
        <motion.div
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 bg-emerald-50 border border-emerald-300 rounded-2xl text-emerald-950 text-xs font-bold flex items-center justify-between shadow-xs"
        >
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            type="button"
            onClick={() => setSuccessMessage('')}
            className="text-emerald-700 hover:text-emerald-950 font-black text-sm"
          >
            ✕
          </button>
        </motion.div>
      )}

      {/* Filters and Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-6 border border-gray-200/70 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Status Tabs */}
        <div className="flex flex-wrap gap-2">
          {[
            { id: 'PENDING', label: 'Pending Review', icon: Clock, color: 'text-amber-700' },
            { id: 'APPROVED', label: 'Approved Operators', icon: CheckCircle2, color: 'text-emerald-700' },
            { id: 'REJECTED', label: 'Rejected', icon: XCircle, color: 'text-red-700' },
            { id: 'ALL', label: 'All Operators', icon: Filter, color: 'text-gray-700' },
          ].map((tab) => {
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => {
                  setActiveTab(tab.id);
                  setPage(0);
                }}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                  active
                    ? 'bg-[#142E1C] text-white shadow-xs'
                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                }`}
              >
                <tab.icon className={`w-3.5 h-3.5 ${active ? 'text-[#C1FF72]' : tab.color}`} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Search Input */}
        <div className="relative min-w-[240px]">
          <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-3" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by name, mobile, skills..."
            className="w-full pl-10 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-900 focus:bg-white focus:ring-2 focus:ring-[#3E7B27] focus:outline-none"
          />
        </div>
      </div>

      {/* Operator List Table */}
      <div className="bg-white rounded-3xl border border-gray-200/70 shadow-sm overflow-hidden">
        {loading ? (
          <div className="py-20 text-center space-y-3">
            <Loader2 className="w-8 h-8 text-[#3E7B27] animate-spin mx-auto" />
            <p className="text-xs font-bold text-gray-500">Loading Operator Profiles from Server...</p>
          </div>
        ) : error ? (
          <div className="py-16 text-center space-y-3">
            <AlertCircle className="w-10 h-10 text-red-500 mx-auto" />
            <p className="text-sm font-bold text-gray-800">Failed to load operators</p>
            <p className="text-xs text-gray-500">{error}</p>
            <button
              type="button"
              onClick={loadOperators}
              className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-bold rounded-xl"
            >
              Try Again
            </button>
          </div>
        ) : filteredOperators.length === 0 ? (
          <div className="py-20 text-center space-y-3">
            <HardHat className="w-12 h-12 text-gray-300 mx-auto" />
            <p className="text-sm font-bold text-gray-800">No Operators Found</p>
            <p className="text-xs text-gray-400 max-w-sm mx-auto">
              There are currently no operators matching the selected tab ({activeTab}) or search filter.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-gray-100 bg-[#F9FAF9] text-gray-400 font-bold uppercase tracking-wider text-[10px]">
                  <th className="py-3.5 px-6">Operator</th>
                  <th className="py-3.5 px-4">Contact Details</th>
                  <th className="py-3.5 px-4">Experience & Skills</th>
                  <th className="py-3.5 px-4">Mobile Status</th>
                  <th className="py-3.5 px-4">Account Status</th>
                  <th className="py-3.5 px-6 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredOperators.map((op) => (
                  <tr key={op.id} className="hover:bg-gray-50/80 transition-colors">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-center font-black text-emerald-900 text-xs">
                          {op.fullName?.charAt(0) || 'O'}
                        </div>
                        <div>
                          <span className="font-extrabold text-gray-900 block text-xs">
                            {op.fullName}
                          </span>
                          <span className="text-[10px] text-gray-400 font-mono">
                            ID #{op.id}
                          </span>
                        </div>
                      </div>
                    </td>

                    <td className="py-4 px-4 space-y-0.5">
                      <div className="font-bold text-gray-800 flex items-center gap-1">
                        <Phone className="w-3 h-3 text-gray-400" />
                        <span>+91 {op.mobileNumber}</span>
                      </div>
                      {op.email && (
                        <div className="text-[11px] text-gray-500 flex items-center gap-1">
                          <Mail className="w-3 h-3 text-gray-400" />
                          <span className="truncate max-w-[150px]">{op.email}</span>
                        </div>
                      )}
                    </td>

                    <td className="py-4 px-4 space-y-1">
                      <span className="inline-block font-extrabold text-emerald-900 bg-emerald-50 px-2 py-0.5 rounded-lg text-[10px]">
                        {op.experience || 0} Years Experience
                      </span>
                      <p className="text-[11px] text-gray-600 font-medium truncate max-w-[180px]">
                        {op.skills || 'General Machinery'}
                      </p>
                    </td>

                    <td className="py-4 px-4">
                      {op.mobileVerified ? (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-emerald-100 text-emerald-900 border border-emerald-300 rounded-full font-bold text-[10px]">
                          <Check className="w-3 h-3 text-emerald-700" /> Verified
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-2.5 py-1 bg-amber-100 text-amber-900 border border-amber-300 rounded-full font-bold text-[10px]">
                          <Clock className="w-3 h-3 text-amber-700" /> Unverified
                        </span>
                      )}
                    </td>

                    <td className="py-4 px-4">
                      <span
                        className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-black uppercase border ${
                          op.status === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-900 border-emerald-300'
                            : op.status === 'REJECTED'
                            ? 'bg-red-100 text-red-900 border-red-300'
                            : 'bg-amber-100 text-amber-900 border-amber-300'
                        }`}
                      >
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            op.status === 'APPROVED'
                              ? 'bg-emerald-600'
                              : op.status === 'REJECTED'
                              ? 'bg-red-600'
                              : 'bg-amber-600 animate-pulse'
                          }`}
                        />
                        {op.status}
                      </span>
                    </td>

                    <td className="py-4 px-6 text-right">
                      <button
                        type="button"
                        onClick={() => handleOpenReview(op.id)}
                        className="px-3 py-1.5 bg-[#142E1C] hover:bg-[#0E2013] text-white rounded-xl text-xs font-bold transition shadow-xs inline-flex items-center gap-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Review & Verify</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Review & Verification Modal */}
      <AnimatePresence>
        {selectedOperatorId && (
          <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 10 }}
              className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto border border-gray-200 shadow-2xl space-y-6 p-6 sm:p-8 relative"
            >
              {/* Modal Close Button */}
              <button
                type="button"
                onClick={handleCloseReview}
                className="absolute top-5 right-5 text-gray-400 hover:text-gray-700 p-1.5 rounded-xl hover:bg-gray-100 transition"
              >
                <X className="w-5 h-5" />
              </button>

              {detailLoading || !operatorDetail ? (
                <div className="py-16 text-center space-y-3">
                  <Loader2 className="w-8 h-8 text-[#3E7B27] animate-spin mx-auto" />
                  <p className="text-xs font-bold text-gray-500">Loading full KYC dossier...</p>
                </div>
              ) : (
                <>
                  {/* Modal Header */}
                  <div className="border-b border-gray-100 pb-4 space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-emerald-900 bg-emerald-50 px-2 py-0.5 rounded-lg">
                        #OP-{operatorDetail.id}
                      </span>
                      <h2 className="text-xl font-black text-[#142E1C]">
                        {operatorDetail.fullName}
                      </h2>
                    </div>
                    <p className="text-xs text-gray-500">
                      Registered on {new Date(operatorDetail.createdAt).toLocaleDateString('en-IN', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </p>
                  </div>

                  {/* Operator Credentials Grid */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                    <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60 space-y-1">
                      <span className="text-gray-400 font-bold uppercase text-[10px] block">
                        Contact & Location
                      </span>
                      <p className="font-bold text-gray-900 flex items-center gap-1">
                        <Phone className="w-3.5 h-3.5 text-gray-500" /> +91 {operatorDetail.mobileNumber}
                      </p>
                      <p className="text-gray-600 flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-gray-400" /> {operatorDetail.address || 'Address provided'}
                      </p>
                    </div>

                    <div className="bg-[#FAF8F5] p-3.5 rounded-2xl border border-amber-100/60 space-y-1">
                      <span className="text-gray-400 font-bold uppercase text-[10px] block">
                        Operational Qualifications
                      </span>
                      <p className="font-bold text-emerald-950">
                        {operatorDetail.experience} Years Field Experience
                      </p>
                      <p className="text-gray-600 truncate">
                        Skills: {operatorDetail.skills}
                      </p>
                    </div>
                  </div>

                  {/* Masked Government KYC Section */}
                  <div className="border border-gray-200/80 rounded-2xl p-4 space-y-3 bg-[#FDFBF7]">
                    <div className="flex items-center gap-2">
                      <ShieldCheck className="w-4 h-4 text-[#3E7B27]" />
                      <h3 className="text-xs font-black uppercase text-[#142E1C] tracking-wider">
                        Masked Government KYC Identifiers
                      </h3>
                    </div>

                    <div className="grid grid-cols-2 gap-4 text-xs font-mono">
                      <div>
                        <span className="text-gray-400 font-sans font-bold text-[10px] block">
                          Aadhaar Number
                        </span>
                        <span className="font-extrabold text-gray-900">
                          {operatorDetail.maskedAadhaarNumber || 'XXXX-XXXX-XXXX'}
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-400 font-sans font-bold text-[10px] block">
                          Driving License Number
                        </span>
                        <span className="font-extrabold text-gray-900">
                          {operatorDetail.maskedDrivingLicenseNumber || 'DL-XXXX-XXXX'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Uploaded Documents List */}
                  <div className="space-y-2">
                    <h3 className="text-xs font-black uppercase text-gray-700 tracking-wider">
                      Attached KYC Documents ({operatorDetail.documents?.length || 0})
                    </h3>

                    {operatorDetail.documents && operatorDetail.documents.length > 0 ? (
                      <div className="space-y-2">
                        {operatorDetail.documents.map((doc) => (
                          <div
                            key={doc.id}
                            className="p-3 bg-gray-50 border border-gray-200 rounded-xl flex items-center justify-between text-xs"
                          >
                            <div className="flex items-center gap-2.5">
                              <FileText className="w-4 h-4 text-gray-500" />
                              <div>
                                <span className="font-bold text-gray-900 block">{doc.documentType}</span>
                                <span className="text-[10px] text-gray-400">{doc.fileName}</span>
                              </div>
                            </div>
                            <span className="px-2 py-0.5 bg-gray-200 text-gray-700 text-[10px] font-bold rounded-md">
                              {doc.verificationStatus}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="text-xs text-gray-400 italic bg-gray-50 p-3 rounded-xl">
                        No additional KYC document files attached. Identifiers registered with account.
                      </p>
                    )}
                  </div>

                  {/* Rejection Reason Form (Toggled) */}
                  {showRejectBox && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      className="p-4 bg-red-50 border border-red-200 rounded-2xl space-y-3"
                    >
                      <span className="text-xs font-black text-red-900 block">
                        Reason for Application Rejection:
                      </span>
                      <textarea
                        rows={2}
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="e.g. Unclear driving license copy, invalid phone verification, experience mismatch..."
                        className="w-full p-2.5 bg-white border border-red-200 rounded-xl text-xs font-medium text-gray-900 focus:ring-2 focus:ring-red-400 focus:outline-none"
                      />
                      <div className="flex justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => setShowRejectBox(false)}
                          className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-bold rounded-lg"
                        >
                          Cancel
                        </button>
                        <button
                          type="button"
                          disabled={actionLoading}
                          onClick={() => handleVerify('REJECTED')}
                          className="px-4 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-extrabold rounded-lg shadow"
                        >
                          {actionLoading ? 'Rejecting...' : 'Confirm Reject ✕'}
                        </button>
                      </div>
                    </motion.div>
                  )}

                  {/* Modal Action Controls */}
                  {operatorDetail.status === 'PENDING' && !showRejectBox && (
                    <div className="pt-4 border-t border-gray-100 flex flex-col sm:flex-row gap-3 justify-end">
                      <button
                        type="button"
                        onClick={() => setShowRejectBox(true)}
                        className="px-5 py-2.5 bg-red-50 hover:bg-red-100 text-red-700 text-xs font-extrabold rounded-xl border border-red-200 transition"
                      >
                        ✕ Reject Application
                      </button>

                      <button
                        type="button"
                        disabled={actionLoading || !operatorDetail.mobileVerified}
                        onClick={() => handleVerify('APPROVED')}
                        className="px-6 py-2.5 bg-[#3E7B27] hover:bg-[#2E6F22] text-white text-xs font-black rounded-xl shadow-md transition flex items-center justify-center gap-2 disabled:opacity-50"
                      >
                        {actionLoading ? (
                          <>
                            <Loader2 className="w-4 h-4 animate-spin" />
                            <span>Processing...</span>
                          </>
                        ) : (
                          <>
                            <CheckCircle2 className="w-4 h-4" />
                            <span>Approve Operator Account ✓</span>
                          </>
                        )}
                      </button>
                    </div>
                  )}

                  {operatorDetail.status !== 'PENDING' && (
                    <div className="p-3 bg-gray-50 border border-gray-200 rounded-xl text-center text-xs text-gray-600 font-bold">
                      This application is already finalized as <span className="uppercase text-gray-900 font-black">{operatorDetail.status}</span>.
                      {operatorDetail.rejectionReason && (
                        <span className="block text-red-700 font-normal mt-1">
                          Reason: {operatorDetail.rejectionReason}
                        </span>
                      )}
                    </div>
                  )}
                </>
              )}
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}

export default ManageOperators;
