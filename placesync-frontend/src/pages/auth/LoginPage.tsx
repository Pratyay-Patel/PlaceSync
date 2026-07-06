import { useNavigate } from 'react-router-dom';
import { Box, Button, Typography, Stack } from '@mui/material';
import { useAuthStore } from '../../store/authStore';
import type { AuthResponse } from '../../types/auth';

// DEV ONLY — removed when real login form is implemented in 6.3
const DEV_BASE: Omit<AuthResponse, 'role'> = {
  accessToken: 'dev-token',
  refreshToken: 'dev-refresh',
  expiresIn: 3600,
  userId: 'dev-user-id',
  email: 'pratyay@placesync.dev',
};

const ROLE_REDIRECTS: Record<AuthResponse['role'], string> = {
  ROLE_STUDENT: '/student/dashboard',
  ROLE_RECRUITER: '/recruiter/dashboard',
  ROLE_ADMIN: '/admin/dashboard',
};

export default function LoginPage() {
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const preview = (role: AuthResponse['role']) => {
    login({ ...DEV_BASE, role });
    navigate(ROLE_REDIRECTS[role], { replace: true });
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        gap: 2,
      }}
    >
      <Typography variant="h5" sx={{ mb: 1 }}>
        DEV PREVIEW — choose a role to test the dashboard shell
      </Typography>
      <Stack direction="row" spacing={2}>
        <Button variant="contained" onClick={() => preview('ROLE_STUDENT')}>
          Student Dashboard
        </Button>
        <Button variant="contained" color="secondary" onClick={() => preview('ROLE_RECRUITER')}>
          Recruiter Dashboard
        </Button>
        <Button variant="outlined" onClick={() => preview('ROLE_ADMIN')}>
          Admin Dashboard
        </Button>
      </Stack>
    </Box>
  );
}
