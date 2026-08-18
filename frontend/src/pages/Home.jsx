import HeroSection from "../components/home/HeroSection";
import StatsBanner from "../components/home/StatsBanner";
import ServicesGrid from "../components/home/ServicesGrid";
import BookingSteps from "../components/home/BookingSteps";
import RoleEcosystem from "../components/home/RoleEcosystem";
import WhatsAppBooking from "../components/home/WhatsAppBooking";
import PricingCards from "../components/home/PricingCards";
import PlatformCapabilities from "../components/home/PlatformCapabilities";
import FAQSection from "../components/home/FAQSection";
import CallToAction from "../components/home/CallToAction";
import { Reveal } from "../components/motion/Reveal";

/**
 * Hero and StatsBanner animate on mount / on their own (parallax + counters),
 * so they are intentionally left unwrapped. Everything below the fold gets a
 * scroll-triggered reveal as it enters the viewport.
 */
const belowTheFold = [
  ServicesGrid,
  BookingSteps,
  RoleEcosystem,
  WhatsAppBooking,
  PricingCards,
  PlatformCapabilities,
  FAQSection,
  CallToAction,
];

function Home() {
  return (
    <div>
      <HeroSection />
      <StatsBanner />

      {belowTheFold.map((Section) => (
        <Reveal key={Section.name} amount={0.12} duration={0.8}>
          <Section />
        </Reveal>
      ))}
    </div>
  );
}

export default Home;
