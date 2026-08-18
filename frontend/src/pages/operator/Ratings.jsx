import { Link } from "react-router-dom";

function OperatorRatings() {
  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">⭐</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 11 — Reputation & Feedback
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Operator Rating & Client Reviews
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Customer feedback, performance star scores, and agricultural driving reputation metrics.
          </p>
        </div>

        <Link
          to="/operator/dashboard"
          className="bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs px-4 py-2.5 rounded-xl transition"
        >
          ← Dashboard
        </Link>
      </div>

      {/* Star Score Card */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-gradient-to-br from-emerald-900 to-green-950 text-white p-8 rounded-3xl shadow-md text-center space-y-3">
          <span className="text-xs font-bold text-lime-300 uppercase tracking-widest">
            Overall Rating Score
          </span>
          <p className="text-5xl font-black text-white">5.0</p>
          <div className="flex justify-center text-xl text-yellow-400 gap-1">
            ★★★★★
          </div>
          <p className="text-xs text-emerald-200">Based on verified deployment reviews</p>
        </div>

        <div className="md:col-span-2 bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-4">
          <h3 className="text-sm font-bold text-emerald-950 uppercase tracking-wider">
            Performance Breakdown
          </h3>
          <div className="space-y-3 text-xs">
            <div>
              <div className="flex justify-between font-bold text-gray-700 mb-1">
                <span>Machinery Handling & Quality</span>
                <span>100%</span>
              </div>
              <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-600 rounded-full w-full"></div>
              </div>
            </div>
            <div>
              <div className="flex justify-between font-bold text-gray-700 mb-1">
                <span>Punctuality & Schedule Adherence</span>
                <span>100%</span>
              </div>
              <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-600 rounded-full w-full"></div>
              </div>
            </div>
            <div>
              <div className="flex justify-between font-bold text-gray-700 mb-1">
                <span>Field Safety & Communication</span>
                <span>100%</span>
              </div>
              <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-600 rounded-full w-full"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Reviews Card */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-4">
        <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider border-b border-amber-100/60 pb-3">
          Recent Client Testimonials
        </h2>

        <div className="p-8 text-center text-xs text-gray-500 space-y-2">
          <p className="text-xl">🌾</p>
          <p className="font-bold text-gray-800">No Public Customer Reviews Yet</p>
          <p>Complete customer machinery deployments to accumulate ratings and build your public operator portfolio.</p>
        </div>
      </div>
    </div>
  );
}

export default OperatorRatings;
