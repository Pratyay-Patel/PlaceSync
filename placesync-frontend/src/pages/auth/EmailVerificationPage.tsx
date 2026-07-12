import { useEffect, useState } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Link,
  CircularProgress,
} from '@mui/material';
import {
  CheckCircleOutlineRounded,
  ErrorOutlineRounded,
} from '@mui/icons-material';
import { authApi } from '../../api/authApi';
import AuthPageLayout from '../../components/layout/AuthPageLayout';

type VerifyState = 'loading' | 'success' | 'error' | 'no-token';

export default function EmailVerificationPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [state, setState] = useState<VerifyState>(token ? 'loading' : 'no-token');

  useEffect(() => {
    if (!token) return;

    authApi
      .verifyEmail(token)
      .then(() => setState('success'))
      .catch(() => setState('error'));
  }, [token]);

  return (
    <AuthPageLayout>
      <Card sx={{ p: 1 }}>
        <CardContent>
          <Box sx={{ textAlign: 'center', py: 2 }}>
            {state === 'loading' && (
              <>
                <CircularProgress sx={{ mb: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Verifying your email…
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  Just a moment.
                </Typography>
              </>
            )}

            {state === 'success' && (
              <>
                <CheckCircleOutlineRounded sx={{ fontSize: 48, color: 'success.main', mb: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Email verified!
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  Your email address has been confirmed. You can now sign in to your account.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/login"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Sign in
                </Link>
              </>
            )}

            {state === 'error' && (
              <>
                <ErrorOutlineRounded sx={{ fontSize: 48, color: 'error.main', mb: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Verification failed
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  This verification link is invalid or has expired. Please register again or
                  contact support.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/register"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Back to register
                </Link>
              </>
            )}

            {state === 'no-token' && (
              <>
                <ErrorOutlineRounded sx={{ fontSize: 48, color: 'warning.main', mb: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Invalid verification link
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  This link is missing a verification token. Please use the link from your
                  registration email.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/login"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Back to sign in
                </Link>
              </>
            )}
          </Box>
        </CardContent>
      </Card>
    </AuthPageLayout>
  );
}
