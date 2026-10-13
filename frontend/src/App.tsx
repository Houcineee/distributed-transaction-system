import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LandingPage } from "./pages/LandingPage";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { TransferPage } from "./pages/TransferPage";
import { SettingsPage } from "./pages/SettingsPage";
import { CreateAccountPage } from "./pages/CreateAccountPage";
import { AdminLayout } from "./layouts/AdminLayout";
import { AdminAccountsPage } from "./pages/admin/AdminAccountsPage";
import { AdminAuditPage } from "./pages/admin/AdminAuditPage";
import { AdminHealthPage } from "./pages/admin/AdminHealthPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          
          {/* Customer Protected Routes (Accessible only if NOT an admin) */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <DashboardPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/accounts/new" 
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <CreateAccountPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/transfers" 
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <TransferPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/settings" 
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <SettingsPage />
              </ProtectedRoute>
            } 
          />

          {/* Admin Protected Routes (Accessible only to ADMIN users) */}
          <Route
            path="/admin"
            element={
              <ProtectedRoute requireRole="ADMIN">
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<AdminAccountsPage />} />
            <Route path="accounts" element={<AdminAccountsPage />} />
            <Route path="audit" element={<AdminAuditPage />} />
            <Route path="health" element={<AdminHealthPage />} />
          </Route>

          {/* Catch-all route */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
