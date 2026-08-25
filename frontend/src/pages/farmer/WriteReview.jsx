import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  Star,
  CheckCircle2,
  AlertCircle,
  ArrowLeft,
  Calendar,
  Tractor,
  Send,
  RefreshCw,
  Sparkles,
} from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { bookingService } from '../../services/bookingService';
import { reviewService } from '../../services/reviewService';
import agroRentLogo from '../../assets/images/agrorent-logo.jpeg';

function WriteReview() {
  const { id } = useParams();
  const navigate = useNavigate();
  const farmerId = getFarmerId();

  const [booking, setBooking] = useState(null);
  const [existingReview, setExistingReview] = useState(null);
  const [rating, setRating] = useState(5);
  const [hoverRating, setHoverRating] = useState(0);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadData() {
      if (!id) return;
      setLoading(true);
      setError(null);
      try {
        const bookingData = await bookingService.getBookingById(id);
        setBooking(bookingData);

        try {
          const rev = await reviewService.getReviewByBookingId(id);
          setExistingReview(rev);
          if (rev) {
            setRating(rev.rating);
            setComment(rev.comment || '');
          }
        } catch {
          // Review not created yet
        }
      } catch (err) {
        console.error('Failed to load review details:', err);
        setError(err.message || 'Booking reservation not found.');
      } finally {
        setLoading(false);
      }
    }

    loadData();
  }, [id]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!booking) return;

    if (booking.status !== 'COMPLETED') {
      setError('Only completed bookings can be reviewed.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const response = await reviewService.createReview({
        bookingId: booking.id,
        farmerId,
        rating,
        comment,
      });

      setExistingReview(response);
    } catch (err) {
      console.error('Failed to submit review:', err);
      setError(err.message || 'Failed to submit review. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto p-8 space-y-6 font-sans animate-pulse">
        <div className="h-8 bg-gray-200 rounded-xl w-1/3" />
        <div className="h-64 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  if (error && !booking) {
    return (
      <div className="max-w-xl mx-auto my-12 p-8 bg-white border border-gray-100 rounded-3xl text-center space-y-4 font-sans">
        <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
        <h2 className="text-xl font-extrabold text-gray-900">Review Error</h2>
        <p className="text-xs text-gray-500">{error}</p>
        <Link
          to="/farmer/my-bookings"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-emerald-700 text-white text-xs font-bold rounded-xl hover:bg-emerald-800"
        >
          <ArrowLeft className="w-4 h-4" /> Return to My Bookings
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6 font-sans">
      
      {/* Navigation Header */}
      <div className="flex items-center gap-3 border-b border-gray-200 pb-4">
        <Link
          to="/farmer/my-bookings"
          className="p-2 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100"
        >
          <ArrowLeft className="w-4 h-4" />
        </Link>
        <div className="hidden sm:flex items-center overflow-hidden rounded-xl bg-white px-2.5 py-1 shadow-sm border border-slate-200 h-11 shrink-0">
          <img src={agroRentLogo} alt="AgroRent" className="h-full w-auto object-contain" />
        </div>
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Rate Your Rental Experience
          </h1>
          <p className="text-xs text-gray-500">Feedback for Reservation #{booking.id}</p>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-xs font-bold flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Booking Overview Card */}
      <div className="bg-white rounded-3xl border border-gray-100 p-6 shadow-sm flex items-center justify-between gap-4">
        <div className="space-y-1">
          <span className="text-[10px] font-black uppercase text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
            {booking.equipmentCategory}
          </span>
          <h3 className="text-lg font-black text-gray-900">{booking.equipmentName}</h3>
          <p className="text-xs text-gray-500 flex items-center gap-1 font-medium">
            <Calendar className="w-3.5 h-3.5" /> {booking.startDate} to {booking.endDate}
          </p>
        </div>

        <span className="text-xs font-black uppercase px-3 py-1 bg-blue-100 text-blue-800 rounded-full border border-blue-200">
          {booking.status}
        </span>
      </div>

      {/* Review Submission or Already Submitted View */}
      {existingReview ? (
        <div className="bg-white rounded-3xl border border-emerald-200 p-8 text-center space-y-4 shadow-sm">
          <CheckCircle2 className="w-16 h-16 text-emerald-600 mx-auto" />
          <h2 className="text-2xl font-black text-gray-900">Review Submitted!</h2>
          <p className="text-xs text-gray-600">Thank you for sharing your experience with the AgroRental community.</p>

          <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-4 max-w-md mx-auto space-y-2 text-left">
            <div className="flex items-center gap-1 text-amber-500 text-lg">
              {[1, 2, 3, 4, 5].map((star) => (
                <Star
                  key={star}
                  className={`w-5 h-5 ${star <= existingReview.rating ? 'fill-amber-400 text-amber-400' : 'text-gray-300'}`}
                />
              ))}
            </div>
            {existingReview.comment && (
              <p className="text-xs text-slate-700 leading-relaxed font-medium italic">
                "{existingReview.comment}"
              </p>
            )}
          </div>

          <div className="pt-2">
            <Link
              to="/farmer/my-bookings"
              className="inline-flex items-center gap-2 px-6 py-3 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold rounded-xl shadow-md transition"
            >
              <ArrowLeft className="w-4 h-4" /> Back to My Bookings
            </Link>
          </div>
        </div>
      ) : booking.status !== 'COMPLETED' ? (
        <div className="bg-white border border-amber-200 rounded-3xl p-8 text-center space-y-3">
          <AlertCircle className="w-12 h-12 text-amber-500 mx-auto" />
          <h3 className="text-lg font-black text-gray-900">Rental Not Completed Yet</h3>
          <p className="text-xs text-gray-500 max-w-md mx-auto">
            Reviews can only be submitted once the machinery rental job has been marked as <strong>COMPLETED</strong> by the operator.
          </p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="bg-white rounded-3xl border border-gray-100 p-6 sm:p-8 space-y-6 shadow-sm">
          
          <div className="space-y-2 text-center">
            <label className="block text-sm font-extrabold text-gray-900">
              How would you rate this machinery rental service?
            </label>
            <div className="flex items-center justify-center gap-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  className="p-1 transition-transform hover:scale-110"
                >
                  <Star
                    className={`w-8 h-8 ${
                      star <= (hoverRating || rating)
                        ? 'fill-amber-400 text-amber-400'
                        : 'text-gray-300'
                    }`}
                  />
                </button>
              ))}
            </div>
            <span className="text-xs font-bold text-gray-500 block">
              {rating === 5 ? '⭐ Excellent Quality & Service' : rating === 4 ? '👍 Good Experience' : rating === 3 ? '👌 Average' : '👎 Poor'}
            </span>
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-gray-700">
              Share detailed feedback (Optional)
            </label>
            <textarea
              rows={4}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Describe machine performance, cleanliness, operator support, or timeliness..."
              className="w-full rounded-2xl border border-gray-200 p-3.5 text-xs text-gray-900 focus:border-emerald-600 focus:ring-2 focus:ring-emerald-100 outline-none transition"
            />
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-3.5 bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-extrabold rounded-xl shadow-md transition flex items-center justify-center gap-2 disabled:opacity-70"
          >
            {submitting ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Submitting Review...</span>
              </>
            ) : (
              <>
                <Send className="w-4 h-4" />
                <span>Submit Review & Rating</span>
              </>
            )}
          </button>

        </form>
      )}

    </div>
  );
}

export default WriteReview;
