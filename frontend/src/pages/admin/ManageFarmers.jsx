import { Loader, Mail, MapPin, Phone, RotateCw, Search, User } from 'lucide-react';
import { useEffect, useState, useRef } from 'react';
import { apiClient } from '../../services/apiClient';

export default function ManageFarmers() {
  const [farmers, setFarmers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [approvingId, setApprovingId] = useState(null);
  const [rejectingId, setRejectingId] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const activeRequestId = useRef(0);

  const [totalElements, setTotalElements] = useState(0);

  const fetchFarmers = async (page = 0, search = '', status = activeTab) => {
    const currentRequestId = ++activeRequestId.current;
    setLoading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const params = { page, size: 10 };
      if (search.trim()) params.search = search.trim();
      if (status && status !== 'ALL') params.status = status;

      const response = await apiClient.get('/admin/farmers', { params });
      
      if (currentRequestId === activeRequestId.current) {
        if (response && response.content) {
          setFarmers(response.content || []);
          setTotalPages(response.totalPages || 0);
          setTotalElements(response.totalElements || 0);
          setCurrentPage(page);
        }
      }
    } catch (err) {
      if (currentRequestId === activeRequestId.current) {
        setError(err.message || 'Failed to fetch farmers');
        console.error('Error fetching farmers:', err);
      }
    } finally {
      if (currentRequestId === activeRequestId.current) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    fetchFarmers(0, debouncedSearchTerm, activeTab);
  }, [debouncedSearchTerm, activeTab]);

  const handleApproveKyc = async (farmerId) => {
    setApprovingId(farmerId);
    try {
      const response = await apiClient.put(`/admin/farmers/${farmerId}/approve-kyc`);
      if (response.data && response.data.success) {
        setFarmers(farmers.map(farmer =>
          farmer.farmerId === farmerId ? { ...farmer, accountStatus: 'ACTIVE' } : farmer
        ));
      } else {
        setError('Failed to approve farmer KYC');
      }
    } catch (err) {
      setError(err.message || 'Failed to approve farmer KYC');
    } finally {
      setApprovingId(null);
    }
  };

  const handleRejectKyc = async (farmer) => {
    if (!window.confirm(`Are you sure you want to reject KYC for ${farmer.fullName}? This action cannot be undone.`)) {
      return;
    }
    
    setRejectingId(farmer.farmerId);
    setError(null);
    setSuccessMessage(null);
    
    try {
      const response = await apiClient.put(`/admin/farmers/${farmer.farmerId}/reject-kyc`);
      if (response.data && response.data.success) {
        let targetPage = currentPage;
        if (farmers.length === 1 && currentPage > 0) {
          targetPage = currentPage - 1;
        }
        await fetchFarmers(targetPage, searchTerm, activeTab);
        setSuccessMessage(`Successfully rejected KYC for ${farmer.fullName}`);
      } else {
        setError('Failed to reject farmer KYC');
      }
    } catch (err) {
      setError(err.message || 'Failed to reject farmer KYC');
    } finally {
      setRejectingId(null);
    }
  };

  // Exact styling from the Partner page badges
  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-green-200 bg-green-50 text-[11px] font-bold text-green-600 tracking-wide">○ ACTIVE</span>;
      case 'KYC_COMPLETED':
        return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-blue-200 bg-blue-50 text-[11px] font-bold text-blue-600 tracking-wide">○ KYC REVIEW</span>;
      default:
        return <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-gray-200 bg-gray-50 text-[11px] font-bold text-gray-500 tracking-wide">○ PENDING</span>;
    }
  };

  const tabs = [
    { label: 'ALL' },
    { label: 'PENDING' },
    { label: 'KYC REVIEW' },
    { label: 'ACTIVE' }
  ];

  const visibleFarmers = farmers;

  return (
    <div className="max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 space-y-6 font-sans">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2.5 mb-1">
            <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">Farmer Management & Verification</h1>
            <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] px-2.5 py-0.5 rounded-full font-extrabold tracking-wider uppercase">ADMIN CONSOLE</span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 font-medium">Review farmer identities, farm registrations, approve or reject KYC verifications.</p>
        </div>
        <button 
          onClick={() => fetchFarmers(currentPage, searchTerm)}
          className="flex items-center gap-2 bg-white text-slate-700 px-4 py-2.5 rounded-xl text-xs font-bold hover:bg-slate-50 transition border border-slate-200 shadow-xs cursor-pointer self-start sm:self-auto"
        >
          <RotateCw size={14} className={`text-slate-500 ${loading ? 'animate-spin' : ''}`} />
          Refresh List
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl text-xs font-bold shadow-xs">
          {error}
        </div>
      )}

      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl text-xs font-bold shadow-xs">
          {successMessage}
        </div>
      )}

      {/* Unified White Card for Search and Table */}
      <div className="bg-white rounded-3xl shadow-sm border border-slate-200 overflow-hidden">
        
        {/* Top Filter Bar */}
        <div className="p-4 sm:p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100">
          <div className="relative w-full md:w-[380px]">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-slate-400" strokeWidth={2} />
            <input 
              type="text" 
              placeholder="Search by name, phone, email..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 placeholder:text-slate-400 focus:bg-white focus:outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500 transition-all"
            />
          </div>
          
          <div className="flex flex-wrap gap-2 text-xs font-bold">
            {tabs.map((tab) => (
              <button 
                key={tab.label}
                onClick={() => setActiveTab(tab.label)}
                className={`px-4 py-2 rounded-xl transition-all cursor-pointer ${
                  activeTab === tab.label 
                    ? 'bg-emerald-700 text-white shadow-xs' 
                    : 'text-slate-600 bg-slate-100 hover:bg-slate-200'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Data Table */}
        <div className="overflow-x-auto min-h-[400px]">
          {loading ? (
            <div className="flex flex-col items-center justify-center h-64">
              <Loader className="animate-spin text-emerald-600 mb-3" size={28} />
              <span className="text-slate-400 text-xs font-bold">Loading farmers...</span>
            </div>
          ) : visibleFarmers.length === 0 ? (
             <div className="flex items-center justify-center h-64 text-slate-400 text-xs font-medium">
               No farmers found matching this criteria.
             </div>
          ) : (
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="py-3.5 px-6">FARMER ID</th>
                  <th className="py-3.5 px-6">FULL NAME / ADDRESS</th>
                  <th className="py-3.5 px-6">CONTACT DETAILS</th>
                  <th className="py-3.5 px-6 text-center">ACCOUNT STATUS</th>
                  <th className="py-3.5 px-6 text-center">KYC ACTIONS</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visibleFarmers.map((farmer) => (
                  <tr key={farmer.farmerId} className="hover:bg-slate-50/80 transition-colors group">
                    <td className="py-4 px-6 text-xs font-mono font-bold text-slate-400">
                      #{farmer.farmerId}
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2 mb-1">
                        <User size={14} className="text-slate-400" strokeWidth={2.5} />
                        <span className="text-xs font-bold text-slate-900">{farmer.fullName}</span>
                      </div>
                      <div className="flex items-start gap-1.5 ml-5 text-[11px] font-medium text-slate-500">
                        <MapPin size={12} className="shrink-0 mt-0.5 text-slate-400" strokeWidth={2.5} />
                        <span>{farmer.address || "Address not provided"}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2 mb-1 text-xs font-semibold text-slate-700">
                        <Phone size={13} className="text-slate-400" strokeWidth={2.5} />
                        <span>{farmer.mobileNumber}</span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px] font-medium text-slate-500">
                        <Mail size={13} className="text-slate-400" strokeWidth={2.5} />
                        <span>{farmer.email || "No email"}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-center">
                      {getStatusBadge(farmer.accountStatus)}
                    </td>
                    <td className="py-4 px-6 text-center">
                      {farmer.accountStatus === 'KYC_COMPLETED' ? (
                        <div className="flex gap-3 justify-center">
                          <button 
                            onClick={() => handleApproveKyc(farmer.farmerId)}
                            disabled={approvingId === farmer.farmerId}
                            className="px-3 py-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200 rounded-xl text-[11px] font-bold disabled:opacity-50 flex items-center gap-1 cursor-pointer transition-colors"
                          >
                            {approvingId === farmer.farmerId ? <Loader size={12} className="animate-spin" /> : null}
                            Approve
                          </button>
                          <button 
                            onClick={() => handleRejectKyc(farmer)}
                            disabled={rejectingId === farmer.farmerId}
                            className="px-3 py-1 bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 rounded-xl text-[11px] font-bold disabled:opacity-50 flex items-center gap-1 cursor-pointer transition-colors"
                          >
                            {rejectingId === farmer.farmerId ? <Loader size={12} className="animate-spin" /> : null}
                            Reject
                          </button>
                        </div>
                      ) : (
                        <span className="text-slate-300 font-bold">-</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Pagination Footer */}
        {!loading && totalPages > 1 && (
          <div className="border-t border-slate-100 p-4 sm:p-5 flex items-center justify-between bg-white">
            <span className="text-xs font-medium text-slate-400">
              Showing page {currentPage + 1} of {totalPages}
            </span>
            <div className="flex gap-1.5">
              <button
                onClick={() => fetchFarmers(Math.max(0, currentPage - 1), searchTerm)}
                disabled={currentPage === 0}
                className="px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-wider border border-slate-200 rounded-xl text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition-colors cursor-pointer"
              >
                Prev
              </button>
              {Array.from({ length: totalPages }, (_, i) => (
                <button
                  key={i}
                  onClick={() => fetchFarmers(i, searchTerm)}
                  className={`px-3.5 py-1.5 text-[11px] font-bold rounded-xl transition-colors cursor-pointer ${
                    currentPage === i
                      ? 'bg-emerald-700 text-white shadow-xs'
                      : 'text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => fetchFarmers(Math.min(totalPages - 1, currentPage + 1), searchTerm)}
                disabled={currentPage === totalPages - 1}
                className="px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-wider border border-slate-200 rounded-xl text-slate-600 hover:bg-slate-50 disabled:opacity-40 transition-colors cursor-pointer"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}