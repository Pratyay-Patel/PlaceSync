import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Button, TextField, FormControl,
  InputLabel, Select, MenuItem, CircularProgress, Alert, Divider,
  Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions,
  Table, TableBody, TableCell, TableHead, TableRow, TableContainer,
} from '@mui/material';
import {
  ArrowBackRounded, AddRounded, CancelRounded, CheckRounded, EditRounded,
} from '@mui/icons-material';
import { recruiterApi } from '../../api/recruiterApi';
import type { InterviewType } from '../../types/interview';
import StatusChip from '../../components/common/StatusChip';

const INTERVIEW_TYPES: InterviewType[] = ['ONLINE', 'OFFLINE'];

const TYPE_LABEL: Record<InterviewType, string> = {
  ONLINE: 'Online',
  OFFLINE: 'Offline',
};

function fmtDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function toLocalDatetimeValue(iso: string) {
  return iso ? iso.slice(0, 16) : '';
}

export default function ScheduleInterviewPage() {
  const { jobId, applicationId } = useParams<{ jobId: string; applicationId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [scheduleError, setScheduleError] = useState('');
  const [rescheduleError, setRescheduleError] = useState('');
  const [cancelError, setCancelError] = useState('');
  const [cancelTargetId, setCancelTargetId] = useState<string | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [rescheduleTargetId, setRescheduleTargetId] = useState<string | null>(null);
  const [rescheduleForm, setRescheduleForm] = useState({ scheduledAt: '', meetingLink: '', venue: '' });

  const [form, setForm] = useState({
    roundNumber: 1,
    interviewType: 'ONLINE' as InterviewType,
    scheduledAt: '',
    durationMinutes: 60,
    meetingLink: '',
    venue: '',
  });

  const { data: interviews, isLoading } = useQuery({
    queryKey: ['application-interviews', applicationId],
    queryFn: () => recruiterApi.getApplicationInterviews(applicationId!),
    enabled: !!applicationId,
  });

  const scheduleMutation = useMutation({
    mutationFn: () =>
      recruiterApi.scheduleInterview(applicationId!, {
        ...form,
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        meetingLink: form.meetingLink || undefined,
        venue: form.venue || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application-interviews', applicationId] });
      setScheduleError('');
      setForm({ roundNumber: 1, interviewType: 'ONLINE', scheduledAt: '', durationMinutes: 60, meetingLink: '', venue: '' });
    },
    onError: (err: { response?: { data?: { message?: string; fieldErrors?: Array<{ field: string; message: string }> } } }) => {
      const data = err?.response?.data;
      if (data?.message && data.message !== 'Validation failed') {
        setScheduleError(data.message);
      } else if (data?.fieldErrors?.[0]) {
        setScheduleError(data.fieldErrors[0].message);
      } else {
        setScheduleError(data?.message ?? 'Failed to schedule interview.');
      }
    },
  });

  const rescheduleMutation = useMutation({
    mutationFn: () =>
      recruiterApi.rescheduleInterview(rescheduleTargetId!, {
        scheduledAt: new Date(rescheduleForm.scheduledAt).toISOString(),
        meetingLink: rescheduleForm.meetingLink || undefined,
        venue: rescheduleForm.venue || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application-interviews', applicationId] });
      setRescheduleTargetId(null);
      setRescheduleError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setRescheduleError(err?.response?.data?.message ?? 'Failed to reschedule interview.');
    },
  });

  const cancelMutation = useMutation({
    mutationFn: () => recruiterApi.cancelInterview(cancelTargetId!, cancelReason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application-interviews', applicationId] });
      setCancelTargetId(null);
      setCancelReason('');
      setCancelError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setCancelError(err?.response?.data?.message ?? 'Failed to cancel interview.');
    },
  });

  const completeMutation = useMutation({
    mutationFn: (interviewId: string) => recruiterApi.completeInterview(interviewId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application-interviews', applicationId] });
    },
  });

  return (
    <Box>
      <Button
        startIcon={<ArrowBackRounded />}
        onClick={() => navigate(`/recruiter/jobs/${jobId}/applications`)}
        sx={{ mb: 2 }}
        size="small"
      >
        Back to Applicants
      </Button>

      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Interview Schedule
      </Typography>

      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' }, gap: 3 }}>
        {/* Existing interviews */}
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Scheduled Rounds
            </Typography>

            {isLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                <CircularProgress size={24} />
              </Box>
            ) : !interviews || interviews.length === 0 ? (
              <Typography color="text.secondary" variant="body2">
                No interviews scheduled yet.
              </Typography>
            ) : (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600 }}>Round</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Date</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 600 }} align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {interviews.map((iv) => (
                      <TableRow key={iv.id}>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontWeight: 500 }}>
                            R{iv.roundNumber} — {TYPE_LABEL[iv.interviewType]}
                          </Typography>
                          {iv.meetingLink && (
                            <Typography
                              variant="caption"
                              component="a"
                              href={iv.meetingLink}
                              target="_blank"
                              rel="noreferrer"
                              sx={{ color: 'primary.main' }}
                            >
                              Join link
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption">{fmtDate(iv.scheduledAt)}</Typography>
                        </TableCell>
                        <TableCell>
                          <StatusChip status={iv.status} />
                        </TableCell>
                        <TableCell align="right">
                          {(iv.status === 'SCHEDULED' || iv.status === 'RESCHEDULED') && (
                            <>
                              <Button
                                size="small"
                                startIcon={<EditRounded fontSize="small" />}
                                onClick={() => {
                                  setRescheduleTargetId(iv.id);
                                  setRescheduleForm({
                                    scheduledAt: toLocalDatetimeValue(iv.scheduledAt),
                                    meetingLink: iv.meetingLink ?? '',
                                    venue: iv.venue ?? '',
                                  });
                                  setRescheduleError('');
                                }}
                                sx={{ mr: 0.5 }}
                              >
                                Reschedule
                              </Button>
                              <Button
                                size="small"
                                color="success"
                                startIcon={<CheckRounded fontSize="small" />}
                                onClick={() => completeMutation.mutate(iv.id)}
                                sx={{ mr: 0.5 }}
                              >
                                Complete
                              </Button>
                              <Button
                                size="small"
                                color="error"
                                startIcon={<CancelRounded fontSize="small" />}
                                onClick={() => { setCancelTargetId(iv.id); setCancelError(''); }}
                              >
                                Cancel
                              </Button>
                            </>
                          )}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </CardContent>
        </Card>

        {/* Schedule new interview */}
        <Card>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>
              Schedule New Round
            </Typography>

            {scheduleError && <Alert severity="error" sx={{ mb: 2 }}>{scheduleError}</Alert>}

            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <TextField
                  label="Round Number"
                  type="number"
                  value={form.roundNumber}
                  onChange={(e) => setForm((f) => ({ ...f, roundNumber: Number(e.target.value) }))}
                  inputProps={{ min: 1 }}
                  size="small"
                />
                <FormControl size="small">
                  <InputLabel>Interview Type</InputLabel>
                  <Select
                    label="Interview Type"
                    value={form.interviewType}
                    onChange={(e) => setForm((f) => ({ ...f, interviewType: e.target.value as InterviewType }))}
                  >
                    {INTERVIEW_TYPES.map((t) => (
                      <MenuItem key={t} value={t}>{TYPE_LABEL[t]}</MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>

              <Box>
                <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
                  Date &amp; Time *
                </Typography>
                <TextField
                  type="datetime-local"
                  value={form.scheduledAt}
                  onChange={(e) => setForm((f) => ({ ...f, scheduledAt: e.target.value }))}
                  size="small"
                  fullWidth
                />
              </Box>

              <TextField
                label="Duration (minutes)"
                type="number"
                value={form.durationMinutes}
                onChange={(e) => setForm((f) => ({ ...f, durationMinutes: Number(e.target.value) }))}
                inputProps={{ min: 15, step: 15 }}
                size="small"
              />

              <TextField
                label="Meeting Link (optional)"
                value={form.meetingLink}
                onChange={(e) => setForm((f) => ({ ...f, meetingLink: e.target.value }))}
                size="small"
                placeholder="https://meet.google.com/..."
              />

              <TextField
                label="Venue (optional)"
                value={form.venue}
                onChange={(e) => setForm((f) => ({ ...f, venue: e.target.value }))}
                size="small"
              />

              <Button
                variant="contained"
                startIcon={<AddRounded />}
                disabled={!form.scheduledAt || scheduleMutation.isPending}
                onClick={() => scheduleMutation.mutate()}
              >
                {scheduleMutation.isPending ? 'Scheduling…' : 'Schedule Interview'}
              </Button>
            </Box>
          </CardContent>
        </Card>
      </Box>

      {/* Cancel dialog */}
      <Dialog open={!!cancelTargetId} onClose={() => setCancelTargetId(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Cancel Interview</DialogTitle>
        <DialogContent>
          {cancelError && <Alert severity="error" sx={{ mb: 2 }}>{cancelError}</Alert>}
          <DialogContentText sx={{ mb: 2 }}>
            Please provide a reason for cancelling this interview.
          </DialogContentText>
          <TextField
            label="Cancellation Reason"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            fullWidth
            required
            multiline
            rows={2}
            size="small"
          />
        </DialogContent>
        <Divider />
        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={() => setCancelTargetId(null)}>Dismiss</Button>
          <Button
            variant="contained"
            color="error"
            disabled={!cancelReason.trim() || cancelMutation.isPending}
            onClick={() => cancelMutation.mutate()}
          >
            {cancelMutation.isPending ? 'Cancelling…' : 'Cancel Interview'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reschedule dialog */}
      <Dialog open={!!rescheduleTargetId} onClose={() => setRescheduleTargetId(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Reschedule Interview</DialogTitle>
        <DialogContent>
          {rescheduleError && <Alert severity="error" sx={{ mb: 2 }}>{rescheduleError}</Alert>}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <Box>
              <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
                New Date &amp; Time *
              </Typography>
              <TextField
                type="datetime-local"
                value={rescheduleForm.scheduledAt}
                onChange={(e) => setRescheduleForm((f) => ({ ...f, scheduledAt: e.target.value }))}
                size="small"
                fullWidth
              />
            </Box>
            <TextField
              label="Meeting Link"
              value={rescheduleForm.meetingLink}
              onChange={(e) => setRescheduleForm((f) => ({ ...f, meetingLink: e.target.value }))}
              size="small"
            />
            <TextField
              label="Venue"
              value={rescheduleForm.venue}
              onChange={(e) => setRescheduleForm((f) => ({ ...f, venue: e.target.value }))}
              size="small"
            />
          </Box>
        </DialogContent>
        <Divider />
        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={() => setRescheduleTargetId(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!rescheduleForm.scheduledAt || rescheduleMutation.isPending}
            onClick={() => rescheduleMutation.mutate()}
          >
            {rescheduleMutation.isPending ? 'Rescheduling…' : 'Reschedule'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
