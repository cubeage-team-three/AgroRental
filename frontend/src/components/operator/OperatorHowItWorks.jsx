function OperatorHowItWorks() {
  const steps = [
    {
      num: "01",
      title: "Register Online",
      desc: "Sign up with your basic contact info, driving experience, and machinery skills.",
    },
    {
      num: "02",
      title: "Verify Mobile",
      desc: "Complete the 6-digit secure SMS OTP verification to protect your account.",
    },
    {
      num: "03",
      title: "Upload Documents",
      desc: "Submit your Aadhaar card and commercial or tractor driving license for KYC.",
    },
    {
      num: "04",
      title: "Get Approved",
      desc: "Once verified by compliance, your profile is activated for work dispatch.",
    },
    {
      num: "05",
      title: "Receive Jobs",
      desc: "Review assigned field jobs with full machinery, location, and payout details.",
    },
    {
      num: "06",
      title: "Work & Earn",
      desc: "Accept the job, complete field operations, and receive direct payout settlements.",
    },
  ];

  return (
    <section className="py-16 md:py-24 bg-[#FDFBF7]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-16">
        <div className="text-center max-w-2xl mx-auto space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-emerald-50 text-emerald-800 rounded-full text-xs font-bold uppercase tracking-wider border border-emerald-200">
            <span>⚡</span>
            <span>Simple 6-Step Workflow</span>
          </div>
          <h2 className="text-3xl md:text-4xl font-extrabold text-emerald-950 font-serif tracking-tight">
            How It Works
          </h2>
          <p className="text-gray-600 text-sm md:text-base">
            From initial registration to direct field payouts — here is how skilled operators thrive on Agro Rental.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
          {steps.map((step, idx) => (
            <div
              key={idx}
              className="bg-white p-8 rounded-3xl border border-amber-100/80 shadow-sm hover:shadow-md hover:border-emerald-400/50 transition-all duration-300 relative space-y-4"
            >
              <div className="flex items-center justify-between">
                <span className="w-12 h-12 rounded-2xl bg-gradient-to-br from-emerald-800 to-green-700 text-white font-black text-lg flex items-center justify-center shadow-md">
                  {step.num}
                </span>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-widest">
                  Step {step.num}
                </span>
              </div>

              <div className="space-y-2">
                <h3 className="text-lg font-black text-emerald-950">
                  {step.title}
                </h3>
                <p className="text-gray-600 text-xs md:text-sm leading-relaxed">
                  {step.desc}
                </p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export default OperatorHowItWorks;
