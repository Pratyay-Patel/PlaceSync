import { useQuery } from '@tanstack/react-query';
import { Box, Typography, Card, CardContent, CircularProgress, Grid, Tooltip } from '@mui/material';
import { adminApi } from '../../api/adminApi';
import type { CompanyStats, DepartmentStats } from '../../types/admin';

function HorizontalBar({
  label,
  value,
  max,
  color = 'primary.main',
  suffix = '',
}: {
  label: string;
  value: number;
  max: number;
  color?: string;
  suffix?: string;
}) {
  const pct = max > 0 ? (value / max) * 100 : 0;
  return (
    <Box sx={{ mb: 1.5 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="body2" noWrap sx={{ maxWidth: '70%' }}>
          {label}
        </Typography>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          {value}{suffix}
        </Typography>
      </Box>
      <Tooltip title={`${value}${suffix} (${pct.toFixed(1)}%)`} placement="right">
        <Box sx={{ height: 8, bgcolor: 'grey.200', borderRadius: 4, overflow: 'hidden' }}>
          <Box
            sx={{
              height: '100%',
              width: `${pct}%`,
              bgcolor: color,
              borderRadius: 4,
              transition: 'width 0.6s ease',
            }}
          />
        </Box>
      </Tooltip>
    </Box>
  );
}

function CompanyOffersChart({ data }: { data: CompanyStats[] }) {
  const maxOffers = Math.max(...data.map((d) => d.offerCount), 1);
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
          Top Companies — Offers
        </Typography>
        {data.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No data available.
          </Typography>
        ) : (
          data.map((c) => (
            <HorizontalBar
              key={c.companyId}
              label={c.companyName}
              value={c.offerCount}
              max={maxOffers}
              color="primary.main"
            />
          ))
        )}
      </CardContent>
    </Card>
  );
}

function DepartmentChart({ data }: { data: DepartmentStats[] }) {
  const maxRate = Math.max(...data.map((d) => d.placementRate), 1);
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
          Department Placement Rates
        </Typography>
        {data.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No data available.
          </Typography>
        ) : (
          data.map((d) => (
            <HorizontalBar
              key={d.department}
              label={d.department}
              value={Math.round(d.placementRate * 10) / 10}
              max={maxRate}
              color="success.main"
              suffix="%"
            />
          ))
        )}
      </CardContent>
    </Card>
  );
}

function CompanyApplicationChart({ data }: { data: CompanyStats[] }) {
  const maxApps = Math.max(...data.map((d) => d.applicationCount), 1);
  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
          Top Companies — Applications
        </Typography>
        {data.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No data available.
          </Typography>
        ) : (
          data.map((c) => (
            <HorizontalBar
              key={c.companyId}
              label={c.companyName}
              value={c.applicationCount}
              max={maxApps}
              color="info.main"
            />
          ))
        )}
      </CardContent>
    </Card>
  );
}

export default function AnalyticsPage() {
  const { data: companies, isLoading: companiesLoading } = useQuery({
    queryKey: ['admin-company-breakdown'],
    queryFn: adminApi.getCompanyBreakdown,
  });

  const { data: departments, isLoading: deptLoading } = useQuery({
    queryKey: ['admin-department-breakdown'],
    queryFn: adminApi.getDepartmentBreakdown,
  });

  const isLoading = companiesLoading || deptLoading;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Analytics
      </Typography>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <CompanyOffersChart data={companies ?? []} />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <DepartmentChart data={departments ?? []} />
          </Grid>
          {companies && companies.length > 0 && (
            <Grid size={{ xs: 12, md: 6 }}>
              <CompanyApplicationChart data={companies} />
            </Grid>
          )}
        </Grid>
      )}
    </Box>
  );
}
