import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Box, Typography, CircularProgress } from '@mui/material';
import { jobApi, type JobFormData } from '../../api/jobApi';
import { JobForm } from './CreateJobPage';

export default function EditJobPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [error, setError] = useState('');

  const { data: job, isLoading } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => jobApi.getById(jobId!),
    enabled: !!jobId,
  });

  const updateMutation = useMutation({
    mutationFn: (data: JobFormData) => jobApi.update(jobId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiter-jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job', jobId] });
      navigate('/recruiter/jobs');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setError(err?.response?.data?.message ?? 'Failed to update job.');
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
    return <Typography color="error">Job not found.</Typography>;
  }

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Edit Job Posting
      </Typography>
      <JobForm
        initialData={{
          title: job.title,
          description: job.description,
          locationType: job.locationType,
          jobType: job.jobType,
          locationCity: job.locationCity,
          compensation: job.compensation,
          applicationDeadline: job.applicationDeadline,
          minCgpa: job.minCgpa ?? null,
          requiredSkills: job.requiredSkills,
          eligibleDepartments: job.eligibleDepartments,
        }}
        onSubmit={(data) => updateMutation.mutate(data)}
        isPending={updateMutation.isPending}
        error={error}
        submitLabel="Save Changes"
      />
    </Box>
  );
}
