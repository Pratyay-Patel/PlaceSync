import { useState } from 'react';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
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
import { VisibilityRounded, VisibilityOffRounded, CheckCircleOutlineRounded } from '@mui/icons-material';
import { motion } from 'framer-motion';
import { authApi } from '../../api/authApi';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;

const schema = z
  .object({
    password: z
      .string()
      .min(8, 'Must be at least 8 characters')
      .regex(PASSWORD_REGEX, 'Must include uppercase, lowercase, number, and special character'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type FormData = z.infer<typeof schema>;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [success, setSuccess] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    if (!token) return;
    setServerError('');
    try {
      await authApi.resetPassword(token, data.password);
      setSuccess(true);
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 400 || status === 401 || status === 404) {
        setServerError('This reset link is invalid or has expired. Please request a new one.');
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
            {!token ? (
              <Box sx={{ textAlign: 'center', py: 2 }}>
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Invalid reset link
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  This link is missing a reset token. Please use the link from your email.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/forgot-password"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Request a new link
                </Link>
              </Box>
            ) : success ? (
              <Box sx={{ textAlign: 'center', py: 2 }}>
                <CheckCircleOutlineRounded sx={{ fontSize: 48, color: 'success.main', mb: 2 }} />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Password reset
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  Your password has been updated. You can now sign in with your new password.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/login"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Sign in
                </Link>
              </Box>
            ) : (
              <>
                <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5 }}>
                  Set a new password
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  Choose a strong password for your account.
                </Typography>

                {serverError && (
                  <Alert severity="error" sx={{ mb: 2.5 }}>
                    {serverError}{' '}
                    {(serverError.includes('invalid') || serverError.includes('expired')) && (
                      <Link component={RouterLink} to="/forgot-password" sx={{ fontWeight: 600 }}>
                        Request a new link
                      </Link>
                    )}
                  </Alert>
                )}

                <Box
                  component="form"
                  onSubmit={handleSubmit(onSubmit)}
                  sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}
                >
                  <TextField
                    {...register('password')}
                    label="New password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    autoFocus
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

                  <TextField
                    {...register('confirmPassword')}
                    label="Confirm new password"
                    type={showConfirm ? 'text' : 'password'}
                    autoComplete="new-password"
                    fullWidth
                    error={!!errors.confirmPassword}
                    helperText={errors.confirmPassword?.message}
                    slotProps={{
                      input: {
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton
                              size="small"
                              onClick={() => setShowConfirm((p) => !p)}
                              edge="end"
                              tabIndex={-1}
                            >
                              {showConfirm
                                ? <VisibilityOffRounded fontSize="small" />
                                : <VisibilityRounded fontSize="small" />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      },
                    }}
                  />

                  <Button
                    type="submit"
                    variant="contained"
                    fullWidth
                    size="large"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Updating…' : 'Update password'}
                  </Button>
                </Box>
              </>
            )}
          </CardContent>
        </Card>
      </motion.div>
    </Box>
  );
}
