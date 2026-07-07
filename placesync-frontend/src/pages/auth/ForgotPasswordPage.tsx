import { useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
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
} from '@mui/material';
import { CheckCircleOutlineRounded } from '@mui/icons-material';
import { motion } from 'framer-motion';
import { authApi } from '../../api/authApi';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
});

type FormData = z.infer<typeof schema>;

export default function ForgotPasswordPage() {
  const [submitted, setSubmitted] = useState(false);
  const [serverError, setServerError] = useState('');

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError('');
    try {
      await authApi.forgotPassword(data.email);
      setSubmitted(true);
    } catch {
      setServerError('Something went wrong. Please try again.');
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
            {submitted ? (
              <Box sx={{ textAlign: 'center', py: 2 }}>
                <CheckCircleOutlineRounded
                  sx={{ fontSize: 48, color: 'success.main', mb: 2 }}
                />
                <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>
                  Check your email
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  If <strong>{getValues('email')}</strong> is registered, you'll receive a
                  password reset link shortly.
                </Typography>
                <Link
                  component={RouterLink}
                  to="/login"
                  sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
                >
                  Back to sign in
                </Link>
              </Box>
            ) : (
              <>
                <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5 }}>
                  Forgot your password?
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
                  Enter your email and we'll send you a reset link.
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

                  <Button
                    type="submit"
                    variant="contained"
                    fullWidth
                    size="large"
                    disabled={isSubmitting}
                  >
                    {isSubmitting ? 'Sending…' : 'Send reset link'}
                  </Button>
                </Box>
              </>
            )}
          </CardContent>
        </Card>

        {!submitted && (
          <Typography variant="body2" sx={{ textAlign: 'center', mt: 2.5, color: 'text.secondary' }}>
            Remember your password?{' '}
            <Link
              component={RouterLink}
              to="/login"
              sx={{ fontWeight: 600, color: 'primary.main', textDecoration: 'none', '&:hover': { textDecoration: 'underline' } }}
            >
              Sign in
            </Link>
          </Typography>
        )}
      </motion.div>
    </Box>
  );
}
