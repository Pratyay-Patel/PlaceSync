import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RoleRoute from '../routes/RoleRoute';
import { useAuthStore } from '../store/authStore';

beforeEach(() => {
  useAuthStore.setState({
    accessToken: null,
    userId: null,
    email: null,
    role: null,
    isAuthenticated: false,
  });
});

describe('RoleRoute', () => {
  it('redirects unauthenticated users to /login', () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={<RoleRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/admin" element={<p>Admin area</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/403" element={<p>Forbidden</p>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('Login page')).toBeInTheDocument();
  });

  it('redirects authenticated user with wrong role to /403', () => {
    useAuthStore.setState({ isAuthenticated: true, role: 'ROLE_STUDENT' });
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={<RoleRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/admin" element={<p>Admin area</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/403" element={<p>Forbidden</p>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('Forbidden')).toBeInTheDocument();
    expect(screen.queryByText('Admin area')).not.toBeInTheDocument();
  });

  it('renders outlet for user with correct role', () => {
    useAuthStore.setState({ isAuthenticated: true, role: 'ROLE_ADMIN' });
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <Routes>
          <Route element={<RoleRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/admin" element={<p>Admin area</p>} />
          </Route>
          <Route path="/login" element={<p>Login page</p>} />
          <Route path="/403" element={<p>Forbidden</p>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('Admin area')).toBeInTheDocument();
  });
});
