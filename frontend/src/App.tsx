import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { NotFoundPage } from './pages/NotFoundPage';

// Pages (lazily referenced; implementations come in later waves)
import { LoginPage } from './pages/LoginPage';
import { LaboratoryListPage } from './pages/LaboratoryListPage';
import { LaboratoryDetailPage } from './pages/LaboratoryDetailPage';

/**
 * Main application router.
 * - /login         → public
 * - /laboratories  → protected (requires authentication)
 * - /laboratories/:id → protected
 * - *              → NotFoundPage
 *
 * Requirements: 9.5, 10.1
 */
function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route path="/laboratories" element={<LaboratoryListPage />} />
          <Route path="/laboratories/:id" element={<LaboratoryDetailPage />} />
        </Route>

        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/laboratories" replace />} />

        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
