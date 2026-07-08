import axiosClient from './axiosClient';
import type { Interview } from '../types/interview';
import type { PagedResponse } from '../types/common';

export const interviewApi = {
  getMyInterviews: (page = 0, size = 50) =>
    axiosClient
      .get<PagedResponse<Interview>>(`/students/interviews?page=${page}&size=${size}`)
      .then((r) => r.data),
};
