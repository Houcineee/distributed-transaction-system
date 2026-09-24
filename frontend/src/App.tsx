import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { TransferPage } from "./pages/TransferPage";
import { SettingsPage } from "./pages/SettingsPage";
import { CreateAccountPage } from "./pages/CreateAccountPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          
          {/* Protected Routes */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/accounts/new" 
            element={
              <ProtectedRoute>
                <CreateAccountPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/transfers" 
            element={
              <ProtectedRoute>
                <TransferPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/settings" 
            element={
              <ProtectedRoute>
                <SettingsPage />
              </ProtectedRoute>
            } 
          />
          <Route
            path="/accounts/new"
            element={
              <ProtectedRoute>
                <CreateAccountPage />
              </ProtectedRoute>
            }
          />

          {/* Catch-all route to redirect to dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
