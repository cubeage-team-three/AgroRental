import { useState } from "react";
import { Link } from "react-router-dom";

function OperatorNotifications() {
  const [filter, setFilter] = useState("ALL");

  const sampleNotifications = [
    {
      id: 1,
      type: "JOB_ASSIGNMENT",
      title: "New Job Assignment Received",
      desc: "Partner Dispatcher assigned you to Mahindra 575 DI Tractor Plowing (Job #100).",
      time: "10 mins ago",
      unread: true,
      link: "/operator/jobs",
    },
    {
      id: 2,
      type: "KYC",
      title: "KYC Documents Approved",
      desc: "Your Aadhaar and Driving License verification was approved by compliance.",
      time: "2 hours ago",
      unread: false,
      link: "/operator/documents",
    },
    {
      id: 3,
      type: "SYSTEM",
      title: "Welcome to Agro Rental Operator Portal",
      desc: "Your profile is active and ready for regional farm machinery deployments.",
      time: "1 day ago",
      unread: false,
      link: "/operator/profile",
    },
  ];

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">🔔</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 10 — Communications
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Job Alerts & Notifications
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Real-time assignment alerts, KYC approval updates, and schedule reminders.
          </p>
        </div>

        <Link
          to="/operator/dashboard"
          className="bg-gray-100 hover:bg-gray-200 text-gray-800 font-bold text-xs px-4 py-2.5 rounded-xl transition"
        >
          ← Dashboard
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="bg-white rounded-2xl p-2 border border-amber-100/70 shadow-sm flex gap-2">
        {["ALL", "UNREAD", "SYSTEM"].map((tab) => (
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

      {/* Notifications List */}
      <div className="space-y-3">
        {sampleNotifications.map((n) => (
          <Link
            key={n.id}
            to={n.link}
            className={`block p-5 rounded-2xl border transition hover:shadow-md ${
              n.unread
                ? "bg-white border-emerald-400 ring-1 ring-emerald-400/20"
                : "bg-white/80 border-amber-100/70"
            }`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <span className="text-xl mt-0.5">
                  {n.type === "JOB_ASSIGNMENT" ? "🚜" : n.type === "KYC" ? "🪪" : "📢"}
                </span>
                <div>
                  <div className="flex items-center gap-2">
                    <h4 className="text-sm font-black text-emerald-950">{n.title}</h4>
                    {n.unread && (
                      <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                    )}
                  </div>
                  <p className="text-xs text-gray-600 mt-1">{n.desc}</p>
                </div>
              </div>
              <span className="text-[10px] text-gray-400 whitespace-nowrap font-medium">{n.time}</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}

export default OperatorNotifications;
