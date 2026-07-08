import axiosClient from './axiosClient';
import type { Application } from '../types/application';
import type { PagedResponse } from '../types/common';

export const applicationApi = {
  getMyApplications: (page = 0, size = 20) =>
    axiosClient
      .get<PagedResponse<Application>>(`/students/applications?page=${page}&size=${size}`)
      .then((r) => r.data),

  apply: (jobId: string, resumeId: string) =>
    axiosClient
      .post<Application>('/applications', { jobId, resumeId })
      .then((r) => r.data),
};
