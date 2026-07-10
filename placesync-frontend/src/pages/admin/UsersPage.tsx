import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Select, MenuItem, FormControl, InputLabel, Chip, IconButton, Tooltip,
  Pagination, CircularProgress, Switch, Alert, Dialog, DialogTitle, DialogContent,
  DialogContentText, DialogActions, Button,
} from '@mui/material';
import { OpenInNewRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';
import type { UserSummary } from '../../types/admin';

const ROLE_LABEL: Record<string, string> = {
  ROLE_STUDENT: 'Student',
  ROLE_RECRUITER: 'Recruiter',
  ROLE_ADMIN: 'Admin',
};

const ROLE_COLOR: Record<string, 'default' | 'primary' | 'secondary' | 'error'> = {
  ROLE_STUDENT: 'default',
  ROLE_RECRUITER: 'primary',
  ROLE_ADMIN: 'error',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

export default function UsersPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [emailInput, setEmailInput] = useState('');
  const [emailFilter, setEmailFilter] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const [toggleTarget, setToggleTarget] = useState<UserSummary | null>(null);
  const [toggleError, setToggleError] = useState('');

  useEffect(() => {
    const timer = setTimeout(() => {
      setEmailFilter(emailInput);
      setPage(0);
    }, 400);
    return () => clearTimeout(timer);
  }, [emailInput]);

  const params = {
    email: emailFilter || undefined,
    role: roleFilter || undefined,
    isActive: activeFilter === '' ? undefined : activeFilter === 'true',
    page,
    size: PAGE_SIZE,
  };

  const { data: usersPage, isLoading } = useQuery({
    queryKey: ['admin-users', params],
    queryFn: () => adminApi.searchUsers(params),
  });

  const toggleMutation = useMutation({
    mutationFn: () => adminApi.updateUserStatus(toggleTarget!.id, !toggleTarget!.isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setToggleTarget(null);
      setToggleError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setToggleError(err?.response?.data?.message ?? 'Failed to update user status.');
    },
  });

  const users = usersPage?.content ?? [];
  const totalPages = usersPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Users
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <TextField
          label="Search by email"
          value={emailInput}
          onChange={(e) => setEmailInput(e.target.value)}
          size="small"
          sx={{ minWidth: 220 }}
        />
        <FormControl size="small" sx={{ minWidth: 160 }}>
          <InputLabel>Role</InputLabel>
          <Select
            label="Role"
            value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All roles</MenuItem>
            <MenuItem value="ROLE_STUDENT">Student</MenuItem>
            <MenuItem value="ROLE_RECRUITER">Recruiter</MenuItem>
            <MenuItem value="ROLE_ADMIN">Admin</MenuItem>
          </Select>
        </FormControl>
        <FormControl size="small" sx={{ minWidth: 140 }}>
          <InputLabel>Status</InputLabel>
          <Select
            label="Status"
            value={activeFilter}
            onChange={(e) => { setActiveFilter(e.target.value); setPage(0); }}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Active</MenuItem>
            <MenuItem value="false">Inactive</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : users.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No users found.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Email</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Role</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Email Verified</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Joined</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Active</TableCell>
                  <TableCell sx={{ fontWeight: 600 }} align="right">Detail</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>{user.email}</TableCell>
                    <TableCell>
                      <Chip
                        label={ROLE_LABEL[user.role] ?? user.role}
                        size="small"
                        color={ROLE_COLOR[user.role] ?? 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={user.isEmailVerified ? 'Verified' : 'Unverified'}
                        size="small"
                        color={user.isEmailVerified ? 'success' : 'warning'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{fmt(user.createdAt)}</Typography>
                    </TableCell>
                    <TableCell>
                      <Switch
                        size="small"
                        checked={user.isActive}
                        onChange={() => { setToggleTarget(user); setToggleError(''); }}
                        color="success"
                      />
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View detail">
                        <IconButton size="small" onClick={() => navigate(`/admin/users/${user.id}`)}>
                          <OpenInNewRounded fontSize="small" />
                        </IconButton>
                      </Tooltip>
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

      <Dialog open={!!toggleTarget} onClose={() => setToggleTarget(null)} maxWidth="xs" fullWidth>
        <DialogTitle>
          {toggleTarget?.isActive ? 'Deactivate User?' : 'Activate User?'}
        </DialogTitle>
        <DialogContent>
          {toggleError && <Alert severity="error" sx={{ mb: 2 }}>{toggleError}</Alert>}
          <DialogContentText>
            {toggleTarget?.isActive
              ? `This will prevent ${toggleTarget.email} from logging in.`
              : `This will restore access for ${toggleTarget?.email}.`}
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setToggleTarget(null)}>Cancel</Button>
          <Button
            variant="contained"
            color={toggleTarget?.isActive ? 'error' : 'success'}
            disabled={toggleMutation.isPending}
            onClick={() => toggleMutation.mutate()}
          >
            {toggleMutation.isPending
              ? 'Saving…'
              : toggleTarget?.isActive
                ? 'Deactivate'
                : 'Activate'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
