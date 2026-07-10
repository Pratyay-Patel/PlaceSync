import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Grid, CircularProgress, LinearProgress,
} from '@mui/material';
import {
  PersonRounded, PeopleRounded, BusinessRounded, WorkRounded,
  AssignmentRounded, EmojiEventsRounded, TrendingUpRounded,
} from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';

function StatCard({
  icon,
  label,
  value,
  color,
  suffix = '',
}: {
  icon: React.ReactNode;
  label: string;
  value: number | undefined;
  color: string;
  suffix?: string;
}) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 48, height: 48, borderRadius: 2,
              bgcolor: `${color}.light`, display: 'flex',
              alignItems: 'center', justifyContent: 'center',
              color: `${color}.dark`, flexShrink: 0,
            }}
          >
            {icon}
          </Box>
          <Box>
            <Typography variant="h5" sx={{ fontWeight: 700, lineHeight: 1 }}>
              {value != null ? `${value}${suffix}` : '—'}
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

export default function AdminDashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['admin-placement-stats'],
    queryFn: adminApi.getPlacementStats,
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Admin Dashboard
      </Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <>
          <Grid container spacing={2} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<PersonRounded />} label="Students" value={stats?.totalStudents} color="primary" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<PeopleRounded />} label="Recruiters" value={stats?.totalRecruiters} color="info" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<BusinessRounded />} label="Companies" value={stats?.totalCompanies} color="secondary" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<WorkRounded />} label="Open Jobs" value={stats?.openJobs} color="warning" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<AssignmentRounded />} label="Applications" value={stats?.totalApplications} color="info" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard icon={<EmojiEventsRounded />} label="Offers" value={stats?.totalOffers} color="success" />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <StatCard
                icon={<TrendingUpRounded />}
                label="Placement Rate"
                value={stats?.placementRate != null ? Math.round(stats.placementRate * 10) / 10 : undefined}
                color="success"
                suffix="%"
              />
            </Grid>
          </Grid>

          {stats && (
            <Card>
              <CardContent>
                <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
                  Overall Placement Rate
                </Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(stats.placementRate, 100)}
                    sx={{ flexGrow: 1, height: 12, borderRadius: 6 }}
                  />
                  <Typography variant="body1" sx={{ fontWeight: 700, minWidth: 52 }}>
                    {stats.placementRate.toFixed(1)}%
                  </Typography>
                </Box>
                <Typography variant="caption" sx={{ color: 'text.secondary', mt: 0.75, display: 'block' }}>
                  {stats.totalOffers} offers out of {stats.totalStudents} students
                </Typography>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </Box>
  );
}
