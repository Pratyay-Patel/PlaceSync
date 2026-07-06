import { useEffect, useState } from 'react';
import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import type { AuthResponse } from '../types/auth';

// Silently restores the session on page refresh by exchanging the stored
// refresh token for a new access token. Returns true while the check is
// in flight so the root layout can defer rendering until auth state is known.
export function useSessionRestore(): boolean {
  const [isRestoring, setIsRestoring] = useState(true);
  const login = useAuthStore((s) => s.login);

  useEffect(() => {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      setIsRestoring(false);
      return;
    }

    axios
      .post<AuthResponse>('/api/v1/auth/refresh', { refreshToken })
      .then(({ data }) => login(data))
      .catch(() => localStorage.removeItem('refreshToken'))
      .finally(() => setIsRestoring(false));
  }, [login]);

  return isRestoring;
}
