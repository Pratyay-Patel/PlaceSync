import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import LoginPage from '../pages/auth/LoginPage';
import { authApi } from '../api/authApi';
import { createTestQueryClient } from './testUtils';
import type { AuthResponse } from '../types/auth';

const mockNavigate = vi.hoisted(() => vi.fn());

vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

// eslint-disable-next-line @typescript-eslint/no-explicit-any
vi.mock('framer-motion', () => ({ motion: { div: (p: any) => <div style={p.style}>{p.children}</div> } }));

vi.mock('../api/authApi');

const AUTH_RESPONSE: AuthResponse = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  expiresIn: 3600,
  userId: 'user-1',
  email: 'student@test.com',
  role: 'ROLE_STUDENT',
};

function renderLoginPage() {
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  mockNavigate.mockReset();
  vi.mocked(authApi.login).mockReset();
});

describe('LoginPage', () => {
  it('shows validation errors when form is submitted empty', async () => {
    renderLoginPage();
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText('Enter a valid email address')).toBeInTheDocument();
    });
    expect(screen.getByText('Password is required')).toBeInTheDocument();
  });

  it('navigates to role dashboard on successful login', async () => {
    vi.mocked(authApi.login).mockResolvedValueOnce(AUTH_RESPONSE);
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'student@test.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/student/dashboard', { replace: true });
    });
  });

  it('displays error message on 401 response', async () => {
    vi.mocked(authApi.login).mockRejectedValueOnce({ response: { status: 401 } });
    renderLoginPage();

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'wrong@test.com' },
    });
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByText('Invalid email or password.')).toBeInTheDocument();
    });
  });
});
