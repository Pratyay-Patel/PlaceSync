import { Box, LinearProgress, Typography } from '@mui/material';
import type { StudentProfile, Skill, Education, Experience } from '../../types/student';

interface ProfileCompletenessBarProps {
  profile: StudentProfile;
  skills: Skill[];
  education: Education[];
  experience: Experience[];
}

const CHECKS: { label: string; check: (p: ProfileCompletenessBarProps) => boolean }[] = [
  { label: 'First & last name',    check: ({ profile }) => !!(profile.firstName && profile.lastName) },
  { label: 'Phone number',         check: ({ profile }) => !!profile.phone },
  { label: 'Date of birth',        check: ({ profile }) => !!profile.dateOfBirth },
  { label: 'Bio',                  check: ({ profile }) => !!profile.bio },
  { label: 'CGPA',                 check: ({ profile }) => profile.cgpa != null },
  { label: 'Profile picture',      check: ({ profile }) => !!profile.profilePictureUrl },
  { label: 'At least one skill',   check: ({ skills }) => skills.length > 0 },
  { label: 'Education record',     check: ({ education }) => education.length > 0 },
  { label: 'Experience record',    check: ({ experience }) => experience.length > 0 },
];

export default function ProfileCompletenessBar(props: ProfileCompletenessBarProps) {
  const completed = CHECKS.filter((c) => c.check(props)).length;
  const total = CHECKS.length;
  const pct = Math.round((completed / total) * 100);

  const color =
    pct >= 80 ? 'success' :
    pct >= 50 ? 'warning' :
    'error';

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          Profile Completeness
        </Typography>
        <Typography variant="body2" sx={{ fontWeight: 700, color: `${color}.main` }}>
          {pct}%
        </Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={color}
        sx={{ height: 6, borderRadius: 3 }}
      />
      {pct < 100 && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
          Missing:{' '}
          {CHECKS.filter((c) => !c.check(props))
            .map((c) => c.label)
            .join(', ')}
        </Typography>
      )}
    </Box>
  );
}
