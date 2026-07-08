import { useEffect, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, TextField, Button,
  Avatar, CircularProgress, Alert, Chip, Divider,
  MenuItem, Select, FormControl, InputLabel, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions, Switch,
  FormControlLabel, Tooltip,
} from '@mui/material';
import {
  EditRounded, DeleteRounded, AddRounded, CameraAltRounded,
  SaveRounded,
} from '@mui/icons-material';
import { studentApi } from '../../api/studentApi';
import type {
  UpdateProfileRequest, EducationRequest, ExperienceRequest, GenderType,
} from '../../types/student';

const GENDERS: { value: GenderType; label: string }[] = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
  { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
];

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

/* ── Education dialog ─────────────────────────────────────────── */
const EMPTY_EDU: EducationRequest = {
  degree: '', institution: '', fieldOfStudy: '', startYear: new Date().getFullYear(), endYear: undefined, percentageOrCgpa: undefined,
};

function EducationDialog({
  open, initial, onClose, onSave, saving,
}: {
  open: boolean;
  initial: EducationRequest;
  onClose: () => void;
  onSave: (data: EducationRequest) => void;
  saving: boolean;
}) {
  const [form, setForm] = useState(initial);
  useEffect(() => { setForm(initial); }, [initial]);
  const set = (k: keyof EducationRequest, v: unknown) => setForm((p) => ({ ...p, [k]: v }));

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initial.degree ? 'Edit Education' : 'Add Education'}</DialogTitle>
      <DialogContent sx={{ pt: '20px !important', display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField label="Degree" value={form.degree} onChange={(e) => set('degree', e.target.value)} size="small" fullWidth required />
        <TextField label="Institution" value={form.institution} onChange={(e) => set('institution', e.target.value)} size="small" fullWidth required />
        <TextField label="Field of Study" value={form.fieldOfStudy ?? ''} onChange={(e) => set('fieldOfStudy', e.target.value)} size="small" fullWidth />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField label="Start Year" type="number" value={form.startYear} onChange={(e) => set('startYear', Number(e.target.value))} size="small" fullWidth />
          <TextField label="End Year" type="number" value={form.endYear ?? ''} onChange={(e) => set('endYear', e.target.value ? Number(e.target.value) : undefined)} size="small" fullWidth />
        </Box>
        <TextField label="Percentage / CGPA" type="number" value={form.percentageOrCgpa ?? ''} onChange={(e) => set('percentageOrCgpa', e.target.value ? Number(e.target.value) : undefined)} size="small" fullWidth slotProps={{ htmlInput: { step: '0.01' } }} />
      </DialogContent>
      <Divider />
      <DialogActions sx={{ px: 3, py: 1.5 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!form.degree || !form.institution || saving} onClick={() => onSave(form)}>
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* ── Experience dialog ────────────────────────────────────────── */
const EMPTY_EXP: ExperienceRequest = {
  companyName: '', role: '', description: '', startDate: '', endDate: undefined, isCurrent: false,
};

function ExperienceDialog({
  open, initial, onClose, onSave, saving,
}: {
  open: boolean;
  initial: ExperienceRequest;
  onClose: () => void;
  onSave: (data: ExperienceRequest) => void;
  saving: boolean;
}) {
  const [form, setForm] = useState(initial);
  useEffect(() => { setForm(initial); }, [initial]);
  const set = (k: keyof ExperienceRequest, v: unknown) => setForm((p) => ({ ...p, [k]: v }));

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{initial.companyName ? 'Edit Experience' : 'Add Experience'}</DialogTitle>
      <DialogContent sx={{ pt: '20px !important', display: 'flex', flexDirection: 'column', gap: 2 }}>
        <TextField label="Company" value={form.companyName} onChange={(e) => set('companyName', e.target.value)} size="small" fullWidth required />
        <TextField label="Role" value={form.role} onChange={(e) => set('role', e.target.value)} size="small" fullWidth required />
        <TextField label="Description" value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} size="small" fullWidth multiline rows={3} />
        <Box sx={{ display: 'flex', gap: 2 }}>
          <TextField label="Start Date" type="date" value={form.startDate} onChange={(e) => set('startDate', e.target.value)} size="small" fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          {!form.isCurrent && (
            <TextField label="End Date" type="date" value={form.endDate ?? ''} onChange={(e) => set('endDate', e.target.value || undefined)} size="small" fullWidth slotProps={{ inputLabel: { shrink: true } }} />
          )}
        </Box>
        <FormControlLabel
          control={<Switch checked={form.isCurrent} onChange={(e) => set('isCurrent', e.target.checked)} />}
          label="Currently working here"
        />
      </DialogContent>
      <Divider />
      <DialogActions sx={{ px: 3, py: 1.5 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" disabled={!form.companyName || !form.role || !form.startDate || saving} onClick={() => onSave(form)}>
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

/* ── Main Page ────────────────────────────────────────────────── */
export default function StudentProfilePage() {
  const queryClient = useQueryClient();
  const pictureInputRef = useRef<HTMLInputElement>(null);

  const [profileSaved, setProfileSaved] = useState(false);
  const [pictureError, setPictureError] = useState('');
  const [skillInput, setSkillInput] = useState('');
  const [eduDialog, setEduDialog] = useState<{ open: boolean; id?: string; initial: EducationRequest }>({ open: false, initial: EMPTY_EDU });
  const [expDialog, setExpDialog] = useState<{ open: boolean; id?: string; initial: ExperienceRequest }>({ open: false, initial: EMPTY_EXP });

  /* ── Queries ── */
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['student-profile'],
    queryFn: studentApi.getProfile,
  });

  const { data: skills = [] } = useQuery({
    queryKey: ['student-skills'],
    queryFn: studentApi.getSkills,
  });

  const { data: education = [] } = useQuery({
    queryKey: ['student-education'],
    queryFn: studentApi.getEducation,
  });

  const { data: experience = [] } = useQuery({
    queryKey: ['student-experience'],
    queryFn: studentApi.getExperience,
  });

  /* ── Profile form state ── */
  const [form, setForm] = useState<UpdateProfileRequest>({
    firstName: '', lastName: '', institution: '', department: '', graduationYear: new Date().getFullYear(),
  });

  useEffect(() => {
    if (profile) {
      setForm({
        firstName: profile.firstName,
        lastName: profile.lastName,
        phone: profile.phone,
        dateOfBirth: profile.dateOfBirth,
        gender: profile.gender,
        institution: profile.institution,
        department: profile.department,
        graduationYear: profile.graduationYear,
        cgpa: profile.cgpa,
        bio: profile.bio,
        isProfilePublic: profile.profilePublic,
      });
    }
  }, [profile]);

  const set = (k: keyof UpdateProfileRequest, v: unknown) =>
    setForm((p) => ({ ...p, [k]: v }));

  /* ── Mutations ── */
  const updateProfileMutation = useMutation({
    mutationFn: () => studentApi.updateProfile(form),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-profile'] });
      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 3000);
    },
  });

  const pictureMutation = useMutation({
    mutationFn: (file: File) => studentApi.uploadPicture(file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['student-profile'] }),
    onError: () => setPictureError('Photo upload failed. S3 storage must be configured.'),
  });

  const addSkillMutation = useMutation({
    mutationFn: (name: string) => studentApi.addSkill(name),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-skills'] });
      setSkillInput('');
    },
  });

  const removeSkillMutation = useMutation({
    mutationFn: (id: string) => studentApi.removeSkill(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['student-skills'] }),
  });

  const addEduMutation = useMutation({
    mutationFn: (data: EducationRequest) => studentApi.addEducation(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-education'] });
      setEduDialog((p) => ({ ...p, open: false }));
    },
  });

  const updateEduMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: EducationRequest }) =>
      studentApi.updateEducation(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-education'] });
      setEduDialog((p) => ({ ...p, open: false }));
    },
  });

  const deleteEduMutation = useMutation({
    mutationFn: (id: string) => studentApi.deleteEducation(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['student-education'] }),
  });

  const addExpMutation = useMutation({
    mutationFn: (data: ExperienceRequest) => studentApi.addExperience(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-experience'] });
      setExpDialog((p) => ({ ...p, open: false }));
    },
  });

  const updateExpMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: ExperienceRequest }) =>
      studentApi.updateExperience(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['student-experience'] });
      setExpDialog((p) => ({ ...p, open: false }));
    },
  });

  const deleteExpMutation = useMutation({
    mutationFn: (id: string) => studentApi.deleteExperience(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['student-experience'] }),
  });

  if (profileLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress />
      </Box>
    );
  }

  const initials = profile
    ? `${profile.firstName[0]}${profile.lastName[0]}`.toUpperCase()
    : 'U';

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>My Profile</Typography>

      {/* ── Avatar + basic info ── */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mb: 3 }}>
            <Box sx={{ position: 'relative' }}>
              <Avatar
                src={profile?.profilePictureUrl}
                sx={{ width: 80, height: 80, fontSize: '1.5rem' }}
              >
                {initials}
              </Avatar>
              <Tooltip title="Change photo">
                <IconButton
                  size="small"
                  sx={{
                    position: 'absolute', bottom: 0, right: 0,
                    bgcolor: 'primary.main', color: 'white',
                    '&:hover': { bgcolor: 'primary.dark' },
                    width: 26, height: 26,
                  }}
                  onClick={() => pictureInputRef.current?.click()}
                >
                  <CameraAltRounded sx={{ fontSize: '0.875rem' }} />
                </IconButton>
              </Tooltip>
              <input
                ref={pictureInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) pictureMutation.mutate(file);
                }}
              />
            </Box>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {profile?.firstName} {profile?.lastName}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {profile?.department} · {profile?.institution}
              </Typography>
            </Box>
          </Box>

          {profileSaved && (
            <Alert severity="success" sx={{ mb: 2 }}>Profile saved successfully.</Alert>
          )}
          {pictureError && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setPictureError('')}>{pictureError}</Alert>
          )}

          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            <TextField label="First Name" value={form.firstName} onChange={(e) => set('firstName', e.target.value)} size="small" fullWidth />
            <TextField label="Last Name" value={form.lastName} onChange={(e) => set('lastName', e.target.value)} size="small" fullWidth />
            <TextField label="Phone" value={form.phone ?? ''} onChange={(e) => set('phone', e.target.value)} size="small" fullWidth />
            <TextField label="Date of Birth" type="date" value={form.dateOfBirth ?? ''} onChange={(e) => set('dateOfBirth', e.target.value)} size="small" fullWidth slotProps={{ inputLabel: { shrink: true } }} />
            <FormControl size="small" fullWidth>
              <InputLabel>Gender</InputLabel>
              <Select label="Gender" value={form.gender ?? ''} onChange={(e) => set('gender', e.target.value || undefined)}>
                <MenuItem value="">Prefer not to say</MenuItem>
                {GENDERS.map((g) => <MenuItem key={g.value} value={g.value}>{g.label}</MenuItem>)}
              </Select>
            </FormControl>
            <TextField label="CGPA" type="number" value={form.cgpa ?? ''} onChange={(e) => set('cgpa', e.target.value ? Number(e.target.value) : undefined)} size="small" fullWidth slotProps={{ htmlInput: { step: '0.01', min: 0, max: 10 } }} />
            <Box sx={{ gridColumn: '1 / -1' }}>
              <TextField label="Institution" value={form.institution} onChange={(e) => set('institution', e.target.value)} size="small" fullWidth />
            </Box>
            <TextField label="Department" value={form.department} onChange={(e) => set('department', e.target.value)} size="small" fullWidth />
            <TextField label="Graduation Year" type="number" value={form.graduationYear} onChange={(e) => set('graduationYear', Number(e.target.value))} size="small" fullWidth />
            <Box sx={{ gridColumn: '1 / -1' }}>
              <TextField label="Bio" value={form.bio ?? ''} onChange={(e) => set('bio', e.target.value)} size="small" fullWidth multiline rows={3} placeholder="Tell recruiters about yourself…" />
            </Box>
            <Box sx={{ gridColumn: '1 / -1' }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={form.isProfilePublic ?? false}
                    onChange={(e) => set('isProfilePublic', e.target.checked)}
                  />
                }
                label="Make profile visible to recruiters"
              />
            </Box>
          </Box>

          <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
            <Button
              variant="contained"
              startIcon={<SaveRounded />}
              disabled={updateProfileMutation.isPending}
              onClick={() => updateProfileMutation.mutate()}
            >
              {updateProfileMutation.isPending ? 'Saving…' : 'Save Profile'}
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* ── Skills ── */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" sx={{ fontWeight: 600, mb: 2 }}>Skills</Typography>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
            {skills.map((s) => (
              <Chip
                key={s.id}
                label={s.skillName}
                onDelete={() => removeSkillMutation.mutate(s.id)}
                size="small"
              />
            ))}
            {skills.length === 0 && (
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>No skills added yet.</Typography>
            )}
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              size="small"
              placeholder="Add a skill…"
              value={skillInput}
              onChange={(e) => setSkillInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && skillInput.trim()) {
                  addSkillMutation.mutate(skillInput.trim());
                }
              }}
              sx={{ width: 220 }}
            />
            <Button
              variant="outlined"
              size="small"
              disabled={!skillInput.trim() || addSkillMutation.isPending}
              onClick={() => addSkillMutation.mutate(skillInput.trim())}
            >
              Add
            </Button>
          </Box>
        </CardContent>
      </Card>

      {/* ── Education ── */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>Education</Typography>
            <Button
              size="small"
              startIcon={<AddRounded />}
              onClick={() => setEduDialog({ open: true, id: undefined, initial: EMPTY_EDU })}
            >
              Add
            </Button>
          </Box>

          {education.length === 0 ? (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>No education added yet.</Typography>
          ) : (
            education.map((edu, i) => (
              <Box key={edu.id}>
                {i > 0 && <Divider sx={{ my: 1.5 }} />}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                      {edu.degree}{edu.fieldOfStudy ? ` in ${edu.fieldOfStudy}` : ''}
                    </Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {edu.institution} · {edu.startYear}–{edu.endYear ?? 'Present'}
                    </Typography>
                    {edu.percentageOrCgpa && (
                      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                        {edu.percentageOrCgpa}
                      </Typography>
                    )}
                  </Box>
                  <Box>
                    <IconButton size="small" onClick={() => setEduDialog({
                      open: true, id: edu.id,
                      initial: {
                        degree: edu.degree, institution: edu.institution,
                        fieldOfStudy: edu.fieldOfStudy, startYear: edu.startYear,
                        endYear: edu.endYear, percentageOrCgpa: edu.percentageOrCgpa,
                      },
                    })}>
                      <EditRounded fontSize="small" />
                    </IconButton>
                    <IconButton size="small" sx={{ color: 'error.main' }} onClick={() => deleteEduMutation.mutate(edu.id)}>
                      <DeleteRounded fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
              </Box>
            ))
          )}
        </CardContent>
      </Card>

      {/* ── Experience ── */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>Experience</Typography>
            <Button
              size="small"
              startIcon={<AddRounded />}
              onClick={() => setExpDialog({ open: true, id: undefined, initial: EMPTY_EXP })}
            >
              Add
            </Button>
          </Box>

          {experience.length === 0 ? (
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>No experience added yet.</Typography>
          ) : (
            experience.map((exp, i) => (
              <Box key={exp.id}>
                {i > 0 && <Divider sx={{ my: 1.5 }} />}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <Box>
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>{exp.role}</Typography>
                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                      {exp.companyName} · {fmt(exp.startDate)}–{exp.isCurrent ? 'Present' : exp.endDate ? fmt(exp.endDate) : ''}
                    </Typography>
                    {exp.description && (
                      <Typography variant="body2" sx={{ mt: 0.5 }}>{exp.description}</Typography>
                    )}
                  </Box>
                  <Box>
                    <IconButton size="small" onClick={() => setExpDialog({
                      open: true, id: exp.id,
                      initial: {
                        companyName: exp.companyName, role: exp.role,
                        description: exp.description, startDate: exp.startDate,
                        endDate: exp.endDate, isCurrent: exp.isCurrent,
                      },
                    })}>
                      <EditRounded fontSize="small" />
                    </IconButton>
                    <IconButton size="small" sx={{ color: 'error.main' }} onClick={() => deleteExpMutation.mutate(exp.id)}>
                      <DeleteRounded fontSize="small" />
                    </IconButton>
                  </Box>
                </Box>
              </Box>
            ))
          )}
        </CardContent>
      </Card>

      {/* Dialogs */}
      <EducationDialog
        open={eduDialog.open}
        initial={eduDialog.initial}
        onClose={() => setEduDialog((p) => ({ ...p, open: false }))}
        saving={addEduMutation.isPending || updateEduMutation.isPending}
        onSave={(data) => {
          if (eduDialog.id) {
            updateEduMutation.mutate({ id: eduDialog.id, data });
          } else {
            addEduMutation.mutate(data);
          }
        }}
      />

      <ExperienceDialog
        open={expDialog.open}
        initial={expDialog.initial}
        onClose={() => setExpDialog((p) => ({ ...p, open: false }))}
        saving={addExpMutation.isPending || updateExpMutation.isPending}
        onSave={(data) => {
          if (expDialog.id) {
            updateExpMutation.mutate({ id: expDialog.id, data });
          } else {
            addExpMutation.mutate(data);
          }
        }}
      />
    </Box>
  );
}
