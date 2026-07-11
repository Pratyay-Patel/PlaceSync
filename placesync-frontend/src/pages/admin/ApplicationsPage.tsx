import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  FormControl, InputLabel, Select, MenuItem, Pagination, CircularProgress,
} from '@mui/material';
import { adminApi } from '../../api/adminApi';
import type { ApplicationStatus } from '../../types/application';
import StatusChip from '../../components/common/StatusChip';

const ALL_STATUSES: ApplicationStatus[] = [
  'APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'OFFERED', 'REJECTED',
];

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });
}

export default function AdminApplicationsPage() {
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | ''>('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const { data: appsPage, isLoading } = useQuery({
    queryKey: ['admin-applications', statusFilter, page],
    queryFn: () =>
      adminApi.getAllApplications({
        status: statusFilter || undefined,
        page,
        size: PAGE_SIZE,
      }),
  });

  const apps = appsPage?.content ?? [];
  const totalPages = appsPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        All Applications
      </Typography>

      <Box sx={{ mb: 3 }}>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Filter by status</InputLabel>
          <Select
            label="Filter by status"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value as ApplicationStatus | '');
              setPage(0);
            }}
          >
            <MenuItem value="">All statuses</MenuItem>
            {ALL_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s.replace('_', ' ')}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : apps.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No applications found.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Student</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Job</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Applied</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {apps.map((a) => (
                  <TableRow key={a.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>
                      {a.studentFirstName} {a.studentLastName}
                    </TableCell>
                    <TableCell>{a.jobTitle}</TableCell>
                    <TableCell>{a.companyName}</TableCell>
                    <TableCell>
                      <StatusChip status={a.status} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{fmt(a.appliedAt)}</Typography>
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
    </Box>
  );
}
