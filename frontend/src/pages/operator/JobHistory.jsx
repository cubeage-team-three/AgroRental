import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { getAssignedJobs } from "../../services/jobService";

function JobHistory() {
  const [filter, setFilter] = useState("COMPLETED");
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadJobs();
  }, [filter]);

  const loadJobs = async () => {
    setLoading(true);
    try {
      const data = await getAssignedJobs(filter === "ALL" ? null : filter);
      setJobs(Array.isArray(data) ? data : []);
    } catch {
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">📜</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Work Archive
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Job History & Performance Log
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Review all previous field operations, completed machinery duties, and declined or cancelled work logs.
          </p>
        </div>

        <Link
          to="/operator/jobs"
          className="bg-emerald-50 hover:bg-emerald-100 text-emerald-900 font-bold text-xs px-4 py-2.5 rounded-xl border border-emerald-200 transition"
        >
          View Active Jobs →
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="bg-white rounded-2xl p-2 border border-amber-100/70 shadow-sm flex gap-2">
        {["COMPLETED", "REJECTED", "CANCELLED", "ALL"].map((tab) => (
          <button
            key={tab}
            onClick={() => setFilter(tab)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition ${
              filter === tab ? "bg-emerald-800 text-white" : "text-gray-600 hover:bg-gray-100"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? (
        <div className="p-12 text-center bg-white rounded-3xl border border-amber-100/70 shadow-sm">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-emerald-600 border-t-transparent"></div>
        </div>
      ) : jobs.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 text-center border border-amber-100/70 shadow-sm space-y-3">
          <div className="w-14 h-14 bg-emerald-50 text-2xl flex items-center justify-center rounded-2xl mx-auto text-emerald-800">
            🌾
          </div>
          <h3 className="text-base font-bold text-emerald-950">No Historical Records Found</h3>
          <p className="text-xs text-gray-500 max-w-md mx-auto">
            Jobs marked as {filter.toLowerCase()} will be archived in this historical ledger.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {jobs.map((job) => (
            <div
              key={job.id}
              className="bg-white p-5 rounded-2xl border border-amber-100/70 shadow-xs hover:shadow-sm transition flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div>
                <span className="text-[10px] font-mono font-bold text-gray-400">#JOB-{job.id}</span>
                <h4 className="text-sm font-black text-emerald-950">{job.jobTitle}</h4>
                <p className="text-xs text-gray-500 mt-0.5">📍 {job.workLocation} • 👤 {job.customerName}</p>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-xs font-black text-emerald-900 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200">
                  ₹{job.operatorPayout}
                </span>
                <Link
                  to={`/operator/jobs/${job.id}`}
                  className="text-xs font-bold text-emerald-800 hover:underline"
                >
                  Details →
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default JobHistory;
