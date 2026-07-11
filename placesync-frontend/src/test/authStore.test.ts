import { useAuthStore } from '../store/authStore';
import type { AuthResponse } from '../types/auth';

const AUTH_RESPONSE: AuthResponse = {
  accessToken: 'access-token-123',
  refreshToken: 'refresh-token-abc',
  expiresIn: 3600,
  userId: 'user-1',
  email: 'student@test.com',
  role: 'ROLE_STUDENT',
};

beforeEach(() => {
  localStorage.clear();
  useAuthStore.setState({
    accessToken: null,
    userId: null,
    email: null,
    role: null,
    isAuthenticated: false,
  });
});

describe('authStore.login', () => {
  it('sets authenticated state', () => {
    useAuthStore.getState().login(AUTH_RESPONSE);
    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.accessToken).toBe('access-token-123');
    expect(state.userId).toBe('user-1');
    expect(state.email).toBe('student@test.com');
    expect(state.role).toBe('ROLE_STUDENT');
  });

  it('persists refreshToken to localStorage', () => {
    useAuthStore.getState().login(AUTH_RESPONSE);
    expect(localStorage.getItem('refreshToken')).toBe('refresh-token-abc');
  });
});

describe('authStore.logout', () => {
  it('clears authenticated state', () => {
    useAuthStore.getState().login(AUTH_RESPONSE);
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.accessToken).toBeNull();
    expect(state.userId).toBeNull();
    expect(state.email).toBeNull();
    expect(state.role).toBeNull();
  });

  it('removes refreshToken from localStorage', () => {
    useAuthStore.getState().login(AUTH_RESPONSE);
    useAuthStore.getState().logout();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });
});
