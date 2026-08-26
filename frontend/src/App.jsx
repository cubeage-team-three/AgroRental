import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { LanguageProvider } from './context/LanguageContext';
import { AuthProvider } from './context/AuthContext';
import LanguageSelector from './components/common/LanguageSelector';

function App() {
  return (
    <AuthProvider>
      <LanguageProvider>
        <BrowserRouter>
          <AppRoutes />
          <LanguageSelector />
        </BrowserRouter>
      </LanguageProvider>
    </AuthProvider>
  );
}

export default App;
