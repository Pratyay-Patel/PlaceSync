import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Button, Card, CardContent, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Chip, CircularProgress, Alert,
  FormControl, Select, MenuItem, Dialog, DialogTitle, DialogContent,
  DialogContentText, DialogActions, IconButton, Tooltip,
} from '@mui/material';
import {
  ArrowBackRounded, VideoCallRounded, DownloadRounded, PersonRounded,
} from '@mui/icons-material';
import { recruiterApi } from '../../api/recruiterApi';
import { jobApi } from '../../api/jobApi';
import type { ApplicationStatus } from '../../types/application';

const STATUS_COLOR: Record<ApplicationStatus, 'default' | 'info' | 'warning' | 'primary' | 'success' | 'error'> = {
  APPLIED: 'info',
  UNDER_REVIEW: 'warning',
  SHORTLISTED: 'primary',
  INTERVIEW_SCHEDULED: 'primary',
  OFFERED: 'success',
  REJECTED: 'error',
};

const VALID_TRANSITIONS: Record<ApplicationStatus, ApplicationStatus[]> = {
  APPLIED: ['UNDER_REVIEW', 'REJECTED'],
  UNDER_REVIEW: ['SHORTLISTED', 'REJECTED'],
  SHORTLISTED: ['INTERVIEW_SCHEDULED', 'REJECTED'],
  INTERVIEW_SCHEDULED: ['OFFERED', 'REJECTED'],
  OFFERED: [],
  REJECTED: [],
};

const STATUS_LABELS: Record<ApplicationStatus, string> = {
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

export default function RecruiterApplicationsPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [profileDialog, setProfileDialog] = useState<{
    name: string;
    studentId: string;
    resumeLabel: string;
  } | null>(null);
  const [statusError, setStatusError] = useState('');

  const { data: job } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => jobApi.getById(jobId!),
    enabled: !!jobId,
  });

  const { data: appsPage, isLoading } = useQuery({
    queryKey: ['recruiter-applications', jobId],
    queryFn: () => recruiterApi.getJobApplications(jobId!, 0, 50),
    enabled: !!jobId,
  });

  const applications = appsPage?.content ?? [];

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: ApplicationStatus }) =>
      recruiterApi.updateApplicationStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['recruiter-applications', jobId] });
      setStatusError('');
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setStatusError(err?.response?.data?.message ?? 'Failed to update status.');
    },
  });

  const downloadResume = async (resumeId: string) => {
    try {
      const { downloadUrl } = await recruiterApi.getResumeDownloadUrl(resumeId);
      window.open(downloadUrl, '_blank');
    } catch {
      // silently fail — user sees no URL opens
    }
  };

  return (
    <Box>
      <Button
        startIcon={<ArrowBackRounded />}
        onClick={() => navigate('/recruiter/jobs')}
        sx={{ mb: 2 }}
        size="small"
      >
        Back to Jobs
      </Button>

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Applicants
          </Typography>
          {job && (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {job.title} &bull; {job.companyName}
            </Typography>
          )}
        </Box>
      </Box>

      {statusError && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setStatusError('')}>
          {statusError}
        </Alert>
      )}

      <Card>
        <CardContent sx={{ p: 0 }}>
          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
              <CircularProgress />
            </Box>
          ) : applications.length === 0 ? (
            <Box sx={{ py: 6, textAlign: 'center' }}>
              <Typography color="text.secondary">No applications yet.</Typography>
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ fontWeight: 600 }}>Applicant</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Resume</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Applied On</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Status</TableCell>
                    <TableCell sx={{ fontWeight: 600 }} align="right">
                      Actions
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {applications.map((app) => {
                    const transitions = VALID_TRANSITIONS[app.status] ?? [];
                    return (
                      <TableRow key={app.id} hover>
                        <TableCell>
                          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                            {app.studentFirstName} {app.studentLastName}
                          </Typography>
                        </TableCell>
                        <TableCell>{app.resumeLabel}</TableCell>
                        <TableCell>{fmt(app.appliedAt)}</TableCell>
                        <TableCell>
                          {transitions.length > 0 ? (
                            <FormControl size="small" sx={{ minWidth: 160 }}>
                              <Select
                                value={app.status}
                                onChange={(e) =>
                                  statusMutation.mutate({
                                    id: app.id,
                                    status: e.target.value as ApplicationStatus,
                                  })
                                }
                                renderValue={(v) => (
                                  <Chip
                                    label={STATUS_LABELS[v as ApplicationStatus]}
                                    size="small"
                                    color={STATUS_COLOR[v as ApplicationStatus]}
                                  />
                                )}
                              >
                                <MenuItem value={app.status} disabled>
                                  <Chip
                                    label={STATUS_LABELS[app.status]}
                                    size="small"
                                    color={STATUS_COLOR[app.status]}
                                  />
                                </MenuItem>
                                {transitions.map((t) => (
                                  <MenuItem key={t} value={t}>
                                    <Chip
                                      label={STATUS_LABELS[t]}
                                      size="small"
                                      color={STATUS_COLOR[t]}
                                    />
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          ) : (
                            <Chip
                              label={STATUS_LABELS[app.status]}
                              size="small"
                              color={STATUS_COLOR[app.status]}
                            />
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="View Profile">
                            <IconButton
                              size="small"
                              onClick={() =>
                                setProfileDialog({
                                  name: `${app.studentFirstName} ${app.studentLastName}`,
                                  studentId: app.studentId,
                                  resumeLabel: app.resumeLabel,
                                })
                              }
                            >
                              <PersonRounded fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Download Resume">
                            <IconButton
                              size="small"
                              onClick={() => downloadResume(app.resumeId)}
                            >
                              <DownloadRounded fontSize="small" />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Schedule Interview">
                            <IconButton
                              size="small"
                              onClick={() =>
                                navigate(`/recruiter/jobs/${jobId}/applications/${app.id}`)
                              }
                            >
                              <VideoCallRounded fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!profileDialog} onClose={() => setProfileDialog(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Applicant Profile</DialogTitle>
        <DialogContent>
          <DialogContentText component="div">
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <Typography>
                <strong>Name:</strong> {profileDialog?.name}
              </Typography>
              <Typography>
                <strong>Resume:</strong> {profileDialog?.resumeLabel}
              </Typography>
              <Typography variant="caption" sx={{ color: 'text.secondary', mt: 1 }}>
                Full student profile access is available for verified recruiters via the student profile
                endpoint in the API.
              </Typography>
            </Box>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setProfileDialog(null)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
