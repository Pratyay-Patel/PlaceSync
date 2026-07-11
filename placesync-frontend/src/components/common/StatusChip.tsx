import { Chip } from '@mui/material';

type KnownStatus =
  | 'APPLIED' | 'UNDER_REVIEW' | 'SHORTLISTED' | 'INTERVIEW_SCHEDULED' | 'OFFERED' | 'REJECTED'
  | 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED'
  | 'PENDING_VERIFICATION' | 'VERIFIED'
  | 'PENDING_APPROVAL' | 'OPEN' | 'CLOSED' | 'EXPIRED';

const STATUS_CONFIG: Record<KnownStatus, { label: string; color: string }> = {
  // Application
  APPLIED:              { label: 'Applied',              color: '#3B82F6' },
  UNDER_REVIEW:         { label: 'Under Review',         color: '#F59E0B' },
  SHORTLISTED:          { label: 'Shortlisted',          color: '#8B5CF6' },
  INTERVIEW_SCHEDULED:  { label: 'Interview Scheduled',  color: '#06B6D4' },
  OFFERED:              { label: 'Offered',              color: '#10B981' },
  REJECTED:             { label: 'Rejected',             color: '#EF4444' },
  // Interview
  SCHEDULED:            { label: 'Scheduled',            color: '#06B6D4' },
  COMPLETED:            { label: 'Completed',            color: '#10B981' },
  CANCELLED:            { label: 'Cancelled',            color: '#EF4444' },
  RESCHEDULED:          { label: 'Rescheduled',          color: '#F59E0B' },
  // Verification
  PENDING_VERIFICATION: { label: 'Pending',              color: '#F59E0B' },
  VERIFIED:             { label: 'Verified',             color: '#10B981' },
  // Job
  PENDING_APPROVAL:     { label: 'Pending Approval',     color: '#F59E0B' },
  OPEN:                 { label: 'Open',                 color: '#10B981' },
  CLOSED:               { label: 'Closed',               color: '#6B7280' },
  EXPIRED:              { label: 'Expired',              color: '#EF4444' },
};

interface StatusChipProps {
  status: string;
  size?: 'small' | 'medium';
}

export default function StatusChip({ status, size = 'small' }: StatusChipProps) {
  const config = STATUS_CONFIG[status as KnownStatus];
  const color = config?.color ?? '#6B7280';
  const label = config?.label ?? status.replace(/_/g, ' ');

  return (
    <Chip
      label={label}
      size={size}
      sx={{
        bgcolor: color + '1A',
        color,
        fontWeight: 600,
        fontSize: size === 'small' ? '0.75rem' : '0.875rem',
        border: `1px solid ${color}33`,
      }}
    />
  );
}
