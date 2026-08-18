function OperatorBenefits() {
  const benefits = [
    {
      icon: "🌾",
      title: "Get More Jobs",
      description: "Receive direct agricultural machinery assignments from verified equipment partners and farm owners in your district.",
    },
    {
      icon: "🛡️",
      title: "Verified Profile",
      description: "Build trust with digital KYC, driving license badges, and certified machinery operating skill verifications.",
    },
    {
      icon: "⏱️",
      title: "Flexible Schedule",
      description: "Review assignment times, field locations, and durations before accepting. Work on your own terms.",
    },
    {
      icon: "💰",
      title: "Guaranteed Payouts",
      description: "Clear fixed-rate payouts for every completed job order, deposited directly into your verified bank account.",
    },
    {
      icon: "⭐",
      title: "Build Your Reputation",
      description: "Earn 5-star operator ratings from farmers and partners to unlock premium high-capacity machinery deployments.",
    },
    {
      icon: "🔔",
      title: "Stay Connected",
      description: "Get real-time job dispatch notifications, schedule reminders, and route coordinates sent right to your phone.",
    },
  ];

  return (
    <section className="py-16 md:py-24 bg-white/60 border-y border-amber-100/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-emerald-50 text-emerald-800 rounded-full text-xs font-bold uppercase tracking-wider border border-emerald-200">
            <span>✨</span>
            <span>Why Join Agro Rental</span>
          </div>
          <h2 className="text-3xl md:text-4xl font-extrabold text-emerald-950 font-serif tracking-tight">
            Everything You Need to Work Smarter
          </h2>
          <p className="text-gray-600 text-sm md:text-base">
            We empower skilled agricultural drivers and equipment handlers with modern tools, secure earnings, and steady work.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {benefits.map((item, idx) => (
            <div
              key={idx}
              className="bg-[#FDFBF7] p-8 rounded-3xl border border-amber-100/80 hover:border-emerald-500/40 hover:shadow-lg transition-all duration-300 group flex flex-col justify-between space-y-4"
            >
              <div className="space-y-4">
                <div className="w-14 h-14 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center text-2xl group-hover:scale-110 group-hover:bg-emerald-100 transition-all duration-200 shadow-sm">
                  {item.icon}
                </div>
                <h3 className="text-xl font-black text-emerald-950">
                  {item.title}
                </h3>
                <p className="text-gray-600 text-sm leading-relaxed">
                  {item.description}
                </p>
              </div>

              <div className="pt-2 flex items-center text-xs font-bold text-emerald-800 gap-1 group-hover:translate-x-1 transition-transform">
                <span>Learn more</span>
                <span>→</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default OperatorBenefits;
