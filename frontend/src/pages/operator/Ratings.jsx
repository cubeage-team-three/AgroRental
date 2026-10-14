import React, { useState, useEffect } from 'react';
import { 
  Star, 
  MessageSquare, 
  Award, 
  ThumbsUp, 
  TrendingUp, 
  ShieldCheck, 
  Calendar, 
  RefreshCw, 
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  UserCheck
} from 'lucide-react';
import operatorService from '../../services/operatorService';

function OperatorRatings() {
  const [summary, setSummary] = useState(null);
  const [reviewsPage, setReviewsPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 5;

  const fetchRatingsData = async (page = 0, isManualRefresh = false) => {
    try {
      if (isManualRefresh) setRefreshing(true);
      else setLoading(true);
      setError(null);

      const [summaryRes, reviewsRes] = await Promise.all([
        operatorService.getMyRatingSummary(),
        operatorService.getMyReviews({ page, size: pageSize })
      ]);

      const summaryData = summaryRes?.data || summaryRes;
      if (summaryData && typeof summaryData === 'object') {
        setSummary(summaryData);
      }
      const reviewsData = reviewsRes?.data || reviewsRes;
      if (reviewsData && (reviewsData.content || Array.isArray(reviewsData))) {
        setReviewsPage(reviewsData);
      }
    } catch (err) {
      console.error('Failed to load ratings data:', err);
      setError(err.message || 'Unable to retrieve customer feedback and ratings.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchRatingsData(currentPage);
  }, [currentPage]);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage < (reviewsPage?.totalPages || 1)) {
      setCurrentPage(newPage);
    }
  };

  const renderStarVisual = (rating, max = 5, size = 18) => {
    const stars = [];
    for (let i = 1; i <= max; i++) {
      stars.push(
        <Star
          key={i}
          size={size}
          className={`${
            i <= rating 
              ? 'text-amber-400 fill-amber-400' 
              : 'text-gray-300'
          } inline-block transition-colors`}
        />
      );
    }
    return stars;
  };

  const calculatePercentage = (count, total) => {
    if (!total || total === 0) return 0;
    return Math.round((count / total) * 100);
  };

  if (loading && !refreshing) {
    return (
      <div className="p-6 max-w-7xl mx-auto space-y-6">
        <div className="h-8 bg-gray-200 rounded w-1/3 animate-pulse"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="h-44 bg-gray-200 rounded-xl animate-pulse"></div>
          <div className="h-44 bg-gray-200 rounded-xl animate-pulse"></div>
          <div className="h-44 bg-gray-200 rounded-xl animate-pulse"></div>
        </div>
        <div className="h-80 bg-gray-200 rounded-xl animate-pulse"></div>
      </div>
    );
  }

  const totalReviews = summary?.totalReviews || 0;
  const avgRating = summary?.averageRating ? Number(summary.averageRating).toFixed(1) : '0.0';
  const fiveStarPercent = calculatePercentage(summary?.fiveStarCount || 0, totalReviews);

  const starBreakdowns = [
    { star: 5, count: summary?.fiveStarCount || 0, color: 'bg-emerald-500' },
    { star: 4, count: summary?.fourStarCount || 0, color: 'bg-teal-500' },
    { star: 3, count: summary?.threeStarCount || 0, color: 'bg-amber-400' },
    { star: 2, count: summary?.twoStarCount || 0, color: 'bg-orange-400' },
    { star: 1, count: summary?.oneStarCount || 0, color: 'bg-rose-500' },
  ];

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8 font-sans">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-slate-200 pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="bg-amber-100 text-amber-800 text-xs font-semibold px-2.5 py-0.5 rounded-full flex items-center gap-1">
              <Award size={13} />
              Verified Operator Performance
            </span>
          </div>
          <h1 className="text-3xl font-black text-slate-900 tracking-tight mt-1">Ratings & Customer Reviews</h1>
          <p className="text-slate-500 text-sm mt-0.5">
            Transparent feedback and star ratings submitted by verified farmers upon completed field tasks.
          </p>
        </div>
        <button
          onClick={() => fetchRatingsData(currentPage, true)}
          disabled={refreshing}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-slate-200 rounded-xl shadow-xs text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors disabled:opacity-50"
        >
          <RefreshCw size={16} className={refreshing ? 'animate-spin text-emerald-700' : 'text-slate-500'} />
          {refreshing ? 'Refreshing...' : 'Refresh Reviews'}
        </button>
      </div>

      {error && (
        <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 flex items-start gap-3 text-rose-800">
          <AlertCircle size={20} className="text-rose-600 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <h4 className="font-semibold text-sm">Error Loading Reviews</h4>
            <p className="text-xs mt-0.5">{error}</p>
          </div>
          <button 
            onClick={() => fetchRatingsData(currentPage, false)}
            className="text-xs font-semibold underline hover:text-rose-900"
          >
            Retry
          </button>
        </div>
      )}

      {/* Top Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Overall Score Card */}
        <div className="bg-gradient-to-r from-[#0F382C] via-[#194D3E] to-[#2E7D32] rounded-3xl p-6 text-white shadow-xl flex flex-col justify-between relative overflow-hidden">
          <div className="absolute right-0 top-0 translate-x-4 -translate-y-4 opacity-10">
            <Star size={180} />
          </div>
          <div>
            <span className="text-lime-300 text-xs font-bold uppercase tracking-wider">Overall Rating</span>
            <div className="flex items-baseline gap-3 mt-2">
              <span className="text-5xl font-black">{avgRating}</span>
              <span className="text-emerald-200 text-lg font-medium">/ 5.0</span>
            </div>
            <div className="flex items-center gap-1 mt-3">
              {renderStarVisual(Math.round(summary?.averageRating || 0), 5, 22)}
            </div>
          </div>
          <div className="mt-6 pt-4 border-t border-white/20 flex items-center justify-between text-xs text-emerald-100">
            <span className="flex items-center gap-1.5">
              <ShieldCheck size={16} className="text-lime-300" />
              100% Verified Farmer Reviews
            </span>
            <span className="font-semibold">{totalReviews} {totalReviews === 1 ? 'Review' : 'Reviews'}</span>
          </div>
        </div>

        {/* Satisfaction & 5-Star Share Card */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500">5-Star Satisfaction</span>
              <span className="p-2 bg-emerald-50 text-emerald-700 rounded-xl">
                <ThumbsUp size={18} />
              </span>
            </div>
            <div className="mt-2">
              <div className="text-4xl font-black text-slate-900">{fiveStarPercent}%</div>
              <p className="text-xs text-slate-500 mt-1">
                {summary?.fiveStarCount || 0} of {totalReviews} farmers gave you a perfect 5-star rating.
              </p>
            </div>
          </div>
          <div className="mt-4 pt-4 border-t border-slate-100 flex items-center gap-2 text-xs font-bold text-emerald-700">
            <TrendingUp size={15} />
            <span>Top Tier Machine Operator Standing</span>
          </div>
        </div>

        {/* Rating Breakdown Bars */}
        <div className="bg-white border border-slate-200 rounded-3xl p-6 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Star Distribution</span>
            <span className="text-xs font-semibold text-slate-400">{totalReviews} Total</span>
          </div>
          <div className="space-y-2">
            {starBreakdowns.map(({ star, count, color }) => {
              const pct = calculatePercentage(count, totalReviews);
              return (
                <div key={star} className="flex items-center gap-2 text-xs">
                  <span className="w-6 font-semibold text-slate-700 flex items-center gap-0.5">
                    {star} <Star size={11} className="text-amber-400 fill-amber-400" />
                  </span>
                  <div className="flex-1 bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div 
                      className={`h-full rounded-full ${color} transition-all duration-500`}
                      style={{ width: `${pct}%` }}
                    ></div>
                  </div>
                  <span className="w-8 text-right font-medium text-slate-500">{count}</span>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Reviews Ledger / Customer Feedback List */}
      <div className="bg-white border border-slate-200 rounded-3xl shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h2 className="text-lg font-black text-slate-900 flex items-center gap-2">
              <MessageSquare size={19} className="text-emerald-700" />
              Farmer Feedback Ledger
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Chronological log of feedback comments left by verified field customers.
            </p>
          </div>
          <span className="text-xs font-bold bg-slate-100 text-slate-700 px-3 py-1 rounded-full">
            Page {(reviewsPage?.number || 0) + 1} of {reviewsPage?.totalPages || 1}
          </span>
        </div>

        {(!reviewsPage?.content || reviewsPage.content.length === 0) ? (
          <div className="p-12 text-center">
            <div className="w-16 h-16 bg-emerald-50 text-emerald-700 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <MessageSquare size={28} />
            </div>
            <h3 className="text-base font-black text-slate-900">No Reviews Received Yet</h3>
            <p className="text-xs text-slate-500 max-w-md mx-auto mt-1">
              Once you complete field machinery assignments, farmers will be able to submit their star rating and review comments here.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {reviewsPage.content.map((review) => (
              <div key={review.reviewId} className="p-6 hover:bg-slate-50/70 transition-colors">
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                  <div className="flex items-start gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-900 flex items-center justify-center font-black text-sm flex-shrink-0">
                      {review.farmerName ? review.farmerName.charAt(0).toUpperCase() : 'F'}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-extrabold text-sm text-slate-900">{review.farmerName || 'Verified Farmer'}</h4>
                        <span className="inline-flex items-center gap-0.5 text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                          <UserCheck size={12} />
                          Verified Farmer
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
                        <span className="flex items-center gap-1">
                          <Calendar size={13} className="text-slate-400" />
                          {review.createdAt ? new Date(review.createdAt).toLocaleDateString('en-IN', {
                            day: 'numeric',
                            month: 'short',
                            year: 'numeric'
                          }) : 'Recent'}
                        </span>
                        <span>•</span>
                        <span className="font-semibold text-slate-600">Booking #{review.bookingId}</span>
                        <span>•</span>
                        <span className="font-semibold text-slate-600">Assignment #{review.assignmentId}</span>
                      </div>
                    </div>
                  </div>

                  {/* Rating Stars Badge */}
                  <div className="flex items-center gap-1 bg-amber-50 border border-amber-200 px-3 py-1.5 rounded-xl self-start">
                    <span className="font-bold text-sm text-amber-900">{review.rating}.0</span>
                    <div className="flex items-center gap-0.5">
                      {renderStarVisual(review.rating, 5, 14)}
                    </div>
                  </div>
                </div>

                {/* Comment Text */}
                {review.comment ? (
                  <div className="mt-3.5 pl-13 text-sm text-slate-700 bg-slate-50 rounded-2xl p-3.5 border border-slate-200 font-normal italic">
                    "{review.comment}"
                  </div>
                ) : (
                  <div className="mt-2 pl-13 text-xs text-slate-400 italic">
                    No written comment provided with star rating.
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Pagination Footer */}
        {reviewsPage && reviewsPage.totalPages > 1 && (
          <div className="p-4 bg-slate-50 border-t border-slate-200 flex items-center justify-between">
            <span className="text-xs text-slate-500">
              Showing {(reviewsPage.number * pageSize) + 1} to {Math.min((reviewsPage.number + 1) * pageSize, reviewsPage.totalElements)} of {reviewsPage.totalElements} reviews
            </span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="p-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                aria-label="Previous Page"
              >
                <ChevronLeft size={16} />
              </button>
              <span className="text-xs font-bold text-slate-700 px-2">
                {currentPage + 1} / {reviewsPage.totalPages}
              </span>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= reviewsPage.totalPages - 1}
                className="p-1.5 rounded-xl border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                aria-label="Next Page"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default OperatorRatings;
