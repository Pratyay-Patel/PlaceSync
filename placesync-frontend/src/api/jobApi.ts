import axiosClient from './axiosClient';
import type { Job, JobSummary, JobFilters } from '../types/job';
import type { PagedResponse } from '../types/common';

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
};
