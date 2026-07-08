import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Chip, CircularProgress,
  Table, TableHead, TableRow, TableCell, TableBody, Pagination,
} from '@mui/material';
import { applicationApi } from '../../api/applicationApi';
import type { ApplicationStatus } from '../../types/application';

const STATUS_COLOR: Record<ApplicationStatus, string> = {
  APPLIED: '#3B82F6',
  UNDER_REVIEW: '#F59E0B',
  SHORTLISTED: '#8B5CF6',
  INTERVIEW_SCHEDULED: '#06B6D4',
  OFFERED: '#10B981',
  REJECTED: '#EF4444',
};

const STATUS_LABEL: Record<ApplicationStatus, string> = {
  APPLIED: 'Applied',
  UNDER_REVIEW: 'Under Review',
  SHORTLISTED: 'Shortlisted',
  INTERVIEW_SCHEDULED: 'Interview Scheduled',
  OFFERED: 'Offered',
  REJECTED: 'Rejected',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

const PAGE_SIZE = 15;

export default function StudentApplicationsPage() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['student-applications', page, PAGE_SIZE],
    queryFn: () => applicationApi.getMyApplications(page, PAGE_SIZE),
  });

  const applications = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>My Applications</Typography>
        {data && (
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            {data.totalElements} application{data.totalElements !== 1 ? 's' : ''} submitted
          </Typography>
        )}
      </Box>

      <Card>
        <CardContent sx={{ p: 0, '&:last-child': { pb: 0 } }}>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress />
            </Box>
          ) : applications.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 6 }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                No applications yet.
              </Typography>
            </Box>
          ) : (
            <>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Job Title</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Applied On</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Last Updated</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {applications.map((app) => (
                    <TableRow key={app.id} hover>
                      <TableCell sx={{ fontWeight: 500 }}>{app.jobTitle}</TableCell>
                      <TableCell>{app.companyName}</TableCell>
                      <TableCell>
                        <Chip
                          label={STATUS_LABEL[app.status]}
                          size="small"
                          sx={{
                            bgcolor: STATUS_COLOR[app.status] + '1A',
                            color: STATUS_COLOR[app.status],
                            fontWeight: 600,
                            fontSize: '0.75rem',
                            border: `1px solid ${STATUS_COLOR[app.status]}33`,
                          }}
                        />
                      </TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>{fmt(app.appliedAt)}</TableCell>
                      <TableCell sx={{ color: 'text.secondary' }}>{fmt(app.updatedAt)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {totalPages > 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
                  <Pagination
                    count={totalPages}
                    page={page + 1}
                    onChange={(_, v) => setPage(v - 1)}
                    color="primary"
                    size="small"
                  />
                </Box>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
