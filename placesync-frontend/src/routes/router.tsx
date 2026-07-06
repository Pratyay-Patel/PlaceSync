import { createBrowserRouter } from 'react-router-dom';
import RootLayout from '../components/layout/RootLayout';
import RoleRoute from './RoleRoute';

import PublicLanding from '../pages/PublicLanding';
import ForbiddenPage from '../pages/common/ForbiddenPage';

import LoginPage from '../pages/auth/LoginPage';
import RegisterPage from '../pages/auth/RegisterPage';
import ForgotPasswordPage from '../pages/auth/ForgotPasswordPage';
import ResetPasswordPage from '../pages/auth/ResetPasswordPage';
import EmailVerificationPage from '../pages/auth/EmailVerificationPage';

import StudentDashboard from '../pages/student/StudentDashboard';
import StudentProfilePage from '../pages/student/ProfilePage';
import ResumesPage from '../pages/student/ResumesPage';
import StudentJobsPage from '../pages/student/JobsPage';
import JobDetailPage from '../pages/student/JobDetailPage';
import StudentApplicationsPage from '../pages/student/ApplicationsPage';
import InterviewsPage from '../pages/student/InterviewsPage';

import RecruiterDashboard from '../pages/recruiter/RecruiterDashboard';
import RecruiterProfilePage from '../pages/recruiter/ProfilePage';
import RecruiterJobsPage from '../pages/recruiter/JobsPage';
import CreateJobPage from '../pages/recruiter/CreateJobPage';
import RecruiterApplicationsPage from '../pages/recruiter/ApplicationsPage';
import ScheduleInterviewPage from '../pages/recruiter/ScheduleInterviewPage';

import AdminDashboard from '../pages/admin/AdminDashboard';
import UsersPage from '../pages/admin/UsersPage';
import UserDetailPage from '../pages/admin/UserDetailPage';
import RecruitersPage from '../pages/admin/RecruitersPage';
import CompaniesPage from '../pages/admin/CompaniesPage';
import AdminJobsPage from '../pages/admin/JobsPage';
import AdminApplicationsPage from '../pages/admin/ApplicationsPage';
import AdminInterviewsPage from '../pages/admin/InterviewsPage';
import AuditLogPage from '../pages/admin/AuditLogPage';
import AnalyticsPage from '../pages/admin/AnalyticsPage';

const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: <PublicLanding /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
      { path: '/reset-password', element: <ResetPasswordPage /> },
      { path: '/verify-email', element: <EmailVerificationPage /> },
      { path: '/403', element: <ForbiddenPage /> },

      // Student routes
      {
        element: <RoleRoute allowedRoles={['ROLE_STUDENT']} />,
        children: [
          { path: '/student/dashboard', element: <StudentDashboard /> },
          { path: '/student/profile', element: <StudentProfilePage /> },
          { path: '/student/resumes', element: <ResumesPage /> },
          { path: '/student/jobs', element: <StudentJobsPage /> },
          { path: '/student/jobs/:jobId', element: <JobDetailPage /> },
          { path: '/student/applications', element: <StudentApplicationsPage /> },
          { path: '/student/interviews', element: <InterviewsPage /> },
        ],
      },

      // Recruiter routes
      {
        element: <RoleRoute allowedRoles={['ROLE_RECRUITER']} />,
        children: [
          { path: '/recruiter/dashboard', element: <RecruiterDashboard /> },
          { path: '/recruiter/profile', element: <RecruiterProfilePage /> },
          { path: '/recruiter/jobs', element: <RecruiterJobsPage /> },
          { path: '/recruiter/jobs/create', element: <CreateJobPage /> },
          { path: '/recruiter/jobs/:jobId/edit', element: <RecruiterJobsPage /> },
          { path: '/recruiter/jobs/:jobId/applications', element: <RecruiterApplicationsPage /> },
          { path: '/recruiter/jobs/:jobId/applications/:applicationId', element: <ScheduleInterviewPage /> },
        ],
      },

      // Admin routes
      {
        element: <RoleRoute allowedRoles={['ROLE_ADMIN']} />,
        children: [
          { path: '/admin/dashboard', element: <AdminDashboard /> },
          { path: '/admin/users', element: <UsersPage /> },
          { path: '/admin/users/:userId', element: <UserDetailPage /> },
          { path: '/admin/recruiters/pending', element: <RecruitersPage /> },
          { path: '/admin/companies/pending', element: <CompaniesPage /> },
          { path: '/admin/jobs/pending', element: <AdminJobsPage /> },
          { path: '/admin/applications', element: <AdminApplicationsPage /> },
          { path: '/admin/interviews', element: <AdminInterviewsPage /> },
          { path: '/admin/analytics', element: <AnalyticsPage /> },
          { path: '/admin/audit-log', element: <AuditLogPage /> },
        ],
      },
    ],
  },
]);

export default router;
