import axiosClient from './axiosClient';
import type { RecruiterProfile, UpdateRecruiterProfileRequest, RecruiterStats } from '../types/recruiter';
import type { JobSummary } from '../types/job';
import type { Application, ApplicationStatus } from '../types/application';
import type { Interview } from '../types/interview';
import type { PagedResponse } from '../types/common';

export const recruiterApi = {
  getProfile: () =>
    axiosClient.get<RecruiterProfile>('/recruiters/profile').then((r) => r.data),

  updateProfile: (data: UpdateRecruiterProfileRequest) =>
    axiosClient.put<RecruiterProfile>('/recruiters/profile', data).then((r) => r.data),

  getMyJobs: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<JobSummary>>(`/recruiters/jobs?page=${page}&size=${size}`)
      .then((r) => r.data),

  getStats: () =>
    axiosClient.get<RecruiterStats>('/analytics/recruiter-stats').then((r) => r.data),

  getJobApplications: (jobId: string, page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<Application>>(`/recruiters/jobs/${jobId}/applications?page=${page}&size=${size}`)
      .then((r) => r.data),

  updateApplicationStatus: (applicationId: string, status: ApplicationStatus, note?: string) =>
    axiosClient
      .patch<Application>(`/recruiters/applications/${applicationId}/status`, { status, note })
      .then((r) => r.data),

  getApplicationInterviews: (applicationId: string) =>
    axiosClient
      .get<Interview[]>(`/recruiters/applications/${applicationId}/interviews`)
      .then((r) => r.data),

  scheduleInterview: (
    applicationId: string,
    data: {
      roundNumber: number;
      interviewType: string;
      scheduledAt: string;
      durationMinutes: number;
      meetingLink?: string;
      venue?: string;
    }
  ) =>
    axiosClient
      .post<Interview>(`/recruiters/applications/${applicationId}/interviews`, data)
      .then((r) => r.data),

  rescheduleInterview: (
    interviewId: string,
    data: { scheduledAt: string; meetingLink?: string; venue?: string }
  ) =>
    axiosClient
      .put<Interview>(`/recruiters/interviews/${interviewId}`, data)
      .then((r) => r.data),

  cancelInterview: (interviewId: string, cancellationReason: string) =>
    axiosClient
      .patch<Interview>(`/recruiters/interviews/${interviewId}/cancel`, { cancellationReason })
      .then((r) => r.data),

  completeInterview: (interviewId: string) =>
    axiosClient
      .patch<Interview>(`/recruiters/interviews/${interviewId}/complete`, {})
      .then((r) => r.data),

  getResumeDownloadUrl: (resumeId: string) =>
    axiosClient
      .get<{ downloadUrl: string; expiresAt: string }>(`/students/resumes/${resumeId}/url`)
      .then((r) => r.data),
};
