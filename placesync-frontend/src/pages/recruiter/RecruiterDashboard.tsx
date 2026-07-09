import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, CardActionArea, Grid,
  CircularProgress, Button, Chip, Divider,
} from '@mui/material';
import {
  WorkRounded, PeopleAltRounded, StarRounded, EmojiEventsRounded,
  AddRounded, ArrowForwardRounded,
} from '@mui/icons-material';
import { recruiterApi } from '../../api/recruiterApi';

const STATUS_COLOR: Record<string, 'default' | 'warning' | 'success' | 'error' | 'info'> = {
  PENDING_APPROVAL: 'warning',
  OPEN: 'success',
  CLOSED: 'default',
  EXPIRED: 'error',
};

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

function StatCard({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: number | undefined;
  color: string;
}) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 2,
              bgcolor: `${color}.light`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: `${color}.dark`,
              flexShrink: 0,
            }}
          >
            {icon}
          </Box>
          <Box>
            <Typography variant="h5" sx={{ fontWeight: 700, lineHeight: 1 }}>
              {value ?? '—'}
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.25 }}>
              {label}
            </Typography>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}

export default function RecruiterDashboard() {
  const navigate = useNavigate();

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['recruiter-stats'],
    queryFn: recruiterApi.getStats,
  });

  const { data: jobsPage, isLoading: jobsLoading } = useQuery({
    queryKey: ['recruiter-jobs', 0],
    queryFn: () => recruiterApi.getMyJobs(0, 5),
  });

  const recentJobs = jobsPage?.content ?? [];

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>
          Dashboard
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddRounded />}
          onClick={() => navigate('/recruiter/jobs/create')}
        >
          Post a Job
        </Button>
      </Box>

      {statsLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={2} sx={{ mb: 4 }}>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              icon={<WorkRounded />}
              label="Jobs Posted"
              value={stats?.jobsPosted}
              color="primary"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              icon={<PeopleAltRounded />}
              label="Total Applications"
              value={stats?.totalApplications}
              color="info"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              icon={<StarRounded />}
              label="Shortlisted"
              value={stats?.shortlisted}
              color="warning"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <StatCard
              icon={<EmojiEventsRounded />}
              label="Offers Extended"
              value={stats?.offers}
              color="success"
            />
          </Grid>
        </Grid>
      )}

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              Recent Job Postings
            </Typography>
            <Button
              size="small"
              endIcon={<ArrowForwardRounded />}
              onClick={() => navigate('/recruiter/jobs')}
            >
              View all
            </Button>
          </Box>

          {jobsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
              <CircularProgress size={24} />
            </Box>
          ) : recentJobs.length === 0 ? (
            <Box sx={{ py: 4, textAlign: 'center' }}>
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
            <Box>
              {recentJobs.map((job, idx) => (
                <Box key={job.id}>
                  {idx > 0 && <Divider sx={{ my: 1 }} />}
                  <CardActionArea
                    sx={{ p: 1.5, borderRadius: 1 }}
                    onClick={() => navigate(`/recruiter/jobs/${job.id}/applications`)}
                  >
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <Box>
                        <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                          {job.title}
                        </Typography>
                        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                          {job.companyName} &bull; Deadline: {fmt(job.applicationDeadline)}
                        </Typography>
                      </Box>
                      <Chip
                        label={job.status.replace('_', ' ')}
                        size="small"
                        color={STATUS_COLOR[job.status] ?? 'default'}
                      />
                    </Box>
                  </CardActionArea>
                </Box>
              ))}
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
