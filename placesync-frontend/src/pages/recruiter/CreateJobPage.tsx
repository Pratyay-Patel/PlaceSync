import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, TextField, Button, Alert,
  FormControl, InputLabel, Select, MenuItem, Chip, InputAdornment,
} from '@mui/material';
import { AddRounded, ArrowBackRounded } from '@mui/icons-material';
import { jobApi, type JobFormData } from '../../api/jobApi';

const LOCATION_TYPES = ['ONSITE', 'REMOTE', 'HYBRID'];
const JOB_TYPES = ['FULL_TIME', 'PART_TIME', 'INTERNSHIP', 'CONTRACT'];

const LOCATION_LABEL: Record<string, string> = {
  ONSITE: 'On-site',
  REMOTE: 'Remote',
  HYBRID: 'Hybrid',
};
const TYPE_LABEL: Record<string, string> = {
  FULL_TIME: 'Full-time',
  PART_TIME: 'Part-time',
  INTERNSHIP: 'Internship',
  CONTRACT: 'Contract',
};

function toLocalDatetimeValue(iso: string) {
  return iso ? iso.slice(0, 16) : '';
}

function toISOFromLocalDatetime(local: string) {
  return local ? new Date(local).toISOString() : '';
}

interface JobFormProps {
  initialData?: Partial<JobFormData>;
  onSubmit: (data: JobFormData) => void;
  isPending: boolean;
  error: string;
  submitLabel: string;
}

