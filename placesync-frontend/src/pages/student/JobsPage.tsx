import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, CardActionArea, Chip,
  CircularProgress, Pagination, TextField, MenuItem, Select,
  FormControl, InputLabel, Button, Divider,
} from '@mui/material';
import {
  SearchRounded, LocationOnRounded, WorkRounded, AccessTimeRounded,
} from '@mui/icons-material';
import { jobApi } from '../../api/jobApi';
import type { JobFilters, JobLocationType, JobType } from '../../types/job';

const LOCATION_OPTIONS: { value: JobLocationType | ''; label: string }[] = [
  { value: '', label: 'All locations' },
  { value: 'ONSITE', label: 'On-site' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
];

const TYPE_OPTIONS: { value: JobType | ''; label: string }[] = [
  { value: '', label: 'All types' },
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'CONTRACT', label: 'Contract' },
];

const LOCATION_LABEL: Record<JobLocationType, string> = {
  ONSITE: 'On-site',
  REMOTE: 'Remote',
  HYBRID: 'Hybrid',
};

const TYPE_LABEL: Record<JobType, string> = {
  FULL_TIME: 'Full-time',
  INTERNSHIP: 'Internship',
  CONTRACT: 'Contract',
};

function fmtDeadline(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

const PAGE_SIZE = 12;

export default function StudentJobsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [locationType, setLocationType] = useState<JobLocationType | ''>('');
  const [jobType, setJobType] = useState<JobType | ''>('');
  const [activeFilters, setActiveFilters] = useState<JobFilters>({});

  const { data, isLoading } = useQuery({
    queryKey: ['jobs', activeFilters, page],
    queryFn: () => jobApi.list(activeFilters, page, PAGE_SIZE),
  });

  const jobs = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  function applyFilters() {
    setPage(0);
    setActiveFilters({
      keyword: keyword || undefined,
      locationType: locationType || undefined,
      jobType: jobType || undefined,
    });
  }

  function clearFilters() {
    setKeyword('');
    setLocationType('');
    setJobType('');
    setPage(0);
    setActiveFilters({});
  }

  return (
    <Box sx={{ display: 'flex', gap: 3 }}>
      {/* Filter sidebar */}
      <Box sx={{ width: 240, flexShrink: 0 }}>
        <Card>
          <CardContent>
            <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 2 }}>
              Filters
            </Typography>

            <TextField
              label="Keyword"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              size="small"
              fullWidth
              sx={{ mb: 2 }}
              onKeyDown={(e) => { if (e.key === 'Enter') applyFilters(); }}
              slotProps={{ input: { startAdornment: <SearchRounded fontSize="small" sx={{ mr: 1, color: 'text.secondary' }} /> } }}
            />

            <FormControl size="small" fullWidth sx={{ mb: 2 }}>
              <InputLabel>Location</InputLabel>
              <Select
                label="Location"
                value={locationType}
                onChange={(e) => setLocationType(e.target.value as JobLocationType | '')}
              >
                {LOCATION_OPTIONS.map((o) => (
                  <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" fullWidth sx={{ mb: 2.5 }}>
              <InputLabel>Job Type</InputLabel>
              <Select
                label="Job Type"
                value={jobType}
                onChange={(e) => setJobType(e.target.value as JobType | '')}
              >
                {TYPE_OPTIONS.map((o) => (
                  <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <Button variant="contained" fullWidth onClick={applyFilters} sx={{ mb: 1 }}>
              Apply
            </Button>
            <Button variant="text" fullWidth onClick={clearFilters} size="small">
              Clear filters
            </Button>
          </CardContent>
        </Card>
      </Box>

      {/* Job list */}
      <Box sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>Browse Jobs</Typography>
          {data && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {data.totalElements} job{data.totalElements !== 1 ? 's' : ''} found
            </Typography>
          )}
        </Box>

        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : jobs.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              No jobs found. Try adjusting your filters.
            </Typography>
          </Box>
        ) : (
          <>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 2, mb: 3 }}>
              {jobs.map((job) => {
                const deadlinePast = new Date(job.applicationDeadline) < new Date();
                return (
                  <Card key={job.id} sx={{ height: '100%' }}>
                    <CardActionArea
                      onClick={() => navigate(`/student/jobs/${job.id}`)}
                      sx={{ height: '100%', alignItems: 'flex-start', display: 'flex', flexDirection: 'column' }}
                    >
                      <CardContent sx={{ flexGrow: 1, width: '100%' }}>
                        <Typography variant="subtitle1" sx={{ fontWeight: 600, mb: 0.5 }}>
                          {job.title}
                        </Typography>
                        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1.5 }}>
                          {job.companyName}
                        </Typography>

                        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1.5 }}>
                          <Chip
                            icon={<LocationOnRounded />}
                            label={LOCATION_LABEL[job.locationType]}
                            size="small"
                            variant="outlined"
                          />
                          <Chip
                            icon={<WorkRounded />}
                            label={TYPE_LABEL[job.jobType]}
                            size="small"
                            variant="outlined"
                          />
                        </Box>

                        {job.compensation && (
                          <Typography variant="body2" sx={{ fontWeight: 500, mb: 1 }}>
                            {job.compensation}
                          </Typography>
                        )}

                        <Divider sx={{ my: 1 }} />

                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                          <AccessTimeRounded fontSize="small" sx={{ color: deadlinePast ? 'error.main' : 'text.secondary', fontSize: '0.875rem' }} />
                          <Typography variant="caption" sx={{ color: deadlinePast ? 'error.main' : 'text.secondary' }}>
                            Deadline: {fmtDeadline(job.applicationDeadline)}
                          </Typography>
                        </Box>
                      </CardContent>
                    </CardActionArea>
                  </Card>
                );
              })}
            </Box>

            {totalPages > 1 && (
              <Box sx={{ display: 'flex', justifyContent: 'center' }}>
                <Pagination
                  count={totalPages}
                  page={page + 1}
                  onChange={(_, v) => setPage(v - 1)}
                  color="primary"
                />
              </Box>
            )}
          </>
        )}
      </Box>
    </Box>
  );
}
