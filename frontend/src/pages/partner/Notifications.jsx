import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CalendarCheck, CheckCircle2, AlertCircle, RefreshCw, Sparkles, ArrowRight, Wallet } from 'lucide-react';
import { getPartnerId } from '../../services/authService';
import { notificationService } from '../../services/notificationService';

function PartnerNotifications() {
  const partnerId = getPartnerId();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchNotifications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await notificationService.getUserNotifications('PARTNER', partnerId);
      setNotifications(data || []);
    } catch (err) {
      console.error('Failed to load partner notifications:', err);
      setError(err.response?.data?.message || 'Failed to retrieve notifications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [partnerId]);

  const handleMarkAsRead = async (id) => {
    try {
      await notificationService.markAsRead(id, 'PARTNER', partnerId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (err) {
      console.error('Failed to mark read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead('PARTNER', partnerId);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6 font-sans">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium text-sm">Loading partner alerts...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Partner Notifications
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Real-time alerts for rental requests, payouts, and machinery schedules.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchNotifications}
            className="p-2 rounded-xl bg-gray-100 hover:bg-gray-200 text-gray-700 transition"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          {notifications.some((n) => !n.isRead) && (
            <button
              type="button"
              onClick={handleMarkAllAsRead}
              className="text-xs font-bold text-[#3E7B27] hover:underline"
            >
              Mark all as read
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border-l-4 border-red-500 rounded-2xl text-red-700 text-sm font-medium flex items-center justify-between shadow-xs">
          <span>{error}</span>
        </div>
      )}

      {notifications.length === 0 ? (
        <div className="bg-white border border-dashed border-gray-300 rounded-3xl p-12 text-center space-y-3">
          <Bell className="w-12 h-12 text-gray-300 mx-auto" />
          <h3 className="text-lg font-black text-gray-900">No Notifications</h3>
          <p className="text-xs text-gray-500">Incoming equipment booking alerts will appear here.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.isRead && handleMarkAsRead(n.id)}
              className={`p-5 rounded-3xl border transition-all flex items-start gap-4 cursor-pointer ${
                !n.isRead
                  ? 'bg-white border-[#3E7B27]/40 shadow-sm'
                  : 'bg-white/60 border-gray-100'
              }`}
            >
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                <CalendarCheck className="w-5 h-5" />
              </div>

              <div className="flex-1 space-y-1">
                <div className="flex items-center justify-between">
                  <h4 className="text-sm font-extrabold text-gray-900">{n.title}</h4>
                  <span className="text-[11px] text-gray-400 font-medium">
                    {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                  </span>
                </div>
                <p className="text-xs text-gray-600 font-medium leading-relaxed">{n.message}</p>
                {n.bookingId && (
                  <div className="pt-1">
                    <Link to="/partner/bookings" className="text-xs font-bold text-[#3E7B27] hover:underline flex items-center gap-1">
                      View Order Requests <ArrowRight className="w-3.5 h-3.5" />
                    </Link>
                  </div>
                )}
              </div>

              {!n.isRead && (
                <span className="w-2.5 h-2.5 bg-[#3E7B27] rounded-full shrink-0 mt-2" />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default PartnerNotifications;
