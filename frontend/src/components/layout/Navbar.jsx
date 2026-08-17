import { Link } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';

function Navbar() {
  const { t } = useLanguage();
  return (
    <nav className="bg-green-700 text-white px-6 py-4 flex items-center justify-between">
      <Link to="/" className="text-xl font-bold">
        {t('app_title')}
      </Link>
      <div className="flex gap-4">
        <Link to="/" className="hover:text-green-200">
          {t('home')}
        </Link>
        <Link to="/login" className="hover:text-green-200">
          {t('login')}
        </Link>
        <Link to="/register" className="hover:text-green-200">
          {t('register')}
        </Link>
      </div>
    </nav>
  );
}

export default Navbar;
