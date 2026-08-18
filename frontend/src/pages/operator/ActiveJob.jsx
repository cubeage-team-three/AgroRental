import { useState } from "react";
import { Link } from "react-router-dom";

function ActiveJob() {
  const [currentStep, setCurrentStep] = useState(1); // 1: Accepted, 2: Started, 3: In Progress, 4: Completed

  const steps = [
    { id: 1, label: "Accepted", desc: "Deployment confirmed" },
    { id: 2, label: "Ready / Started", desc: "On-site arrival & engine check" },
    { id: 3, label: "In Progress", desc: "Active field operation" },
    { id: 4, label: "Completed", desc: "Duty finished & settlement ready" },
  ];

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">⚡</span>
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-800">
              Module 8 — Active Duty Progress
            </span>
          </div>
          <h1 className="text-2xl md:text-3xl font-black text-emerald-950 tracking-tight mt-1">
            Active Job Status & Duty Tracker
          </h1>
          <p className="text-xs md:text-sm text-gray-500 mt-1 max-w-xl">
            Real-time stage tracking for accepted field deployments. Update your work milestone from start to completion.
          </p>
        </div>

        <Link
          to="/operator/jobs"
          className="bg-emerald-50 hover:bg-emerald-100 text-emerald-900 text-xs font-bold px-4 py-2.5 rounded-xl border border-emerald-200 transition"
        >
          ← All Jobs
        </Link>
      </div>

      {/* Progress Timeline Card */}
      <div className="bg-white rounded-3xl p-6 md:p-8 border border-amber-100/70 shadow-sm space-y-8">
        <div className="flex items-center justify-between border-b border-amber-100/60 pb-4">
          <h2 className="text-base font-bold text-emerald-950 uppercase tracking-wider">
            Work Milestone Progress
          </h2>
          <span className="text-xs font-bold px-3 py-1 bg-emerald-100 text-emerald-900 rounded-full border border-emerald-300">
            Step {currentStep} of 4: {steps[currentStep - 1].label}
          </span>
        </div>

        {/* 4-Step Stepper */}
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
          {steps.map((step) => {
            const isDone = currentStep > step.id;
            const isCurrent = currentStep === step.id;

            return (
              <div
                key={step.id}
                onClick={() => setCurrentStep(step.id)}
                className={`cursor-pointer p-5 rounded-2xl border transition-all duration-200 flex flex-col justify-between space-y-3 ${
                  isCurrent
                    ? "bg-emerald-900 text-white border-emerald-900 shadow-md ring-2 ring-emerald-500/30"
                    : isDone
                    ? "bg-emerald-50 border-emerald-200 text-emerald-950"
                    : "bg-[#FAF8F5] border-amber-100/50 text-gray-400"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span
                    className={`w-8 h-8 rounded-xl font-black text-xs flex items-center justify-center ${
                      isCurrent
                        ? "bg-lime-400 text-emerald-950"
                        : isDone
                        ? "bg-emerald-600 text-white"
                        : "bg-gray-200 text-gray-600"
                    }`}
                  >
                    {isDone ? "✓" : step.id}
                  </span>
                  <span className="text-[10px] font-bold uppercase tracking-wider opacity-80">
                    Milestone {step.id}
                  </span>
                </div>

                <div>
                  <p className={`font-bold text-sm ${isCurrent ? "text-white" : "text-emerald-950"}`}>
                    {step.label}
                  </p>
                  <p className={`text-[11px] mt-0.5 ${isCurrent ? "text-emerald-200" : "text-gray-500"}`}>
                    {step.desc}
                  </p>
                </div>
              </div>
            );
          })}
        </div>

        {/* Milestone Action Panel */}
        <div className="bg-[#FAF8F5] p-6 rounded-2xl border border-amber-100/60 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-emerald-950">
              Current Duty Milestone: {steps[currentStep - 1].label}
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">
              Click next to update dispatchers and farm clients regarding your real-time status.
            </p>
          </div>

          <div className="flex gap-2.5">
            {currentStep > 1 && (
              <button
                type="button"
                onClick={() => setCurrentStep((prev) => Math.max(1, prev - 1))}
                className="px-4 py-2.5 rounded-xl text-xs font-bold bg-gray-200 hover:bg-gray-300 text-gray-800 transition"
              >
                ← Previous
              </button>
            )}
            {currentStep < 4 ? (
              <button
                type="button"
                onClick={() => setCurrentStep((prev) => Math.min(4, prev + 1))}
                className="px-5 py-2.5 rounded-xl text-xs font-bold bg-emerald-800 hover:bg-emerald-900 text-white transition shadow"
              >
                Advance to {steps[currentStep].label} →
              </button>
            ) : (
              <span className="px-4 py-2.5 rounded-xl text-xs font-bold bg-blue-100 text-blue-900 border border-blue-300">
                🎉 Work Completed
              </span>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ActiveJob;
