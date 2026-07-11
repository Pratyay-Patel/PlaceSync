import { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
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
  ToggleButtonGroup,
  ToggleButton,
  MenuItem,
} from '@mui/material';
import { VisibilityRounded, VisibilityOffRounded } from '@mui/icons-material';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../api/authApi';
import type { UserRole } from '../../types/auth';

const CURRENT_YEAR = new Date().getFullYear();
const GRAD_YEARS = Array.from({ length: 7 }, (_, i) => CURRENT_YEAR - 1 + i);

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;

const schema = z
  .object({
    role: z.enum(['ROLE_STUDENT', 'ROLE_RECRUITER']),
    firstName: z.string().min(1, 'First name is required'),
    lastName: z.string().min(1, 'Last name is required'),
    email: z.string().email('Enter a valid email address'),
    password: z
      .string()
      .min(8, 'Must be at least 8 characters')
      .regex(PASSWORD_REGEX, 'Must include uppercase, lowercase, number, and special character'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
    institution: z.string().optional(),
    department: z.string().optional(),
    graduationYear: z.number().optional(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  })
  .superRefine((d, ctx) => {
    if (d.role === 'ROLE_STUDENT') {
      if (!d.institution?.trim()) {
        ctx.addIssue({ code: 'custom', path: ['institution'], message: 'Institution is required' });
      }
      if (!d.department?.trim()) {
        ctx.addIssue({ code: 'custom', path: ['department'], message: 'Department is required' });
      }
      if (!d.graduationYear) {
        ctx.addIssue({ code: 'custom', path: ['graduationYear'], message: 'Graduation year is required' });
      }
    }
  });

type FormData = z.infer<typeof schema>;

const ROLE_REDIRECTS: Record<UserRole, string> = {
  ROLE_STUDENT: '/student/dashboard',
  ROLE_RECRUITER: '/recruiter/dashboard',
  ROLE_ADMIN: '/admin/dashboard',
};

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [serverError, setServerError] = useState('');
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: 'ROLE_STUDENT' },
  });

  const role = watch('role');
  const isStudent = role === 'ROLE_STUDENT';

  const onSubmit = async (data: FormData) => {
    setServerError('');
    try {
      const authResponse = await authApi.register({
        email: data.email,
        password: data.password,
        role: data.role,
        firstName: data.firstName,
        lastName: data.lastName,
        ...(isStudent && {
          institution: data.institution,
          department: data.department,
          graduationYear: data.graduationYear,
        }),
      });
      login(authResponse);
      navigate(ROLE_REDIRECTS[authResponse.role], { replace: true });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      if (status === 409) {
        setServerError('An account with this email already exists.');
      } else if (status === 400) {
        setServerError(message || 'Please check your details and try again.');
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
        py: 4,
      }}
    >
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
        style={{ width: '100%', maxWidth: 480 }}
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
              Create your account
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
              Join PlaceSync — find your next opportunity
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
              {/* Role selector */}
              <Box>
                <Typography
                  variant="caption"
                  sx={{ color: 'text.secondary', mb: 1, display: 'block', fontWeight: 500 }}
                >
                  I am a
                </Typography>
                <Controller
                  name="role"
                  control={control}
                  render={({ field }) => (
                    <ToggleButtonGroup
                      exclusive
                      fullWidth
                      value={field.value}
                      onChange={(_, val) => {
                        if (val) field.onChange(val);
                      }}
                      sx={{ height: 40 }}
                    >
                      <ToggleButton
                        value="ROLE_STUDENT"
                        sx={{ fontSize: '0.875rem', fontWeight: 600, textTransform: 'none' }}
                      >
                        Student
                      </ToggleButton>
                      <ToggleButton
                        value="ROLE_RECRUITER"
                        sx={{ fontSize: '0.875rem', fontWeight: 600, textTransform: 'none' }}
                      >
                        Recruiter
                      </ToggleButton>
                    </ToggleButtonGroup>
                  )}
                />
              </Box>

              {/* Name row */}
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  {...register('firstName')}
                  label="First name"
                  autoComplete="given-name"
                  autoFocus
                  fullWidth
                  error={!!errors.firstName}
                  helperText={errors.firstName?.message}
                />
                <TextField
                  {...register('lastName')}
                  label="Last name"
                  autoComplete="family-name"
                  fullWidth
                  error={!!errors.lastName}
                  helperText={errors.lastName?.message}
                />
              </Box>

              {/* Email */}
              <TextField
                {...register('email')}
                label="Email address"
                type="email"
                autoComplete="email"
                fullWidth
                error={!!errors.email}
                helperText={errors.email?.message}
              />

              {/* Password */}
              <TextField
                {...register('password')}
                label="Password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
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

              {/* Confirm password */}
              <TextField
                {...register('confirmPassword')}
                label="Confirm password"
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

              {/* Student-only fields — animate in/out on role switch */}
              <AnimatePresence initial={false}>
                {isStudent && (
                  <motion.div
                    key="student-fields"
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    transition={{ duration: 0.22, ease: 'easeInOut' }}
                    style={{ overflow: 'hidden', paddingTop: 10 }}
                  >
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
                      <TextField
                        {...register('institution')}
                        label="Institution / University"
                        autoComplete="organization"
                        fullWidth
                        error={!!errors.institution}
                        helperText={errors.institution?.message}
                      />
                      <TextField
                        {...register('department')}
                        label="Department"
                        fullWidth
                        error={!!errors.department}
                        helperText={errors.department?.message}
                      />
                      <Controller
                        name="graduationYear"
                        control={control}
                        render={({ field }) => (
                          <TextField
                            label="Expected graduation year"
                            select
                            fullWidth
                            value={field.value ?? ''}
                            onChange={(e) =>
                              field.onChange(e.target.value ? Number(e.target.value) : undefined)
                            }
                            error={!!errors.graduationYear}
                            helperText={errors.graduationYear?.message}
                          >
                            <MenuItem value="" disabled>
                              Select year
                            </MenuItem>
                            {GRAD_YEARS.map((year) => (
                              <MenuItem key={year} value={year}>
                                {year}
                              </MenuItem>
                            ))}
                          </TextField>
                        )}
                      />
                    </Box>
                  </motion.div>
                )}
              </AnimatePresence>

              <Button
                type="submit"
                variant="contained"
                fullWidth
                size="large"
                disabled={isSubmitting}
                sx={{ mt: 0.5 }}
              >
                {isSubmitting ? 'Creating account…' : 'Create account'}
              </Button>
            </Box>
          </CardContent>
        </Card>

        <Typography variant="body2" sx={{ textAlign: 'center', mt: 2.5, color: 'text.secondary' }}>
          Already have an account?{' '}
          <Link
            component={RouterLink}
            to="/login"
            sx={{
              fontWeight: 600,
              color: 'primary.main',
              textDecoration: 'none',
              '&:hover': { textDecoration: 'underline' },
            }}
          >
            Sign in
          </Link>
        </Typography>
      </motion.div>
    </Box>
  );
}
