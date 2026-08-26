import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { LanguageProvider } from './context/LanguageContext';
import { AuthProvider } from './context/AuthContext';
import LanguageSelector from './components/common/LanguageSelector';

function App() {
  return (
    <LanguageProvider>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
          <LanguageSelector />
        </BrowserRouter>
      </AuthProvider>
    </LanguageProvider>
  );
}

export default App;
