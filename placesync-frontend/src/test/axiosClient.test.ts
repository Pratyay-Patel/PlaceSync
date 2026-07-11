import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import axiosClient from '../api/axiosClient';
import { queryClient } from '../lib/queryClient';
import { useAuthStore } from '../store/authStore';
import type { AuthResponse } from '../types/auth';

const originalAdapter = axiosClient.defaults.adapter;

function make401(config: InternalAxiosRequestConfig): AxiosError {
  return new AxiosError(
    'Request failed with status code 401',
    'ERR_BAD_RESPONSE',
    config,
    null,
    { status: 401, statusText: 'Unauthorized', data: {}, headers: {}, config },
  );
}

beforeEach(() => {
  localStorage.clear();
  useAuthStore.setState({
    accessToken: null,
    userId: null,
    email: null,
    role: null,
    isAuthenticated: false,
  });
  vi.restoreAllMocks();
  axiosClient.defaults.adapter = originalAdapter;
});

describe('axiosClient 401 interceptor', () => {
  it('calls refresh endpoint and replays request when refreshToken exists', async () => {
    localStorage.setItem('refreshToken', 'rt-valid');

    const refreshResponse: AuthResponse = {
      accessToken: 'new-access',
      refreshToken: 'rt-new',
      expiresIn: 3600,
      userId: 'u1',
      email: 'x@y.com',
      role: 'ROLE_STUDENT',
    };

    let requestCount = 0;
    axiosClient.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      requestCount++;
      if (requestCount === 1) throw make401(config);
      return { status: 200, statusText: 'OK', data: { ok: true }, headers: {}, config };
    };

    const refreshSpy = vi.spyOn(axios, 'post').mockResolvedValueOnce({ data: refreshResponse });

    const result = await axiosClient.get('/test');

    expect(refreshSpy).toHaveBeenCalledWith('/api/v1/auth/refresh', { refreshToken: 'rt-valid' });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().accessToken).toBe('new-access');
    expect(result.data).toEqual({ ok: true });
  });

  it('calls logout and redirects when refresh fails', async () => {
    localStorage.setItem('refreshToken', 'rt-valid');
    vi.stubGlobal('location', { href: '' });

    axiosClient.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      throw make401(config);
    };

    const refreshError = new Error('Refresh failed');
    vi.spyOn(axios, 'post').mockRejectedValueOnce(refreshError);
    const clearSpy = vi.spyOn(queryClient, 'clear').mockReturnValue(undefined);

    await expect(axiosClient.get('/test')).rejects.toBeDefined();

    expect(clearSpy).toHaveBeenCalled();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(window.location.href).toBe('/login');

    vi.unstubAllGlobals();
  });

  it('calls logout and redirects immediately when no refreshToken', async () => {
    vi.stubGlobal('location', { href: '' });

    axiosClient.defaults.adapter = async (config: InternalAxiosRequestConfig) => {
      throw make401(config);
    };

    const clearSpy = vi.spyOn(queryClient, 'clear').mockReturnValue(undefined);

    await expect(axiosClient.get('/test')).rejects.toBeDefined();

    expect(clearSpy).toHaveBeenCalled();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(window.location.href).toBe('/login');

    vi.unstubAllGlobals();
  });
});
