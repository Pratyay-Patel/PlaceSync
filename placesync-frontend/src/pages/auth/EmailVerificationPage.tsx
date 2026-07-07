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
import { motion } from 'framer-motion';
import { authApi } from '../../api/authApi';

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
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
        px: 2,
      }}
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
        style={{ width: '100%', maxWidth: 420 }}
      >
        {/* Logo */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 3, justifyContent: 'center' }}>
          <Box
            sx={{
              width: 38,
              height: 38,
              borderRadius: '10px',
              background: 'linear-gradient(135deg, #4F46E5 0%, #4338CA 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 2px 10px rgba(79, 70, 229, 0.4)',
            }}
          >
            <Typography sx={{ color: '#FFF', fontWeight: 700, fontSize: '0.875rem', lineHeight: 1 }}>
              PS
            </Typography>
          </Box>
          <Typography variant="h6" sx={{ fontWeight: 700, color: 'text.primary' }}>
            PlaceSync
          </Typography>
        </Box>

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
      </motion.div>
    </Box>
  );
}
