export type UserRole = 'ROLE_STUDENT' | 'ROLE_RECRUITER' | 'ROLE_ADMIN';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userId: string;
  email: string;
  role: UserRole;
}
