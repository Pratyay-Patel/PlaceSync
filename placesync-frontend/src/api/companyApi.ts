import axiosClient from './axiosClient';
import type { Company } from '../types/recruiter';
import type { PagedResponse } from '../types/common';

export const companyApi = {
  list: (page = 0, size = 100) =>
    axiosClient
      .get<PagedResponse<Company>>(`/companies?page=${page}&size=${size}`)
      .then((r) => r.data),
};
