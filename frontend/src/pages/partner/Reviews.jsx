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
      setError(err.message || 'Failed to retrieve reviews.');
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

  return (
    <div className="max-w-6xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
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
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100 grid grid-cols-1 sm:grid-cols-3 gap-6 items-center">
        <div className="text-center sm:text-left space-y-1">
          <span className="text-xs font-bold uppercase tracking-wider text-gray-400">Fleet Average Rating</span>
          <div className="flex items-center justify-center sm:justify-start gap-2">
            <span className="text-4xl font-black text-[#142E1C]">{avgRating.toFixed(1)}</span>
            <div className="flex text-amber-400 text-xl">
              {[1, 2, 3, 4, 5].map((s) => (
                <Star
                  key={s}
                  className={`w-5 h-5 ${s <= Math.round(avgRating) ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`}
                />
              ))}
            </div>
          </div>
          <p className="text-xs text-gray-500 font-medium">Based on {totalCount} verified farmer reviews</p>
        </div>

        <div className="sm:col-span-2 space-y-2 text-xs">
          <div className="p-4 bg-emerald-50/60 border border-emerald-100 rounded-2xl text-emerald-900 font-semibold leading-relaxed">
            ⭐ Live backend ratings automatically summarize feedback from completed machinery rental reservations.
          </div>
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
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-1.5">
                    <h4 className="text-sm font-extrabold text-gray-900">{rev.farmerName || 'Farmer'}</h4>
                    <BadgeCheck className="w-4 h-4 text-emerald-600" />
                  </div>
                  <span className="text-xs text-gray-500">
                    {rev.equipmentName} • Booking #{rev.bookingId} • {new Date(rev.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <div className="flex items-center text-amber-400">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-4 h-4 ${star <= rev.rating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`}
                    />
                  ))}
                </div>
              </div>

              {rev.comment && (
                <p className="text-xs sm:text-sm text-gray-700 leading-relaxed font-medium italic">
                  "{rev.comment}"
                </p>
              )}
            </div>
          ))
        )}
      </div>

    </div>
  );
}

export default PartnerReviews;
