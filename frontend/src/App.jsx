import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { LanguageProvider } from './context/LanguageContext';
import LanguageSelector from './components/common/LanguageSelector';

function App() {
  return (
    <LanguageProvider>
      <BrowserRouter>
        <AppRoutes />
        <LanguageSelector />
      </BrowserRouter>
    </LanguageProvider>
  );
}

export default App;
