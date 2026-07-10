import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Chip, Pagination, CircularProgress,
} from '@mui/material';
import { adminApi } from '../../api/adminApi';

const STATUS_COLOR: Record<string, 'default' | 'info' | 'success' | 'error' | 'warning'> = {
  SCHEDULED: 'info',
  COMPLETED: 'success',
  CANCELLED: 'error',
  RESCHEDULED: 'warning',
};

function fmtDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

export default function AdminInterviewsPage() {
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const { data: interviewsPage, isLoading } = useQuery({
    queryKey: ['admin-interviews', page],
    queryFn: () => adminApi.getAllInterviews(page, PAGE_SIZE),
  });

  const interviews = interviewsPage?.content ?? [];
  const totalPages = interviewsPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        All Interviews
      </Typography>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : interviews.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No interviews found.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Candidate</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Job</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Round</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Scheduled</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {interviews.map((iv) => (
                  <TableRow key={iv.id} hover>
                    <TableCell sx={{ fontWeight: 500 }}>
                      {iv.studentFirstName} {iv.studentLastName}
                    </TableCell>
                    <TableCell>{iv.jobTitle}</TableCell>
                    <TableCell>{iv.companyName}</TableCell>
                    <TableCell>R{iv.roundNumber}</TableCell>
                    <TableCell>
                      <Typography variant="caption">{iv.interviewType}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{fmtDate(iv.scheduledAt)}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={iv.status}
                        size="small"
                        color={STATUS_COLOR[iv.status] ?? 'default'}
                      />
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
