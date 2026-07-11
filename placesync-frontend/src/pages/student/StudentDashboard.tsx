import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, CircularProgress,
  Table, TableHead, TableRow, TableCell, TableBody, Button,
} from '@mui/material';
import {
  AssignmentRounded, EventNoteRounded, EmojiEventsRounded, WorkRounded,
} from '@mui/icons-material';
import { applicationApi } from '../../api/applicationApi';
import { interviewApi } from '../../api/interviewApi';
import StatusChip from '../../components/common/StatusChip';

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function StudentDashboard() {
  const navigate = useNavigate();

  const { data: appData, isLoading: appLoading } = useQuery({
    queryKey: ['student-applications', 0, 5],
    queryFn: () => applicationApi.getMyApplications(0, 5),
  });

  const { data: interviewData, isLoading: intLoading } = useQuery({
    queryKey: ['student-interviews'],
    queryFn: () => interviewApi.getMyInterviews(0, 50),
  });

  const totalApplications = appData?.totalElements ?? 0;
  const recentApplications = appData?.content ?? [];
  const upcomingInterviews = (interviewData?.content ?? []).filter(
    (i) => i.status === 'SCHEDULED',
  ).length;
  const offers = recentApplications.filter((a) => a.status === 'OFFERED').length;

  const loading = appLoading || intLoading;

  const stats = [
    {
      label: 'Applications Submitted',
      value: totalApplications,
      icon: <AssignmentRounded />,
      color: '#4F46E5',
      bg: '#EEF2FF',
      path: '/student/applications',
    },
    {
      label: 'Upcoming Interviews',
      value: upcomingInterviews,
      icon: <EventNoteRounded />,
      color: '#0891B2',
      bg: '#ECFEFF',
      path: '/student/interviews',
    },
    {
      label: 'Offers Received',
      value: offers,
      icon: <EmojiEventsRounded />,
      color: '#059669',
      bg: '#ECFDF5',
      path: '/student/applications',
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>Dashboard</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            Your placement activity at a glance
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<WorkRounded />}
          onClick={() => navigate('/student/jobs')}
        >
          Browse Jobs
        </Button>
      </Box>

      {/* Stat cards */}
      <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 2, mb: 4 }}>
        {stats.map((s) => (
          <Card
            key={s.label}
            sx={{ cursor: 'pointer', '&:hover': { boxShadow: 4 }, transition: 'box-shadow 200ms' }}
            onClick={() => navigate(s.path)}
          >
            <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <Box
                sx={{
                  width: 48, height: 48, borderRadius: 2,
                  bgcolor: s.bg, color: s.color,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  flexShrink: 0,
                }}
              >
                {s.icon}
              </Box>
              <Box>
                {loading ? (
                  <CircularProgress size={24} />
                ) : (
                  <Typography variant="h4" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
                    {s.value}
                  </Typography>
                )}
                <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.25 }}>
                  {s.label}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        ))}
      </Box>

      {/* Recent applications */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>Recent Applications</Typography>
            <Button size="small" onClick={() => navigate('/student/applications')}>
              View all
            </Button>
          </Box>

          {appLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : recentApplications.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 4 }}>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                No applications yet.{' '}
                <Button size="small" onClick={() => navigate('/student/jobs')}>Browse jobs</Button>
              </Typography>
            </Box>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Job</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Company</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Applied</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {recentApplications.map((app) => (
                  <TableRow key={app.id} hover sx={{ cursor: 'pointer' }}
                    onClick={() => navigate('/student/applications')}>
                    <TableCell sx={{ fontWeight: 500 }}>{app.jobTitle}</TableCell>
                    <TableCell>{app.companyName}</TableCell>
                    <TableCell>
                      <StatusChip status={app.status} />
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary' }}>{fmt(app.appliedAt)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
