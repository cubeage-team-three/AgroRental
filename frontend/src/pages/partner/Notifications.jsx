import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Bell, 
  CalendarCheck, 
  RefreshCw, 
  ArrowRight, 
  Trash2, 
  XCircle, 
  CheckCircle,
  AlertTriangle,
  Wallet,
  HardHat,
  Info
} from 'lucide-react';
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
      window.dispatchEvent(new Event('notificationsUpdated'));
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
      window.dispatchEvent(new Event('notificationsUpdated'));
    } catch (err) {
      console.error('Failed to mark read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead('PARTNER', partnerId);
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
      window.dispatchEvent(new Event('notificationsUpdated'));
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const handleDelete = async (e, id) => {
    e.stopPropagation(); // prevent clicking the card and marking as read
    try {
      await notificationService.deleteNotification(id, 'PARTNER', partnerId);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      window.dispatchEvent(new Event('notificationsUpdated'));
    } catch (err) {
      console.error('Failed to delete notification:', err);
    }
  };

  const handleClearAll = async () => {
    if (!window.confirm("Are you sure you want to delete all notifications?")) return;
    try {
      await notificationService.clearAllNotifications('PARTNER', partnerId);
      setNotifications([]);
      window.dispatchEvent(new Event('notificationsUpdated'));
    } catch (err) {
      console.error('Failed to clear notifications:', err);
    }
  };

  const getNotificationIcon = (type) => {
    switch (type) {
      case 'BOOKING_CREATED':
      case 'BOOKING_COMPLETED':
        return { icon: CalendarCheck, color: 'text-emerald-600', bg: 'bg-emerald-50' };
      case 'BOOKING_CANCELLED':
      case 'BOOKING_REJECTED':
        return { icon: XCircle, color: 'text-red-600', bg: 'bg-red-50' };
      case 'PAYMENT_RECEIVED':
      case 'PAYMENT_SUCCESS':
        return { icon: Wallet, color: 'text-lime-600', bg: 'bg-lime-50' };
      case 'OPERATOR_ASSIGNED':
        return { icon: HardHat, color: 'text-amber-600', bg: 'bg-amber-50' };
      default:
        return { icon: Info, color: 'text-blue-600', bg: 'bg-blue-50' };
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-gray-200 pb-4">
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
            title="Refresh"
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

          {notifications.length > 0 && (
            <button
              type="button"
              onClick={handleClearAll}
              className="text-xs font-bold text-red-600 hover:underline border-l border-gray-300 pl-3 ml-1"
            >
              Clear All
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
          {notifications.map((n) => {
            const { icon: Icon, color, bg } = getNotificationIcon(n.notificationType);
            return (
              <div
                key={n.id}
                onClick={() => !n.isRead && handleMarkAsRead(n.id)}
                className={`p-5 rounded-3xl border transition-all flex flex-col sm:flex-row items-start sm:items-center gap-4 ${
                  !n.isRead
                    ? 'cursor-pointer bg-white border-[#3E7B27]/40 shadow-sm'
                    : 'bg-white/60 border-gray-100'
                }`}
              >
                <div className={`w-10 h-10 rounded-2xl ${bg} ${color} flex items-center justify-center shrink-0`}>
                  <Icon className="w-5 h-5" />
                </div>

                <div className="flex-1 space-y-1 w-full">
                  <div className="flex items-center justify-between">
                    <h4 className="text-sm font-extrabold text-gray-900">{n.title}</h4>
                    <span className="text-[11px] text-gray-400 font-medium">
                      {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                    </span>
                  </div>
                  <p className="text-xs text-gray-600 font-medium leading-relaxed">{n.message}</p>
                  
                  {n.bookingId && (
                    <div className="pt-1">
                      <Link to="/partner/bookings" className="text-xs font-bold text-[#3E7B27] hover:underline flex items-center gap-1 w-fit">
                        View Order Details <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-3 self-end sm:self-auto shrink-0 mt-2 sm:mt-0">
                  {!n.isRead && (
                    <span className="w-2.5 h-2.5 bg-[#3E7B27] rounded-full shrink-0" title="Unread" />
                  )}
                  <button
                    type="button"
                    onClick={(e) => handleDelete(e, n.id)}
                    className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    title="Delete Notification"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default PartnerNotifications;
