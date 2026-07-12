import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Pagination, CircularProgress, Alert,
} from '@mui/material';
import { CheckRounded, CloseRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';
import type { JobSummary } from '../../types/job';
import RejectDialog from '../../components/admin/RejectDialog';
import { formatDate } from '../../utils/format';

export default function AdminJobsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const [rejectTarget, setRejectTarget] = useState<JobSummary | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionError, setActionError] = useState('');

  const { data: jobsPage, isLoading } = useQuery({
    queryKey: ['admin-pending-jobs', page],
    queryFn: () => adminApi.getPendingJobs(page, PAGE_SIZE),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => adminApi.approveJob(id, 'APPROVE'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-jobs'] });
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to approve job.');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => adminApi.approveJob(rejectTarget!.id, 'REJECT', rejectReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-jobs'] });
      setRejectTarget(null);
      setRejectReason('');
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to reject job.');
    },
  });

  const jobs = jobsPage?.content ?? [];
  const totalPages = jobsPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Job Approvals
      </Typography>

      {actionError && <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert>}

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : jobs.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No pending job approvals.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Title</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Location</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Deadline</TableCell>
                  <TableCell sx={{ fontWeight: 600 }} align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {jobs.map((j) => (
                  <TableRow key={j.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>{j.title}</TableCell>
                    <TableCell>{j.companyName}</TableCell>
                    <TableCell>
                      <Typography variant="body2">{j.jobType.replace('_', ' ')}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{j.locationType}</Typography>
                      {j.locationCity && (
                        <Typography variant="caption" sx={{ display: 'block', color: 'text.secondary' }}>
                          {j.locationCity}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <Typography
                        variant="caption"
                        sx={{ color: new Date(j.applicationDeadline) < new Date() ? 'error.main' : 'inherit' }}
                      >
                        {formatDate(j.applicationDeadline)}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        startIcon={<CheckRounded fontSize="small" />}
                        disabled={approveMutation.isPending}
                        onClick={() => approveMutation.mutate(j.id)}
                        sx={{ mr: 0.5 }}
                      >
                        Approve
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<CloseRounded fontSize="small" />}
                        onClick={() => { setRejectTarget(j); setRejectReason(''); setActionError(''); }}
                      >
                        Reject
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(_, v) => setPage(v - 1)}
            color="primary"
            size="small"
          />
        </Box>
      )}

      <RejectDialog
        open={!!rejectTarget}
        title="Reject Job Posting"
        description={<>Provide a reason for rejecting <strong>{rejectTarget?.title}</strong>.</>}
        reason={rejectReason}
        onReasonChange={setRejectReason}
        onConfirm={() => rejectMutation.mutate()}
        onCancel={() => setRejectTarget(null)}
        isPending={rejectMutation.isPending}
      />
    </Box>
  );
}
