import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Button, Card, CardContent, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Chip, IconButton, Tooltip, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Alert,
} from '@mui/material';
import {
  AddRounded, PeopleAltRounded, EditRounded, StopRounded,
} from '@mui/icons-material';
import { recruiterApi } from '../../api/recruiterApi';
import { jobApi } from '../../api/jobApi';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error' | 'info'> = {
  PENDING_APPROVAL: 'warning',
  OPEN: 'success',
  CLOSED: 'default',
  EXPIRED: 'error',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function RecruiterJobsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [closeTargetId, setCloseTargetId] = useState<string | null>(null);
  const [closeError, setCloseError] = useState('');

  const { data: jobsPage, isLoading } = useQuery({
    queryKey: ['recruiter-jobs', 0],
    queryFn: () => recruiterApi.getMyJobs(0, 50),
  });

  const jobs = jobsPage?.content ?? [];

  const closeMutation = useMutation({
    mutationFn: (jobId: string) => jobApi.close(jobId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiter-jobs'] });
      setCloseTargetId(null);
      setCloseError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setCloseError(err?.response?.data?.message ?? 'Failed to close job.');
    },
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          My Job Postings
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddRounded />}
          onClick={() => navigate('/recruiter/jobs/create')}
        >
          Post a Job
        </Button>
      </Box>

      <Card>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress />
            </Box>
          ) : jobs.length === 0 ? (
            <Box sx={{ py: 6, textAlign: 'center' }}>
              <Typography color="text.secondary" gutterBottom>
                No job postings yet.
              </Typography>
              <Button
                variant="outlined"
                startIcon={<AddRounded />}
                onClick={() => navigate('/recruiter/jobs/create')}
                sx={{ mt: 1 }}
              >
                Post your first job
              </Button>
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Title</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Type</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Deadline</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Actions
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {jobs.map((job) => (
                    <TableRow key={job.id} hover>
                      <TableCell sx={{ fontWeight: 500 }}>{job.title}</TableCell>
                      <TableCell>{job.companyName}</TableCell>
                      <TableCell>
                        <Typography variant="body2">{job.jobType.replace('_', ' ')}</Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          {job.locationType}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography
                          variant="body2"
                          sx={{
                            color: new Date(job.applicationDeadline) < new Date() ? 'error.main' : 'inherit',
                          }}
                        >
                          {fmt(job.applicationDeadline)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={job.status.replace('_', ' ')}
                          size="small"
                          color={STATUS_COLOR[job.status] ?? 'default'}
                        />
                      </TableCell>
                      <TableCell align="right">
                        <Tooltip title="View Applicants">
                          <IconButton
                            size="small"
                            onClick={() => navigate(`/recruiter/jobs/${job.id}/applications`)}
                          >
                            <PeopleAltRounded fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Edit">
                          <span>
                            <IconButton
                              size="small"
                              disabled={job.status === 'CLOSED' || job.status === 'EXPIRED'}
                              onClick={() => navigate(`/recruiter/jobs/${job.id}/edit`)}
                            >
                              <EditRounded fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                        <Tooltip title="Close Job">
                          <span>
                            <IconButton
                              size="small"
                              color="error"
                              disabled={job.status !== 'OPEN'}
                              onClick={() => { setCloseTargetId(job.id); setCloseError(''); }}
                            >
                              <StopRounded fontSize="small" />
                            </IconButton>
                          </span>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!closeTargetId} onClose={() => setCloseTargetId(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Close Job Posting?</DialogTitle>
        <DialogContent>
          {closeError && <Alert severity="error" sx={{ mb: 2 }}>{closeError}</Alert>}
          <DialogContentText>
            Closing this job will stop accepting new applications. This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setCloseTargetId(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={closeMutation.isPending}
            onClick={() => closeTargetId && closeMutation.mutate(closeTargetId)}
          >
            {closeMutation.isPending ? 'Closing…' : 'Close Job'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