export function JobForm({ initialData, onSubmit, isPending, error, submitLabel }: JobFormProps) {
  const navigate = useNavigate();
  const [form, setForm] = useState<JobFormData>({
    title: initialData?.title ?? '',
    description: initialData?.description ?? '',
    locationType: initialData?.locationType ?? 'ONSITE',
    jobType: initialData?.jobType ?? 'FULL_TIME',
    locationCity: initialData?.locationCity ?? '',
    compensation: initialData?.compensation ?? '',
    applicationDeadline: initialData?.applicationDeadline
      ? toLocalDatetimeValue(initialData.applicationDeadline)
      : '',
    minCgpa: initialData?.minCgpa ?? null,
    requiredSkills: initialData?.requiredSkills ?? [],
    eligibleDepartments: initialData?.eligibleDepartments ?? [],
  });

  const [skillInput, setSkillInput] = useState('');
  const [deptInput, setDeptInput] = useState('');

  const addSkill = () => {
    const v = skillInput.trim();
    if (v && !form.requiredSkills.includes(v)) {
      setForm((f) => ({ ...f, requiredSkills: [...f.requiredSkills, v] }));
    }
    setSkillInput('');
  };

  const addDept = () => {
    const v = deptInput.trim();
    if (v && !form.eligibleDepartments.includes(v)) {
      setForm((f) => ({ ...f, eligibleDepartments: [...f.eligibleDepartments, v] }));
    }
    setDeptInput('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({
      ...form,
      applicationDeadline: toISOFromLocalDatetime(form.applicationDeadline as string),
      locationCity: form.locationCity || undefined,
      compensation: form.compensation || undefined,
      minCgpa: form.minCgpa !== null && form.minCgpa !== undefined && String(form.minCgpa) !== ''
        ? Number(form.minCgpa)
        : null,
    });
  };

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}

      <Card>
        <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Basic Information
          </Typography>

          <TextField
            label="Job Title"
            value={form.title}
            onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            required
            size="small"
            fullWidth
          />

          <TextField
            label="Description"
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            required
            multiline
            rows={5}
            size="small"
            fullWidth
          />

          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            <FormControl size="small" required>
              <InputLabel>Location Type</InputLabel>
              <Select
                label="Location Type"
                value={form.locationType}
                onChange={(e) => setForm((f) => ({ ...f, locationType: e.target.value }))}
              >
                {LOCATION_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{LOCATION_LABEL[t]}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" required>
              <InputLabel>Job Type</InputLabel>
              <Select
                label="Job Type"
                value={form.jobType}
                onChange={(e) => setForm((f) => ({ ...f, jobType: e.target.value }))}
              >
                {JOB_TYPES.map((t) => (
                  <MenuItem key={t} value={t}>{TYPE_LABEL[t]}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            <TextField
              label="City (optional)"
              value={form.locationCity}
              onChange={(e) => setForm((f) => ({ ...f, locationCity: e.target.value }))}
              size="small"
            />
            <TextField
              label="Compensation (optional)"
              value={form.compensation}
              onChange={(e) => setForm((f) => ({ ...f, compensation: e.target.value }))}
              size="small"
              placeholder="e.g. ₹12 LPA"
            />
          </Box>

          <TextField
            label="Application Deadline"
            type="datetime-local"
            value={form.applicationDeadline}
            onChange={(e) => setForm((f) => ({ ...f, applicationDeadline: e.target.value }))}
            required
            size="small"
            InputLabelProps={{ shrink: true }}
          />
        </CardContent>
      </Card>

      <Card>
        <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Eligibility Criteria
          </Typography>

          <TextField
            label="Minimum CGPA (optional)"
            type="number"
            value={form.minCgpa ?? ''}
            onChange={(e) => {
              const v = e.target.value;
              setForm((f) => ({ ...f, minCgpa: v === '' ? null : Number(v) }));
            }}
            size="small"
            inputProps={{ min: 0, max: 10, step: 0.1 }}
            InputProps={{ endAdornment: <InputAdornment position="end">/ 10</InputAdornment> }}
          />

          <Box>
            <Typography variant="body2" sx={{ mb: 1, color: 'text.secondary' }}>
              Eligible Departments
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, mb: 1, flexWrap: 'wrap' }}>
              {form.eligibleDepartments.map((d) => (
                <Chip
                  key={d}
                  label={d}
                  size="small"
                  onDelete={() =>
                    setForm((f) => ({ ...f, eligibleDepartments: f.eligibleDepartments.filter((x) => x !== d) }))
                  }
                />
              ))}
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <TextField
                value={deptInput}
                onChange={(e) => setDeptInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addDept(); } }}
                size="small"
                placeholder="e.g. Computer Science"
                sx={{ flex: 1 }}
              />
              <Button variant="outlined" size="small" onClick={addDept} disabled={!deptInput.trim()}>
                Add
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            Required Skills
          </Typography>

          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {form.requiredSkills.map((s) => (
              <Chip
                key={s}
                label={s}
                size="small"
                onDelete={() =>
                  setForm((f) => ({ ...f, requiredSkills: f.requiredSkills.filter((x) => x !== s) }))
                }
              />
            ))}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              value={skillInput}
              onChange={(e) => setSkillInput(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addSkill(); } }}
              size="small"
              placeholder="e.g. Java"
              sx={{ flex: 1 }}
            />
            <Button variant="outlined" size="small" onClick={addSkill} disabled={!skillInput.trim()}>
              Add
            </Button>
          </Box>
        </CardContent>
      </Card>

      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
        <Button
          startIcon={<ArrowBackRounded />}
          onClick={() => navigate('/recruiter/jobs')}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          variant="contained"
          startIcon={<AddRounded />}
          disabled={isPending}
        >
          {isPending ? 'Submitting…' : submitLabel}
        </Button>
      </Box>
    </Box>
  );
}

export default function CreateJobPage() {
  const navigate = useNavigate();
  const [error, setError] = useState('');

  const createMutation = useMutation({
    mutationFn: (data: JobFormData) => jobApi.create(data),
    onSuccess: (job) => {
      navigate(`/recruiter/jobs/${job.id}/applications`);
    },
    onError: (err: { response?: { data?: { message?: string } } }) => {
      setError(err?.response?.data?.message ?? 'Failed to create job posting.');
    },
  });

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Post a New Job
      </Typography>
      <JobForm
        onSubmit={(data) => createMutation.mutate(data)}
        isPending={createMutation.isPending}
        error={error}
        submitLabel="Post Job"
      />
    </Box>
  );
}
