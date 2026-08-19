import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Bell, CalendarCheck, CheckCircle2, AlertCircle, RefreshCw, Sparkles, ArrowRight } from 'lucide-react';
import { getFarmerId } from '../../services/authService';
import { notificationService } from '../../services/notificationService';

function FarmerNotifications() {
  const farmerId = getFarmerId();

  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchNotifications = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await notificationService.getUserNotifications('FARMER', farmerId);
      setNotifications(data || []);
    } catch (err) {
      console.error('Failed to load farmer notifications:', err);
      setError(err.response?.data?.message || 'Failed to retrieve notifications.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [farmerId]);

  const handleMarkAsRead = async (id) => {
    try {
      await notificationService.markAsRead(id, 'FARMER', farmerId);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch (err) {
      console.error('Failed to mark read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    setActionLoading(true);
    try {
      await notificationService.markAllAsRead('FARMER', farmerId);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    } finally {
      setActionLoading(false);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'BOOKING_CONFIRMED':
      case 'BOOKING_COMPLETED':
        return <CheckCircle2 className="h-5 w-5 text-emerald-600" />;
      case 'BOOKING_REJECTED':
      case 'BOOKING_CANCELLED':
        return <AlertCircle className="h-5 w-5 text-red-600" />;
      case 'OPERATOR_ASSIGNED':
        return <Sparkles className="h-5 w-5 text-blue-600" />;
      default:
        return <Bell className="h-5 w-5 text-slate-600" />;
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center p-6 font-sans">
        <div className="text-center">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-emerald-600 border-t-transparent mx-auto mb-3"></div>
          <p className="text-slate-600 font-medium text-sm">Loading your notification alerts...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 font-sans space-y-6">
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-slate-900">Farmer Notifications</h1>
          <p className="text-sm text-slate-600 mt-1">Real-time alerts for machine bookings and operator assignments</p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={fetchNotifications}
            className="p-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 transition"
            title="Refresh Notifications"
          >
            <RefreshCw className="h-4 w-4" />
          </button>
          {notifications.some((n) => !n.isRead) && (
            <button
              type="button"
              disabled={actionLoading}
              onClick={handleMarkAllAsRead}
              className="text-xs font-bold text-emerald-700 hover:underline"
            >
              Mark all read
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="flex items-center gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <AlertCircle className="h-5 w-5 text-red-600 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {notifications.length === 0 ? (
        <div className="rounded-3xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <Bell className="mx-auto h-12 w-12 text-slate-300 mb-3" />
          <h3 className="text-lg font-bold text-slate-900 mb-1">No Notifications Yet</h3>
          <p className="text-xs text-slate-500">You will receive updates here when partners confirm your rental bookings.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {notifications.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.isRead && handleMarkAsRead(n.id)}
              className={`p-5 rounded-3xl border transition-all flex items-start gap-4 cursor-pointer ${
                !n.isRead
                  ? 'bg-white border-emerald-300 shadow-sm ring-1 ring-emerald-100'
                  : 'bg-white/70 border-slate-200'
              }`}
            >
              <div className="p-2.5 rounded-2xl bg-slate-50 shrink-0">
                {getNotificationIcon(n.notificationType)}
              </div>

              <div className="flex-1 min-w-0 space-y-1">
                <div className="flex items-center justify-between gap-2">
                  <h4 className="text-sm font-bold text-slate-900 truncate">{n.title}</h4>
                  <span className="text-[11px] font-semibold text-slate-400 shrink-0">
                    {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                  </span>
                </div>
                <p className="text-xs font-medium text-slate-600 leading-relaxed">{n.message}</p>

                {n.bookingId && (
                  <div className="pt-2">
                    <Link
                      to="/farmer/my-bookings"
                      className="inline-flex items-center gap-1 text-xs font-bold text-emerald-700 hover:text-emerald-800"
                    >
                      View Reservation Details <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                )}
              </div>

              {!n.isRead && (
                <span className="h-2.5 w-2.5 rounded-full bg-emerald-600 shrink-0 mt-2" title="Unread" />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default FarmerNotifications;
