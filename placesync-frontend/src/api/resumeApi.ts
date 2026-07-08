import axiosClient from './axiosClient';
import type { Resume } from '../types/student';

export const resumeApi = {
  list: () =>
    axiosClient.get<Resume[]>('/students/resumes').then((r) => r.data),

  upload: (file: File, label: string, isDefault = false) => {
    const form = new FormData();
    form.append('file', file);
    return axiosClient
      .post<Resume>(
        `/students/resumes?label=${encodeURIComponent(label)}&isDefault=${isDefault}`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      )
      .then((r) => r.data);
  },

  setDefault: (resumeId: string) =>
    axiosClient.patch<Resume>(`/students/resumes/${resumeId}/default`).then((r) => r.data),

  delete: (resumeId: string) =>
    axiosClient.delete(`/students/resumes/${resumeId}`),

  getDownloadUrl: (resumeId: string) =>
    axiosClient
      .get<{ downloadUrl: string; expiresAt: string }>(`/students/resumes/${resumeId}/url`)
      .then((r) => r.data),
};
