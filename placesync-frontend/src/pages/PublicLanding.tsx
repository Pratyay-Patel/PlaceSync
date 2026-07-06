import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import type { UserRole } from '../types/auth';

const ROLE_REDIRECTS: Record<UserRole, string> = {
  ROLE_STUDENT: '/student/dashboard',
  ROLE_RECRUITER: '/recruiter/dashboard',
  ROLE_ADMIN: '/admin/dashboard',
};

export default function PublicLanding() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const role = useAuthStore((s) => s.role);

  if (isAuthenticated && role) {
    return <Navigate to={ROLE_REDIRECTS[role]} replace />;
  }

  return <Navigate to="/login" replace />;
}
