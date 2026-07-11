import axiosClient from './axiosClient';
import type {
  StudentProfile,
  UpdateProfileRequest,
  Skill,
  Education,
  EducationRequest,
  Experience,
  ExperienceRequest,
} from '../types/student';

export const studentApi = {
  getProfile: () =>
    axiosClient.get<StudentProfile>('/students/profile').then((r) => r.data),

  updateProfile: (data: UpdateProfileRequest) =>
    axiosClient.put<StudentProfile>('/students/profile', data).then((r) => r.data),

  uploadPicture: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return axiosClient
      .patch<StudentProfile>('/students/profile/picture', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },

  getSkills: () =>
    axiosClient.get<Skill[]>('/students/profile/skills').then((r) => r.data),

  addSkill: (skillName: string) =>
    axiosClient.post('/students/profile/skills', { skillName }).then((r) => r.data),

  removeSkill: (skillId: string) =>
    axiosClient.delete(`/students/profile/skills/${skillId}`),

  getEducation: () =>
    axiosClient.get<Education[]>('/students/profile/education').then((r) => r.data),

  addEducation: (data: EducationRequest) =>
    axiosClient.post<Education>('/students/profile/education', data).then((r) => r.data),

  updateEducation: (id: string, data: EducationRequest) =>
    axiosClient.put<Education>(`/students/profile/education/${id}`, data).then((r) => r.data),

  deleteEducation: (id: string) =>
    axiosClient.delete(`/students/profile/education/${id}`),

  getExperience: () =>
    axiosClient.get<Experience[]>('/students/profile/experience').then((r) => r.data),

  addExperience: (data: ExperienceRequest) =>
    axiosClient.post<Experience>('/students/profile/experience', data).then((r) => r.data),

  updateExperience: (id: string, data: ExperienceRequest) =>
    axiosClient.put<Experience>(`/students/profile/experience/${id}`, data).then((r) => r.data),

  deleteExperience: (id: string) =>
    axiosClient.delete(`/students/profile/experience/${id}`),
};
