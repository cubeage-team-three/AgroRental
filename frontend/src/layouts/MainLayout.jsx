import { Outlet } from 'react-router-dom';
import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import NewsTicker from '../components/layout/NewsTicker';

function MainLayout() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <NewsTicker />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}

export default MainLayout;
