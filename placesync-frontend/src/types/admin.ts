export interface UserSummary {
  id: string;
  email: string;
  role: 'ROLE_STUDENT' | 'ROLE_RECRUITER' | 'ROLE_ADMIN';
  isActive: boolean;
  isEmailVerified: boolean;
  createdAt: string;
}

export interface PlacementStats {
  totalStudents: number;
  totalRecruiters: number;
  totalCompanies: number;
  openJobs: number;
  totalApplications: number;
  totalOffers: number;
  placementRate: number;
}

export interface CompanyStats {
  companyId: string;
  companyName: string;
  offerCount: number;
  jobCount: number;
  applicationCount: number;
}

export interface DepartmentStats {
  department: string;
  placedCount: number;
  totalStudents: number;
  placementRate: number;
}

export type AuditAction =
  | 'CREATE'
  | 'UPDATE'
  | 'DELETE'
  | 'SOFT_DELETE'
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'LOGOUT'
  | 'PASSWORD_CHANGE'
  | 'PASSWORD_RESET'
  | 'EMAIL_VERIFIED'
  | 'ACCOUNT_LOCKED'
  | 'ACCOUNT_UNLOCKED';

export interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: AuditAction;
  actorId: string;
  actorRole: string;
  actorEmail: string;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
}
