import { useState, useEffect, useCallback } from 'react';
import { partnerService } from '../../services/partnerService';
import {
  ShieldCheck,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Clock,
  RefreshCw,
  Search,
  Building2,
  Phone,
  Mail,
  User,
} from 'lucide-react';

function ManagePartners() {
  const [partners, setPartners] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successToast, setSuccessToast] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [actionLoading, setActionLoading] = useState(null);

  const fetchPartners = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await partnerService.getAllPartners();
      setPartners(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load partners for admin:', err);
      setError(err.message || 'Failed to fetch partner accounts');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPartners();
  }, [fetchPartners]);

  const handleApproveKyc = async (partner) => {
    setActionLoading(partner.id);
    setSuccessToast('');
    setError(null);
    try {
      await partnerService.approveKyc(partner.id);
      setSuccessToast(`✓ Partner #${partner.id} (${partner.fullName}) KYC approved successfully!`);
      await fetchPartners();
    } catch (err) {
      setError(err.message || `Failed to approve KYC for Partner #${partner.id}`);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRejectKyc = async (partner) => {
    const reason = window.prompt(`Enter rejection reason for Partner #${partner.id} (${partner.fullName}):`);
    if (reason === null) return; // Cancelled prompt

    setActionLoading(partner.id);
    setSuccessToast('');
    setError(null);
    try {
      await partnerService.rejectKyc(partner.id);
      setSuccessToast(`✓ Partner #${partner.id} (${partner.fullName}) KYC rejected.`);
      await fetchPartners();
    } catch (err) {
      setError(err.message || `Failed to reject KYC for Partner #${partner.id}`);
    } finally {
      setActionLoading(null);
    }
  };

  const filteredPartners = partners.filter((p) => {
    const matchesStatus = statusFilter === 'ALL' || p.verificationStatus === statusFilter;
    const query = searchQuery.toLowerCase().trim();
    const matchesSearch =
      !query ||
      (p.fullName && p.fullName.toLowerCase().includes(query)) ||
      (p.businessName && p.businessName.toLowerCase().includes(query)) ||
      (p.email && p.email.toLowerCase().includes(query)) ||
      (p.mobileNumber && p.mobileNumber.includes(query)) ||
      String(p.id).includes(query);

    return matchesStatus && matchesSearch;
  });

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              Partner KYC & Account Verification
            </h1>
            <span className="px-2.5 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-full text-[10px] font-extrabold uppercase">
              Admin Console
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 font-medium mt-1">
            Review partner identities, business registrations, approve or reject KYC verifications.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchPartners}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-bold rounded-xl transition shadow-xs shrink-0 cursor-pointer self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh List</span>
        </button>
      </div>

      {/* Success Toast */}
      {successToast && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-900 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successToast}</span>
          </div>
          <button type="button" onClick={() => setSuccessToast('')} className="text-emerald-700 hover:text-emerald-900 font-bold ml-2 cursor-pointer">✕</button>
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-2xl text-red-700 text-xs font-bold flex items-center justify-between shadow-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
            <span>{error}</span>
          </div>
          <button type="button" onClick={fetchPartners} className="text-xs font-bold text-red-800 underline cursor-pointer">
            Retry
          </button>
        </div>
      )}

      {/* Filters & Search */}
      <div className="bg-white rounded-3xl p-4 sm:p-6 border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search by name, business, email, phone..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 placeholder:text-slate-400 focus:bg-white focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 focus:outline-none"
          />
        </div>

        <div className="flex items-center gap-2 w-full md:w-auto overflow-x-auto text-xs">
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setStatusFilter(st)}
              className={`px-4 py-2 rounded-xl font-bold transition-all shrink-0 cursor-pointer ${
                statusFilter === st
                  ? 'bg-emerald-700 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {st} ({st === 'ALL' ? partners.length : partners.filter((p) => p.verificationStatus === st).length})
            </button>
          ))}
        </div>
      </div>

      {/* Partners Table */}
      {loading ? (
        <div className="space-y-3 animate-pulse">
          {[1, 2, 3, 4].map((idx) => (
            <div key={idx} className="h-16 bg-slate-100 rounded-2xl border border-slate-200" />
          ))}
        </div>
      ) : filteredPartners.length === 0 ? (
        <div className="bg-white border border-slate-200 rounded-3xl p-12 text-center space-y-3 shadow-xs">
          <ShieldCheck className="w-12 h-12 text-slate-300 mx-auto" />
          <h3 className="text-sm font-bold text-slate-800">No Partners Found</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            {searchQuery || statusFilter !== 'ALL'
              ? 'No partner accounts match the selected search or filter criteria.'
              : 'There are currently no registered partners in the system.'}
          </p>
        </div>
      ) : (
        <div className="bg-white rounded-3xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700">
              <thead className="bg-slate-50 text-[10px] font-bold text-slate-400 uppercase tracking-wider border-b border-slate-100">
                <tr>
                  <th className="px-6 py-4">Partner ID</th>
                  <th className="px-6 py-4">Full Name / Business</th>
                  <th className="px-6 py-4">Contact Details</th>
                  <th className="px-6 py-4">Verification Status</th>
                  <th className="px-6 py-4">Active Status</th>
                  <th className="px-6 py-4 text-right">KYC Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-medium">
                {filteredPartners.map((p) => {
                  const isProcessing = actionLoading === p.id;

                  return (
                    <tr key={p.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="px-6 py-4 text-xs font-mono font-bold text-gray-500">
                        #{p.id}
                      </td>

                      <td className="px-6 py-4">
                        <div className="font-extrabold text-gray-900 flex items-center gap-1.5">
                          <User className="w-4 h-4 text-emerald-700 shrink-0" />
                          <span>{p.fullName}</span>
                        </div>
                        {p.businessName && (
                          <div className="text-xs text-emerald-800 font-semibold flex items-center gap-1 mt-0.5">
                            <Building2 className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                            <span>{p.businessName}</span>
                          </div>
                        )}
                        {p.address && (
                          <div className="text-[11px] text-gray-400 truncate max-w-xs mt-0.5">
                            {p.address}
                          </div>
                        )}
                      </td>

                      <td className="px-6 py-4 text-xs space-y-0.5">
                        <div className="flex items-center gap-1.5 text-gray-800 font-semibold">
                          <Phone className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                          <span>+91 {p.mobileNumber}</span>
                        </div>
                        {p.email && (
                          <div className="flex items-center gap-1.5 text-gray-500">
                            <Mail className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                            <span>{p.email}</span>
                          </div>
                        )}
                      </td>

                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-black uppercase tracking-wide border shadow-2xs ${
                            p.verificationStatus === 'APPROVED'
                              ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                              : p.verificationStatus === 'REJECTED'
                              ? 'bg-red-100 text-red-800 border-red-300'
                              : 'bg-amber-100 text-amber-800 border-amber-300'
                          }`}
                        >
                          {p.verificationStatus === 'APPROVED' && <CheckCircle2 className="w-3.5 h-3.5" />}
                          {p.verificationStatus === 'REJECTED' && <XCircle className="w-3.5 h-3.5" />}
                          {p.verificationStatus === 'PENDING' && <Clock className="w-3.5 h-3.5" />}
                          <span>{p.verificationStatus || 'PENDING'}</span>
                        </span>
                      </td>

                      <td className="px-6 py-4">
                        <span
                          className={`text-[11px] font-bold px-2 py-0.5 rounded ${
                            p.verificationStatus === 'APPROVED'
                              ? 'bg-green-50 text-green-700 border border-green-200'
                              : 'bg-gray-100 text-gray-500 border border-gray-200'
                          }`}
                        >
                          {p.verificationStatus === 'APPROVED' ? 'Active' : 'Inactive'}
                        </span>
                      </td>

                      <td className="px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {p.verificationStatus !== 'APPROVED' && (
                            <button
                              type="button"
                              disabled={isProcessing}
                              onClick={() => handleApproveKyc(p)}
                              className="px-3.5 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-xs transition-colors disabled:opacity-50 cursor-pointer"
                            >
                              {isProcessing ? 'Updating...' : 'Approve'}
                            </button>
                          )}

                          {p.verificationStatus !== 'REJECTED' && (
                            <button
                              type="button"
                              disabled={isProcessing}
                              onClick={() => handleRejectKyc(p)}
                              className="px-3.5 py-1.5 bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 text-xs font-bold rounded-xl transition-colors disabled:opacity-50 cursor-pointer"
                            >
                              {isProcessing ? 'Updating...' : 'Reject'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}

export default ManagePartners;
