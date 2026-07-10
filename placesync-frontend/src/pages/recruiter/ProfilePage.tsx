import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, TextField, Button,
  CircularProgress, Alert, Chip, FormControl, InputLabel, Select, MenuItem,
} from '@mui/material';
import { CheckCircleRounded, HourglassEmptyRounded, CancelRounded } from '@mui/icons-material';
import { recruiterApi } from '../../api/recruiterApi';
import { companyApi } from '../../api/companyApi';
import type { UpdateRecruiterProfileRequest } from '../../types/recruiter';

const VERIFICATION_CONFIG = {
  VERIFIED: { label: 'Verified', icon: <CheckCircleRounded fontSize="small" />, color: 'success' as const },
  PENDING_VERIFICATION: { label: 'Pending Verification', icon: <HourglassEmptyRounded fontSize="small" />, color: 'warning' as const },
  REJECTED: { label: 'Rejected', icon: <CancelRounded fontSize="small" />, color: 'error' as const },
};

export default function RecruiterProfilePage() {
  const queryClient = useQueryClient();
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['recruiter-profile'],
    queryFn: recruiterApi.getProfile,
  });

  const { data: companiesPage } = useQuery({
    queryKey: ['companies'],
    queryFn: () => companyApi.list(),
  });
  const companies = companiesPage?.content ?? [];

  const [form, setForm] = useState<UpdateRecruiterProfileRequest>({
    firstName: '',
    lastName: '',
    jobTitle: '',
    contactEmail: '',
    phone: '',
    companyId: '',
  });

  useEffect(() => {
    if (profile) {
      setForm({
        firstName: profile.firstName,
        lastName: profile.lastName,
        jobTitle: profile.jobTitle ?? '',
        contactEmail: profile.contactEmail ?? '',
        phone: profile.phone ?? '',
        companyId: profile.companyId ?? '',
      });
    }
  }, [profile]);

  const updateMutation = useMutation({
    mutationFn: (data: UpdateRecruiterProfileRequest) => recruiterApi.updateProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiter-profile'] });
      setSuccess(true);
      setError('');
      setTimeout(() => setSuccess(false), 3000);
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setError(err?.response?.data?.message ?? 'Failed to update profile.');
      setSuccess(false);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    updateMutation.mutate({
      ...form,
      companyId: form.companyId || undefined,
      jobTitle: form.jobTitle || undefined,
      contactEmail: form.contactEmail || undefined,
      phone: form.phone || undefined,
    });
  };

  if (profileLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const verStatus = profile?.verificationStatus ?? 'PENDING_VERIFICATION';
  const verConfig = VERIFICATION_CONFIG[verStatus];

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        My Profile
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 2fr' }, gap: 3 }}>
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 4 }}>
            <Box
              sx={{
                width: 80,
                height: 80,
                borderRadius: '50%',
                bgcolor: 'primary.light',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                mx: 'auto',
                mb: 2,
              }}
            >
              <Typography variant="h4" sx={{ color: 'primary.dark', fontWeight: 700 }}>
                {profile?.firstName?.[0]?.toUpperCase() ?? '?'}
              </Typography>
            </Box>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              {profile?.firstName} {profile?.lastName}
            </Typography>
            {profile?.jobTitle && (
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
                {profile.jobTitle}
              </Typography>
            )}
            {profile?.companyName && (
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
                {profile.companyName}
              </Typography>
            )}
            <Chip
              icon={verConfig.icon}
              label={verConfig.label}
              color={verConfig.color}
              size="small"
            />
            {verStatus === 'REJECTED' && profile?.rejectionReason && (
              <Typography variant="caption" sx={{ display: 'block', color: 'error.main', mt: 1 }}>
                Reason: {profile.rejectionReason}
              </Typography>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Edit Profile
            </Typography>

            {success && <Alert severity="success" sx={{ mb: 2 }}>Profile updated successfully.</Alert>}
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

            <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="First Name"
                  value={form.firstName}
                  onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))}
                  required
                  size="small"
                />
                <TextField
                  label="Last Name"
                  value={form.lastName}
                  onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))}
                  required
                  size="small"
                />
              </Box>

              <TextField
                label="Job Title"
                value={form.jobTitle}
                onChange={(e) => setForm((f) => ({ ...f, jobTitle: e.target.value }))}
                size="small"
                placeholder="e.g. Senior Software Engineer"
              />

              <TextField
                label="Contact Email"
                type="email"
                value={form.contactEmail}
                onChange={(e) => setForm((f) => ({ ...f, contactEmail: e.target.value }))}
                size="small"
              />

              <TextField
                label="Phone"
                value={form.phone}
                onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
                size="small"
              />

              <FormControl size="small">
                <InputLabel>Company</InputLabel>
                <Select
                  label="Company"
                  value={form.companyId ?? ''}
                  onChange={(e) => setForm((f) => ({ ...f, companyId: e.target.value }))}
                >
                  <MenuItem value="">
                    <em>None</em>
                  </MenuItem>
                  {companies
                    .filter((c) => c.status === 'VERIFIED')
                    .map((c) => (
                      <MenuItem key={c.id} value={c.id}>
                        {c.name}
                      </MenuItem>
                    ))}
                </Select>
              </FormControl>

              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="submit"
                  variant="contained"
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Saving…' : 'Save Changes'}
                </Button>
              </Box>
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}
