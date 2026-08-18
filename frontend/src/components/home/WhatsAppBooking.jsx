import { MessageCircle, Headphones, Send, Phone, Sprout, CheckCheck } from "lucide-react";

const steps = [
  {
    icon: MessageCircle,
    title: "Send a WhatsApp Request",
    description: "Describe the service, crop, and field location in your language.",
  },
  {
    icon: Headphones,
    title: "Agent Creates the Booking",
    description:
      "Support admin logs it as Assisted Booking. OTP confirmation before finalizing.",
  },
  {
    icon: Send,
    title: "Receive Tracking Link",
    description:
      "A browser-based live tracking link sent to your WhatsApp. No login required.",
  },
  {
    icon: Phone,
    title: "Updates by Call/WhatsApp",
    description:
      "ETA, work start, completion, and payment messages all via WhatsApp.",
  },
];

const chatMessages = [
  {
    from: "user",
    text: "Namaste, mujhe 5 acre mein tractor chahiye kal",
    time: "09:12",
  },
  {
    from: "support",
    text: "Namaskar! Aapka gaon/district kya hai? Crop type?",
    time: "09:12",
  },
  { from: "user", text: "Rampur, Bareilly. Gehu ke liye", time: "09:13" },
  {
    from: "support",
    text: "✅ Booking confirmed! OTP: 4821. Ram Singh assigned (⭐ 4.9).",
    time: "09:14",
  },
  {
    from: "support",
    text: "🚜 Live tracking: agrorent.app/track/BK2847",
    time: "09:14",
  },
];

function WhatsAppBooking() {
  return (
    <section className="relative isolate overflow-hidden bg-emerald-950">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_80%_10%,rgba(132,204,22,0.16),transparent_55%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_10%_90%,rgba(16,185,129,0.16),transparent_55%)]" />

      <div className="relative mx-auto max-w-7xl px-6 py-24">
        <div className="grid grid-cols-1 items-center gap-14 lg:grid-cols-2">
          {/* Narrative */}
          <div>
            <span className="inline-flex items-center rounded-full border border-lime-300/30 bg-lime-400/10 px-4 py-1.5 text-xs font-semibold uppercase tracking-widest text-lime-300 backdrop-blur-md">
              WhatsApp Assisted Booking
            </span>
            <h2 className="mt-5 font-display text-4xl font-bold tracking-tight text-white sm:text-5xl">
              No Smartphone?{" "}
              <span className="bg-gradient-to-r from-lime-300 to-emerald-300 bg-clip-text text-transparent">
                No Problem.
              </span>
            </h2>
            <p className="mt-5 max-w-lg text-lg leading-relaxed text-white/65">
              Farmers who can&apos;t operate a website can send a WhatsApp
              message. Agents create the booking, confirm via OTP, and send
              live tracking links right to WhatsApp.
            </p>

            <div className="mt-10 space-y-3">
              {steps.map(({ icon: Icon, title, description }) => (
                <div
                  key={title}
                  className="group flex items-start gap-4 rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur-xl transition-all duration-300 ease-out hover:-translate-y-1 hover:border-lime-300/30 hover:bg-white/10"
                >
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border border-white/10 bg-gradient-to-br from-emerald-500/30 to-lime-400/20 text-lime-300 transition-all duration-300 ease-out group-hover:scale-110">
                    <Icon className="h-5 w-5" />
                  </span>
                  <div>
                    <p className="font-semibold text-white">{title}</p>
                    <p className="mt-0.5 text-sm leading-relaxed text-white/60 transition-colors duration-300 ease-out group-hover:text-white/75">
                      {description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Chat device mockup */}
          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute -inset-6 rounded-[3rem] bg-gradient-to-tr from-emerald-500/20 to-lime-400/20 blur-3xl"
            />

            <div className="relative overflow-hidden rounded-[2rem] border border-white/15 bg-white/10 shadow-2xl shadow-emerald-950/60 backdrop-blur-2xl transition-all duration-300 ease-out hover:-translate-y-1">
              {/* Header */}
              <div className="flex items-center gap-3 border-b border-white/10 bg-gradient-to-r from-emerald-800 to-emerald-700 px-5 py-4">
                <span className="flex h-11 w-11 items-center justify-center rounded-full bg-lime-400/20 text-lime-300 ring-1 ring-lime-300/30">
                  <Sprout className="h-5 w-5" />
                </span>
                <div className="flex-1">
                  <p className="font-semibold text-white">AgroRent Support</p>
                  <p className="flex items-center gap-1.5 text-xs text-white/70">
                    <span className="h-1.5 w-1.5 rounded-full bg-lime-400 shadow-[0_0_8px_rgba(132,204,22,0.9)]" />
                    +91 98765 43210 · Online
                  </p>
                </div>
                <Phone className="h-4 w-4 text-white/50" />
              </div>

              {/* Transcript */}
              <div className="space-y-3 bg-emerald-950/40 p-5">
                {chatMessages.map((message, index) => {
                  const isUser = message.from === "user";
                  return (
                    <div
                      key={index}
                      className={`flex ${isUser ? "justify-end" : "justify-start"}`}
                    >
                      <div
                        className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm shadow-lg backdrop-blur-md transition-all duration-300 ease-out hover:-translate-y-0.5 ${
                          isUser
                            ? "rounded-br-md bg-gradient-to-br from-emerald-600 to-emerald-700 text-white"
                            : "rounded-bl-md border border-white/10 bg-white/10 text-white/90"
                        }`}
                      >
                        <p className="leading-relaxed">{message.text}</p>
                        <span
                          className={`mt-1 flex items-center justify-end gap-1 text-[10px] ${
                            isUser ? "text-white/60" : "text-white/40"
                          }`}
                        >
                          {message.time}
                          {isUser && (
                            <CheckCheck className="h-3 w-3 text-lime-300" />
                          )}
                        </span>
                      </div>
                    </div>
                  );
                })}

                {/* Typing indicator */}
                <div className="flex justify-start">
                  <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-md border border-white/10 bg-white/10 px-4 py-3">
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-lime-300" />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-lime-300 [animation-delay:150ms]" />
                    <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-lime-300 [animation-delay:300ms]" />
                  </div>
                </div>
              </div>
            </div>

            {/* Floating badge */}
            <span className="absolute -right-3 -top-3 flex h-14 w-14 items-center justify-center rounded-full bg-lime-400 text-emerald-950 shadow-[0_0_25px_rgba(132,204,22,0.6)] transition-all duration-300 ease-out hover:scale-110">
              <MessageCircle className="h-6 w-6" />
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}

export default WhatsAppBooking;
