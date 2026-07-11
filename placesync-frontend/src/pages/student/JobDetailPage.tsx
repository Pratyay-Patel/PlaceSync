import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Chip, CircularProgress,
  Button, Divider, Dialog, DialogTitle, DialogContent, DialogActions,
  FormControl, InputLabel, Select, MenuItem, Alert,
} from '@mui/material';
import {
  ArrowBackRounded, LocationOnRounded, WorkRounded, AccessTimeRounded,
  SchoolRounded, CheckCircleRounded,
} from '@mui/icons-material';
import { jobApi } from '../../api/jobApi';
import { applicationApi } from '../../api/applicationApi';
import { resumeApi } from '../../api/resumeApi';
import type { JobLocationType, JobType } from '../../types/job';

const LOCATION_LABEL: Record<JobLocationType, string> = {
  ONSITE: 'On-site',
  REMOTE: 'Remote',
  HYBRID: 'Hybrid',
};

const TYPE_LABEL: Record<JobType, string> = {
  FULL_TIME: 'Full-time',
  INTERNSHIP: 'Internship',
  CONTRACT: 'Contract',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedResumeId, setSelectedResumeId] = useState('');
  const [applyError, setApplyError] = useState('');
  const [applied, setApplied] = useState(false);

  const { data: job, isLoading } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => jobApi.getById(jobId!),
    enabled: !!jobId,
  });

  const { data: resumes = [] } = useQuery({
    queryKey: ['resumes'],
    queryFn: resumeApi.list,
  });

  const applyMutation = useMutation({
    mutationFn: () => applicationApi.apply(jobId!, selectedResumeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-applications'] });
      setDialogOpen(false);
      setApplied(true);
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setApplyError(err?.response?.data?.message ?? 'Failed to submit application. Please try again.');
    },
  });

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!job) {
    return (
      <Box>
        <Typography color="error">Job not found.</Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Button
        startIcon={<ArrowBackRounded />}
        onClick={() => navigate(-1)}
        sx={{ mb: 2 }}
        size="small"
      >
        Back to jobs
      </Button>

      {applied && (
        <Alert
          icon={<CheckCircleRounded />}
          severity="success"
          sx={{ mb: 3 }}
        >
          Application submitted successfully!
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 2 }}>
            <Box>
              <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5 }}>
                {job.title}
              </Typography>
              <Typography variant="body1" sx={{ color: 'text.secondary' }}>
                {job.companyName}
              </Typography>
            </Box>

            <Button
              variant="contained"
              size="large"
              disabled={applied || job.status !== 'OPEN'}
              onClick={() => { setApplyError(''); setDialogOpen(true); }}
            >
              {applied ? 'Applied' : job.status !== 'OPEN' ? 'Closed' : 'Apply Now'}
            </Button>
          </Box>

          <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', mb: 2 }}>
            <Chip icon={<LocationOnRounded />} label={LOCATION_LABEL[job.locationType]} variant="outlined" />
            <Chip icon={<WorkRounded />} label={TYPE_LABEL[job.jobType]} variant="outlined" />
            {job.locationCity && <Chip label={job.locationCity} variant="outlined" />}
            {job.compensation && (
              <Chip label={job.compensation} variant="outlined" color="success" />
            )}
            <Chip
              icon={<AccessTimeRounded />}
              label={`Deadline: ${fmt(job.applicationDeadline)}`}
              variant="outlined"
              color={new Date(job.applicationDeadline) < new Date() ? 'error' : 'default'}
            />
          </Box>

          {job.minCgpa && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, mb: 2 }}>
              <SchoolRounded fontSize="small" sx={{ color: 'text.secondary' }} />
              <Typography variant="body2">
                Minimum CGPA: <strong>{job.minCgpa}</strong>
              </Typography>
            </Box>
          )}
        </CardContent>
      </Card>

      <Box sx={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 3 }}>
        <Box>
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
                Job Description
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.8 }}>
                {job.description}
              </Typography>
            </CardContent>
          </Card>

          {job.requiredSkills.length > 0 && (
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
                  Required Skills
                </Typography>
                <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                  {job.requiredSkills.map((skill) => (
                    <Chip key={skill} label={skill} size="small" />
                  ))}
                </Box>
              </CardContent>
            </Card>
          )}
        </Box>

        <Box>
          {job.eligibleDepartments.length > 0 && (
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
                  Eligible Departments
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                  {job.eligibleDepartments.map((dept) => (
                    <Typography key={dept} variant="body2">• {dept}</Typography>
                  ))}
                </Box>
              </CardContent>
            </Card>
          )}
        </Box>
      </Box>

      {/* Apply dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Apply for {job.title}</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          {applyError && (
            <Alert severity="error" sx={{ mb: 2 }}>{applyError}</Alert>
          )}

          {resumes.length === 0 ? (
            <Alert severity="warning">
              You need to upload a resume before applying.
            </Alert>
          ) : (
            <FormControl fullWidth size="small">
              <InputLabel>Select Resume</InputLabel>
              <Select
                label="Select Resume"
                value={selectedResumeId}
                onChange={(e) => setSelectedResumeId(e.target.value)}
              >
                {resumes.map((r) => (
                  <MenuItem key={r.id} value={r.id}>
                    {r.label}{r.isDefault ? ' (default)' : ''}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </DialogContent>
        <Divider />
        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!selectedResumeId || resumes.length === 0 || applyMutation.isPending}
            onClick={() => applyMutation.mutate()}
          >
            {applyMutation.isPending ? 'Submitting…' : 'Submit Application'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
