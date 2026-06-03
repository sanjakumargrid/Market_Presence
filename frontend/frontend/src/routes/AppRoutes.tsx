import { Routes, Route, Navigate } from 'react-router-dom';
import { CandidateLayout } from '../layouts/CandidateLayout';
import { HomePage } from '../pages/HomePage';
import { JobsPage } from '../pages/JobsPage';
import { JobDetailsPage } from '../pages/JobDetailsPage';
import { ApplicationPage } from '../pages/ApplicationPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { ProfilePage } from '../pages/ProfilePage';
import { LoginPage } from '../pages/LoginPage';
import { RegisterPage } from '../pages/RegisterPage';
import { AdminJobsPage } from '../pages/admin/AdminJobsPage';
import { AdminJobEditorPage } from '../pages/admin/AdminJobEditorPage';
import { AdminHandoffsPage } from '../pages/admin/AdminHandoffsPage';

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  return <>{children}</>;
};

export const AppRoutes = () => (
  <Routes>
    {/* Main app with sidebar layout */}
    <Route element={<CandidateLayout />}>
      <Route path="/" element={<HomePage />} />

      {/* Legacy /jobs routes — kept for backward compatibility */}
      <Route path="/jobs" element={<JobsPage />} />
      <Route path="/jobs/:slug" element={<JobDetailsPage />} />
      <Route path="/jobs/:slug/apply" element={
        <ProtectedRoute><ApplicationPage /></ProtectedRoute>
      } />

      {/* REQ-JP-09 — canonical /careers routes (PES URL structure) */}
      <Route path="/careers" element={<Navigate to="/jobs" replace />} />
      <Route path="/careers/:slug" element={<JobDetailsPage />} />
      <Route path="/careers/:slug/apply" element={
        <ProtectedRoute><ApplicationPage /></ProtectedRoute>
      } />

      <Route path="/applications" element={
        <ProtectedRoute><ApplicationsPage /></ProtectedRoute>
      } />
      <Route path="/profile" element={
        <ProtectedRoute><ProfilePage /></ProtectedRoute>
      } />

      {/* REQ-JP-02 — Admin / recruiter job posting editor */}
      <Route path="/admin" element={<Navigate to="/admin/jobs" replace />} />
      <Route path="/admin/jobs" element={<AdminJobsPage />} />
      <Route path="/admin/jobs/new" element={<AdminJobEditorPage />} />
      <Route path="/admin/jobs/:id/edit" element={<AdminJobEditorPage />} />
      <Route path="/admin/handoffs" element={<AdminHandoffsPage />} />
    </Route>

    <Route path="/login" element={<LoginPage />} />
    <Route path="/register" element={<RegisterPage />} />

    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
);
