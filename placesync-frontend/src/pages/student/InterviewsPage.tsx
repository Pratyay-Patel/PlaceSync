import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Tabs, Tab, Card, CardContent, CircularProgress,
  Chip, Divider, Link,
} from '@mui/material';
import {
  EventNoteRounded, VideocamRounded, PhoneRounded,
  BusinessRounded, LocationOnRounded,
} from '@mui/icons-material';
import { interviewApi } from '../../api/interviewApi';
import type { Interview, InterviewStatus, InterviewType } from '../../types/interview';

const TYPE_LABEL: Record<InterviewType, string> = {
  PHONE: 'Phone',
  VIDEO: 'Video',
  ONSITE: 'On-site',
  TECHNICAL: 'Technical',
  HR: 'HR',
};

const TYPE_ICON: Record<InterviewType, React.ReactNode> = {
  PHONE: <PhoneRounded fontSize="small" />,
  VIDEO: <VideocamRounded fontSize="small" />,
  ONSITE: <BusinessRounded fontSize="small" />,
  TECHNICAL: <BusinessRounded fontSize="small" />,
  HR: <BusinessRounded fontSize="small" />,
};

const STATUS_COLOR: Record<InterviewStatus, string> = {
  SCHEDULED: '#06B6D4',
  COMPLETED: '#10B981',
  CANCELLED: '#EF4444',
  RESCHEDULED: '#F59E0B',
};

const STATUS_LABEL: Record<InterviewStatus, string> = {
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
  RESCHEDULED: 'Rescheduled',
};

function fmtDateTime(dateStr: string) {
  return new Date(dateStr).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function InterviewCard({ interview }: { interview: Interview }) {
  return (
    <Card sx={{ mb: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 1 }}>
          <Box>
            <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
              {interview.jobTitle}
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {interview.companyName} · Round {interview.roundNumber}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Chip
              icon={TYPE_ICON[interview.interviewType] as React.ReactElement}
              label={TYPE_LABEL[interview.interviewType]}
              size="small"
              variant="outlined"
            />
            <Chip
              label={STATUS_LABEL[interview.status]}
              size="small"
              sx={{
                bgcolor: STATUS_COLOR[interview.status] + '1A',
                color: STATUS_COLOR[interview.status],
                fontWeight: 600,
                border: `1px solid ${STATUS_COLOR[interview.status]}33`,
              }}
            />
          </Box>
        </Box>

        <Divider sx={{ my: 1.5 }} />

        <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
            <EventNoteRounded fontSize="small" sx={{ color: 'text.secondary' }} />
            <Typography variant="body2">{fmtDateTime(interview.scheduledAt)}</Typography>
          </Box>

          {interview.durationMinutes && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {interview.durationMinutes} min
            </Typography>
          )}

          {interview.venue && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75 }}>
              <LocationOnRounded fontSize="small" sx={{ color: 'text.secondary' }} />
              <Typography variant="body2">{interview.venue}</Typography>
            </Box>
          )}

          {interview.meetingLink && (
            <Link href={interview.meetingLink} target="_blank" rel="noreferrer" variant="body2">
              Join meeting
            </Link>
          )}
        </Box>

        {interview.cancellationReason && (
          <Typography variant="body2" sx={{ color: 'error.main', mt: 1 }}>
            Cancelled: {interview.cancellationReason}
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}

export default function InterviewsPage() {
  const [tab, setTab] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['student-interviews'],
    queryFn: () => interviewApi.getMyInterviews(0, 100),
  });

  const all = data?.content ?? [];
  const upcoming = all.filter((i) => i.status === 'SCHEDULED' || i.status === 'RESCHEDULED');
  const past = all.filter((i) => i.status === 'COMPLETED' || i.status === 'CANCELLED');

  const list = tab === 0 ? upcoming : past;

  return (
    <Box>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 700 }}>My Interviews</Typography>
        <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
          Track your scheduled and past interviews
        </Typography>
      </Box>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label={`Upcoming (${upcoming.length})`} />
        <Tab label={`Past (${past.length})`} />
      </Tabs>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : list.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {tab === 0 ? 'No upcoming interviews.' : 'No past interviews.'}
          </Typography>
        </Box>
      ) : (
        list.map((interview) => (
          <InterviewCard key={interview.id} interview={interview} />
        ))
      )}
    </Box>
  );
}
