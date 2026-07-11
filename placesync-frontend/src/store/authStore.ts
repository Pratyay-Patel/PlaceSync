import { create } from 'zustand';
import type { AuthResponse, UserRole } from '../types/auth';

interface AuthState {
  accessToken: string | null;
  userId: string | null;
  email: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  login: (authResponse: AuthResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  userId: null,
  email: null,
  role: null,
  isAuthenticated: false,

  login: (authResponse) => {
    localStorage.setItem('refreshToken', authResponse.refreshToken);
    set({
      accessToken: authResponse.accessToken,
      userId: authResponse.userId,
      email: authResponse.email,
      role: authResponse.role,
      isAuthenticated: true,
    });
  },

  logout: () => {
    localStorage.removeItem('refreshToken');
    set({
      accessToken: null,
      userId: null,
      email: null,
      role: null,
      isAuthenticated: false,
    });
  },
}));
