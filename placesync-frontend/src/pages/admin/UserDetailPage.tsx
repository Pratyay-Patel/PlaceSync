import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Button, Chip, CircularProgress, Alert, Divider,
} from '@mui/material';
import { ArrowBackRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';

const ROLE_LABEL: Record<string, string> = {
  ROLE_STUDENT: 'Student',
  ROLE_RECRUITER: 'Recruiter',
  ROLE_ADMIN: 'Admin',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

function DetailRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1.5 }}>
      <Typography variant="body2" sx={{ color: 'text.secondary', minWidth: 160 }}>
        {label}
      </Typography>
      <Box>{children}</Box>
    </Box>
  );
}

export default function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const { data: user, isLoading } = useQuery({
    queryKey: ['admin-user', userId],
    queryFn: () => adminApi.getUserById(userId!),
    enabled: !!userId,
  });

  const toggleMutation = useMutation({
    mutationFn: () => adminApi.updateUserStatus(userId!, !user!.isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-user', userId] });
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setError(err?.response?.data?.message ?? 'Failed to update user status.');
    },
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    return <Typography color="error">User not found.</Typography>;
  }

  return (
    <Box>
      <Button
        startIcon={<ArrowBackRounded />}
        onClick={() => navigate('/admin/users')}
        size="small"
        sx={{ mb: 2 }}
      >
        Back to Users
      </Button>

      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        User Detail
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Card sx={{ maxWidth: 560 }}>
        <CardContent>
          <DetailRow label="Email">
            <Typography variant="body2" sx={{ fontWeight: 500 }}>{user.email}</Typography>
          </DetailRow>
          <Divider />
          <DetailRow label="Role">
            <Chip
              label={ROLE_LABEL[user.role] ?? user.role}
              size="small"
              color={user.role === 'ROLE_ADMIN' ? 'error' : user.role === 'ROLE_RECRUITER' ? 'primary' : 'default'}
            />
          </DetailRow>
          <Divider />
          <DetailRow label="Email Verified">
            <Chip
              label={user.isEmailVerified ? 'Verified' : 'Unverified'}
              size="small"
              color={user.isEmailVerified ? 'success' : 'warning'}
            />
          </DetailRow>
          <Divider />
          <DetailRow label="Account Status">
            <Chip
              label={user.isActive ? 'Active' : 'Inactive'}
              size="small"
              color={user.isActive ? 'success' : 'default'}
            />
          </DetailRow>
          <Divider />
          <DetailRow label="Joined">
            <Typography variant="body2">{fmt(user.createdAt)}</Typography>
          </DetailRow>

          <Box sx={{ mt: 3 }}>
            <Button
              variant="contained"
              color={user.isActive ? 'error' : 'success'}
              disabled={toggleMutation.isPending}
              onClick={() => toggleMutation.mutate()}
            >
              {toggleMutation.isPending
                ? 'Saving…'
                : user.isActive
                  ? 'Deactivate User'
                  : 'Activate User'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
