import axiosClient from './axiosClient';
import type { UserSummary, PlacementStats, CompanyStats, DepartmentStats, AuditLog, AuditAction } from '../types/admin';
import type { RecruiterProfile } from '../types/recruiter';
import type { Company } from '../types/recruiter';
import type { JobSummary, Job } from '../types/job';
import type { Application, ApplicationStatus } from '../types/application';
import type { Interview } from '../types/interview';
import type { PagedResponse } from '../types/common';

export const adminApi = {
  // ── Analytics (wrapped in ApiResponse<T>) ────────────────────────────────

  getPlacementStats: () =>
    axiosClient
      .get<{ success: boolean; data: PlacementStats }>('/analytics/placement-stats')
      .then((r) => r.data.data),

  getCompanyBreakdown: () =>
    axiosClient
      .get<{ success: boolean; data: CompanyStats[] }>('/analytics/companies')
      .then((r) => r.data.data),

  getDepartmentBreakdown: () =>
    axiosClient
      .get<{ success: boolean; data: DepartmentStats[] }>('/analytics/departments')
      .then((r) => r.data.data),

  // ── Users (raw PagedResponse) ─────────────────────────────────────────────

  searchUsers: (params: {
    email?: string;
    role?: string;
    isActive?: boolean;
    page?: number;
    size?: number;
  }) =>
    axiosClient
      .get<PagedResponse<UserSummary>>('/admin/users', { params: { ...params, size: params.size ?? 20 } })
      .then((r) => r.data),

  getUserById: (userId: string) =>
    axiosClient.get<UserSummary>(`/admin/users/${userId}`).then((r) => r.data),

  updateUserStatus: (userId: string, isActive: boolean) =>
    axiosClient
      .patch<UserSummary>(`/admin/users/${userId}/status`, { isActive })
      .then((r) => r.data),

  // ── Recruiters (raw PagedResponse) ───────────────────────────────────────

  getPendingRecruiters: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<RecruiterProfile>>(`/admin/recruiters/pending?page=${page}&size=${size}`)
      .then((r) => r.data),

  verifyRecruiter: (recruiterId: string, decision: 'APPROVE' | 'REJECT', rejectionReason?: string) =>
    axiosClient
      .patch<RecruiterProfile>(`/admin/recruiters/${recruiterId}/verify`, { decision, rejectionReason })
      .then((r) => r.data),

  // ── Companies (raw PagedResponse) ────────────────────────────────────────

  getPendingCompanies: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<Company>>(`/admin/companies/pending?page=${page}&size=${size}`)
      .then((r) => r.data),

  verifyCompany: (companyId: string, decision: 'APPROVE' | 'REJECT', rejectionReason?: string) =>
    axiosClient
      .patch<Company>(`/admin/companies/${companyId}/verify`, { decision, rejectionReason })
      .then((r) => r.data),

  // ── Jobs (raw PagedResponse<JobSummaryResponse>) ──────────────────────────

  getPendingJobs: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<JobSummary>>(`/admin/jobs/pending?page=${page}&size=${size}`)
      .then((r) => r.data),

  approveJob: (jobId: string, decision: 'APPROVE' | 'REJECT', rejectionReason?: string) =>
    axiosClient
      .patch<Job>(`/admin/jobs/${jobId}/approve`, { decision, rejectionReason })
      .then((r) => r.data),

  // ── Applications (raw PagedResponse) ─────────────────────────────────────

  getAllApplications: (params: {
    status?: ApplicationStatus;
    page?: number;
    size?: number;
  }) =>
    axiosClient
      .get<PagedResponse<Application>>('/admin/applications', {
        params: { ...params, size: params.size ?? 20 },
      })
      .then((r) => r.data),

  // ── Interviews (raw PagedResponse) ───────────────────────────────────────

  getAllInterviews: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<Interview>>(`/admin/interviews?page=${page}&size=${size}`)
      .then((r) => r.data),

  // ── Audit log (ApiResponse<PagedResponse<AuditLog>>) ─────────────────────

  getAuditLog: (params: {
    entityType?: string;
    actorId?: string;
    action?: AuditAction;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }) =>
    axiosClient
      .get<{ success: boolean; data: PagedResponse<AuditLog> }>('/admin/audit-log', {
        params: { ...params, size: params.size ?? 20, sort: 'createdAt,desc' },
      })
      .then((r) => r.data.data),
};
