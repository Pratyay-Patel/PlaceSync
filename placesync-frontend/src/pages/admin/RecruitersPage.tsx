import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Chip, Pagination, CircularProgress, Alert, Dialog, DialogTitle, DialogContent,
  DialogContentText, DialogActions, TextField,
} from '@mui/material';
import { CheckRounded, CloseRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';
import type { RecruiterProfile } from '../../types/recruiter';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error'> = {
  PENDING_VERIFICATION: 'warning',
  VERIFIED: 'success',
  REJECTED: 'error',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

export default function RecruitersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const [rejectTarget, setRejectTarget] = useState<RecruiterProfile | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionError, setActionError] = useState('');

  const { data: recruitersPage, isLoading } = useQuery({
    queryKey: ['admin-pending-recruiters', page],
    queryFn: () => adminApi.getPendingRecruiters(page, PAGE_SIZE),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => adminApi.verifyRecruiter(id, 'APPROVE'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-recruiters'] });
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to approve recruiter.');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => adminApi.verifyRecruiter(rejectTarget!.id, 'REJECT', rejectReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-recruiters'] });
      setRejectTarget(null);
      setRejectReason('');
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to reject recruiter.');
    },
  });

  const recruiters = recruitersPage?.content ?? [];
  const totalPages = recruitersPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Recruiter Approvals
      </Typography>

      {actionError && <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert>}

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : recruiters.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No pending recruiter verifications.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Name</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Contact</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Applied</TableCell>
                  <TableCell sx={{ fontWeight: 600 }} align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {recruiters.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>
                      {r.firstName} {r.lastName}
                    </TableCell>
                    <TableCell>{r.companyName ?? '—'}</TableCell>
                    <TableCell>
                      <Typography variant="caption">{r.contactEmail ?? '—'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={r.verificationStatus.replace('_', ' ')}
                        size="small"
                        color={STATUS_COLOR[r.verificationStatus] ?? 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{fmt(r.createdAt)}</Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        startIcon={<CheckRounded fontSize="small" />}
                        disabled={r.verificationStatus === 'VERIFIED' || approveMutation.isPending}
                        onClick={() => approveMutation.mutate(r.id)}
                        sx={{ mr: 0.5 }}
                      >
                        Approve
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<CloseRounded fontSize="small" />}
                        disabled={r.verificationStatus === 'REJECTED'}
                        onClick={() => { setRejectTarget(r); setRejectReason(''); setActionError(''); }}
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

      <Dialog open={!!rejectTarget} onClose={() => setRejectTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Reject Recruiter Verification</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Provide a reason for rejecting{' '}
            <strong>{rejectTarget?.firstName} {rejectTarget?.lastName}</strong>'s verification.
          </DialogContentText>
          <TextField
            label="Rejection Reason"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            fullWidth
            multiline
            rows={3}
            size="small"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setRejectTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            disabled={!rejectReason.trim() || rejectMutation.isPending}
            onClick={() => rejectMutation.mutate()}
          >
            {rejectMutation.isPending ? 'Rejecting…' : 'Reject'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
