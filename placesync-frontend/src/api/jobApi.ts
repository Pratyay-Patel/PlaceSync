import axiosClient from './axiosClient';
import type { Job, JobSummary, JobFilters } from '../types/job';
import type { PagedResponse } from '../types/common';

export interface JobFormData {
  title: string;
  description: string;
  locationType: string;
  jobType: string;
  locationCity?: string;
  compensation?: string;
  applicationDeadline: string;
  minCgpa?: number | null;
  requiredSkills: string[];
  eligibleDepartments: string[];
}

export const jobApi = {
  list: (filters: JobFilters = {}, page = 0, size = 12) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (filters.keyword) params.set('keyword', filters.keyword);
    if (filters.locationType) params.set('locationType', filters.locationType);
    if (filters.jobType) params.set('jobType', filters.jobType);
    return axiosClient
      .get<PagedResponse<JobSummary>>(`/jobs?${params.toString()}`)
      .then((r) => r.data);
  },

  getById: (jobId: string) =>
    axiosClient.get<Job>(`/jobs/${jobId}`).then((r) => r.data),

  create: (data: JobFormData) =>
    axiosClient.post<Job>('/jobs', data).then((r) => r.data),

  update: (jobId: string, data: JobFormData) =>
    axiosClient.put<Job>(`/jobs/${jobId}`, data).then((r) => r.data),

  close: (jobId: string) =>
    axiosClient.patch<Job>(`/jobs/${jobId}/close`, {}).then((r) => r.data),
};
