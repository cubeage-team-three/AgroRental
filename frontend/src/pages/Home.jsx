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

function Home() {
  return (
    <div>
      <HeroSection />
      <StatsBanner />
      <ServicesGrid />
      <BookingSteps />
      <RoleEcosystem />
      <WhatsAppBooking />
      <PricingCards />
      <PlatformCapabilities />
      {/* Testimonials — pending */}
      <FAQSection />
      <CallToAction />
    </div>
  );
}

export default Home;
