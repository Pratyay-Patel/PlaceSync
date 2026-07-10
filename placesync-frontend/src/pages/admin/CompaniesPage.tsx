import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Button, Chip, Pagination, CircularProgress, Alert, Dialog, DialogTitle, DialogContent,
  DialogContentText, DialogActions, TextField,
} from '@mui/material';
import { CheckRounded, CloseRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';
import type { Company } from '../../types/recruiter';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error'> = {
  PENDING_APPROVAL: 'warning',
  ACTIVE: 'success',
  REJECTED: 'error',
  SUSPENDED: 'error',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

export default function CompaniesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const [rejectTarget, setRejectTarget] = useState<Company | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [actionError, setActionError] = useState('');

  const { data: companiesPage, isLoading } = useQuery({
    queryKey: ['admin-pending-companies', page],
    queryFn: () => adminApi.getPendingCompanies(page, PAGE_SIZE),
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => adminApi.verifyCompany(id, 'APPROVE'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-companies'] });
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to approve company.');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: () => adminApi.verifyCompany(rejectTarget!.id, 'REJECT', rejectReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-pending-companies'] });
      setRejectTarget(null);
      setRejectReason('');
      setActionError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setActionError(err?.response?.data?.message ?? 'Failed to reject company.');
    },
  });

  const companies = companiesPage?.content ?? [];
  const totalPages = companiesPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Company Approvals
      </Typography>

      {actionError && <Alert severity="error" sx={{ mb: 2 }}>{actionError}</Alert>}

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : companies.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No pending company approvals.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Name</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Industry</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Headquarters</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Created</TableCell>
                  <TableCell sx={{ fontWeight: 600 }} align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {companies.map((c) => (
                  <TableRow key={c.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>{c.name}</TableCell>
                    <TableCell>{c.industry ?? '—'}</TableCell>
                    <TableCell>{c.headquarters ?? '—'}</TableCell>
                    <TableCell>
                      <Chip
                        label={c.status.replace('_', ' ')}
                        size="small"
                        color={STATUS_COLOR[c.status] ?? 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{fmt(c.createdAt)}</Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        startIcon={<CheckRounded fontSize="small" />}
                        disabled={c.status === 'ACTIVE' || approveMutation.isPending}
                        onClick={() => approveMutation.mutate(c.id)}
                        sx={{ mr: 0.5 }}
                      >
                        Approve
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<CloseRounded fontSize="small" />}
                        disabled={c.status === 'REJECTED'}
                        onClick={() => { setRejectTarget(c); setRejectReason(''); setActionError(''); }}
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
        <DialogTitle>Reject Company</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Provide a reason for rejecting <strong>{rejectTarget?.name}</strong>.
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
