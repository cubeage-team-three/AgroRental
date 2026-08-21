import { useState, useEffect } from 'react';
import { Star, MessageSquare, BadgeCheck, RefreshCw } from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { reviewService } from '../../services/reviewService';

function PartnerReviews() {
  const partnerId = getPartnerId();

  const [summary, setSummary] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchReviewsData = async () => {
    setLoading(true);
    setError(null);
    try {
      const summaryData = await reviewService.getPartnerRatingSummary(partnerId);
      setSummary(summaryData);

      const list = await reviewService.getReviewsForPartner(partnerId);
      setReviews(list || []);
    } catch (err) {
      console.error('Failed to load partner review metrics:', err);
      setError(err.response?.data?.message || err.message || 'Failed to retrieve reviews.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReviewsData();
  }, [partnerId]);

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6 font-sans">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium text-sm">Loading partner reviews & ratings...</p>
        </div>
      </div>
    );
  }

  const avgRating = summary?.averageRating || 0;
  const totalCount = summary?.totalReviews || 0;

  // Calculate rating distribution locally from the fetched reviews list
  const distribution = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  reviews.forEach((r) => {
    if (r.rating >= 1 && r.rating <= 5) {
      distribution[r.rating] += 1;
    }
  });

  return (
    <div className="max-w-6xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Machinery Reviews & Farmer Ratings
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Feedback and quality ratings from farmers who rented your machinery fleet.
          </p>
        </div>

        <button
          type="button"
          onClick={fetchReviewsData}
          className="p-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 transition"
          title="Refresh Reviews"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between">
          <span>{error}</span>
        </div>
      )}

      {/* Rating Overview Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 grid grid-cols-1 md:grid-cols-3 gap-8 items-center">
        <div className="text-center md:text-left space-y-1">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Fleet Average Rating</span>
          <div className="flex items-center justify-center md:justify-start gap-2">
            <span className="text-5xl font-black text-[#142E1C]">{avgRating.toFixed(1)}</span>
            <div className="flex flex-col gap-1">
              <div className="flex text-amber-400 text-xl">
                {[1, 2, 3, 4, 5].map((s) => (
                  <Star
                    key={s}
                    className={`w-5 h-5 ${s <= Math.round(avgRating) ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`}
                  />
                ))}
              </div>
              <span className="text-xs text-gray-500 font-medium">{totalCount} verified reviews</span>
            </div>
          </div>
        </div>

        <div className="md:col-span-2 space-y-2.5">
          {[5, 4, 3, 2, 1].map((star) => {
            const count = distribution[star];
            const percentage = totalCount > 0 ? (count / totalCount) * 100 : 0;
            return (
              <div key={star} className="flex items-center gap-3 text-xs">
                <span className="w-12 text-right font-bold text-gray-600 flex items-center justify-end gap-1 shrink-0">
                  {star} <Star className="w-3.5 h-3.5 fill-gray-400 text-gray-400" />
                </span>
                <div className="flex-1 h-2.5 bg-gray-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-amber-400 rounded-full"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
                <span className="w-10 text-gray-500 font-medium shrink-0">{count}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Reviews List */}
      <div className="space-y-4">
        {reviews.length === 0 ? (
          <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-3">
            <MessageSquare className="w-12 h-12 text-gray-300 mx-auto" />
            <h3 className="text-lg font-black text-gray-900">No Reviews Yet</h3>
            <p className="text-xs text-gray-500">Reviews submitted by farmers after completed bookings will appear here.</p>
          </div>
        ) : (
          reviews.map((rev) => (
            <div
              key={rev.id}
              className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-3"
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-1.5">
                    <h4 className="text-sm font-extrabold text-gray-900">{rev.farmerName || 'Farmer'}</h4>
                    <BadgeCheck className="w-4 h-4 text-emerald-600" />
                  </div>
                  <span className="text-xs text-gray-500 block mt-0.5">
                    {rev.equipmentName} • Booking #{rev.bookingId}
                  </span>
                  <span className="text-[11px] text-gray-400 font-medium block mt-1">
                    Service Date: {rev.serviceDate ? new Date(rev.serviceDate).toLocaleDateString() : new Date(rev.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="flex items-center text-amber-400 bg-amber-50 px-2.5 py-1 rounded-lg">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-4 h-4 ${star <= rev.rating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`}
                    />
                  ))}
                  <span className="ml-2 font-bold text-amber-700 text-xs">{rev.rating}.0</span>
                </div>
              </div>

              {rev.comment && (
                <div className="pt-2">
                  <p className="text-sm text-gray-700 leading-relaxed font-medium bg-gray-50/80 p-3.5 rounded-2xl border border-gray-100">
                    "{rev.comment}"
                  </p>
                </div>
              )}
            </div>
          ))
        )}
      </div>

    </div>
  );
}

export default PartnerReviews;
