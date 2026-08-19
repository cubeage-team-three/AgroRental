import { useState } from 'react';
import { Bell, Check, CalendarCheck, Wallet, ShieldAlert, Sparkles } from 'lucide-react';

const MOCK_NOTIFICATIONS = [
  {
    id: 1,
    title: 'New Booking Request Received',
    message: 'Ramesh Yadav requested Mahindra 575 DI Tractor for 3 days starting 20 Aug 2026.',
    time: '10 mins ago',
    type: 'BOOKING',
    unread: true,
  },
  {
    id: 2,
    title: 'Payment Credited to Wallet',
    message: '₹18,000 received for completed Preet Harvester rental order #BK-2026-0870.',
    time: '2 hours ago',
    type: 'PAYMENT',
    unread: true,
  },
  {
    id: 3,
    title: 'Partner KYC Approved',
    message: 'Your Aadhaar and PAN documents have been verified by AgroRent Compliance.',
    time: 'Yesterday',
    type: 'SYSTEM',
    unread: false,
  },
];

function PartnerNotifications() {
  const [notifications, setNotifications] = useState(MOCK_NOTIFICATIONS);

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, unread: false })));
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 font-sans">
      
      {/* Header */}
      <div className="flex items-center justify-between border-b border-gray-200 pb-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#142E1C] tracking-tight">
            Partner Notifications
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Real-time alerts for rental requests, payouts, and machinery schedules.
          </p>
        </div>

        <button
          type="button"
          onClick={markAllAsRead}
          className="text-xs font-bold text-[#3E7B27] hover:underline"
        >
          Mark all as read
        </button>
      </div>

      {/* Notifications List */}
      <div className="space-y-3">
        {notifications.map((n) => (
          <div
            key={n.id}
            className={`p-5 rounded-3xl border transition-all flex items-start gap-4 ${
              n.unread
                ? 'bg-white border-[#3E7B27]/30 shadow-sm'
                : 'bg-white/60 border-gray-100'
            }`}
          >
            <div
              className={`w-10 h-10 rounded-2xl flex items-center justify-center shrink-0 ${
                n.type === 'BOOKING'
                  ? 'bg-blue-50 text-blue-600'
                  : n.type === 'PAYMENT'
                  ? 'bg-emerald-50 text-emerald-600'
                  : 'bg-purple-50 text-purple-600'
              }`}
            >
              {n.type === 'BOOKING' ? (
                <CalendarCheck className="w-5 h-5" />
              ) : n.type === 'PAYMENT' ? (
                <Wallet className="w-5 h-5" />
              ) : (
                <Sparkles className="w-5 h-5" />
              )}
            </div>

            <div className="flex-1 space-y-1">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-extrabold text-gray-900">{n.title}</h4>
                <span className="text-[11px] text-gray-400 font-medium">{n.time}</span>
              </div>
              <p className="text-xs text-gray-600 font-medium leading-relaxed">{n.message}</p>
            </div>

            {n.unread && (
              <span className="w-2.5 h-2.5 bg-[#3E7B27] rounded-full shrink-0 mt-2" />
            )}
          </div>
        ))}
      </div>

    </div>
  );
}

export default PartnerNotifications;
