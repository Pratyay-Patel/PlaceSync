import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Link,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { VisibilityRounded, VisibilityOffRounded } from '@mui/icons-material';
import { motion } from 'framer-motion';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/authApi';
import type { UserRole } from '../../types/auth';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type FormData = z.infer<typeof schema>;

const ROLE_REDIRECTS: Record<UserRole, string> = {
  ROLE_STUDENT: '/student/dashboard',
  ROLE_RECRUITER: '/recruiter/dashboard',
  ROLE_ADMIN: '/admin/dashboard',
};

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError('');
    try {
      const authResponse = await authApi.login(data);
      login(authResponse);
      navigate(ROLE_REDIRECTS[authResponse.role], { replace: true });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 401 || status === 400 || status === 403) {
        setServerError('Invalid email or password.');
      } else {
        setServerError('Something went wrong. Please try again.');
      }
    }
  };

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
            <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5 }}>
              Welcome back
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
              Sign in to your PlaceSync account
            </Typography>

            {serverError && (
              <Alert severity="error" sx={{ mb: 2.5 }}>
                {serverError}
              </Alert>
            )}

            <Box
              component="form"
              onSubmit={handleSubmit(onSubmit)}
              sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
            >
              <TextField
                {...register('email')}
                label="Email address"
                type="email"
                autoComplete="email"
                autoFocus
                fullWidth
                error={!!errors.email}
                helperText={errors.email?.message}
              />

              <Box>
                <TextField
                  {...register('password')}
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  fullWidth
                  error={!!errors.password}
                  helperText={errors.password?.message}
                  slotProps={{
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            size="small"
                            onClick={() => setShowPassword((p) => !p)}
                            edge="end"
                            tabIndex={-1}
                          >
                            {showPassword
                              ? <VisibilityOffRounded fontSize="small" />
                              : <VisibilityRounded fontSize="small" />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    },
                  }}
                />
                <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 0.75 }}>
                  <Link
                    component={RouterLink}
                    to="/forgot-password"
                    variant="caption"
                    sx={{ fontWeight: 500, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                  >
                    Forgot password?
                  </Link>
                </Box>
              </Box>

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Signing in…' : 'Sign in'}
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Typography variant="body2" sx={{ textAlign: 'center', mt: 2.5, color: 'text.secondary' }}>
          Don't have an account?{' '}
          <Link
            component={RouterLink}
            to="/register"
            sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
          >
            Create one
          </Link>
        </Typography>
      </motion.div>
    </Box>
  );
}
