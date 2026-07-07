import axios from 'axios';
import axiosClient from './axiosClient';
import type { AuthResponse } from '../types/auth';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  role: 'ROLE_STUDENT' | 'ROLE_RECRUITER';
  firstName: string;
  lastName: string;
  institution?: string;
  department?: string;
  graduationYear?: number;
}

export const authApi = {
  // Base axios used intentionally — axiosClient's 401 interceptor would
  // attempt a token refresh on bad credentials, causing an infinite loop.
  login: (data: LoginRequest) =>
    axios.post<AuthResponse>('/api/v1/auth/login', data).then((r) => r.data),

  register: (data: RegisterRequest) =>
    axios.post<AuthResponse>('/api/v1/auth/register', data).then((r) => r.data),

  logout: (refreshToken: string) =>
    axiosClient.post('/auth/logout', { refreshToken }),

  verifyEmail: (token: string) =>
    axios.get(`/api/v1/auth/verify-email?token=${token}`),

  forgotPassword: (email: string) =>
    axios.post('/api/v1/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    axios.post('/api/v1/auth/reset-password', { token, newPassword }),
};
