import type { ReactNode } from 'react';
import {
  Button, Dialog, DialogActions, DialogContent,
  DialogContentText, DialogTitle, TextField,
} from '@mui/material';

interface Props {
  open: boolean;
  title: string;
  description: ReactNode;
  reason: string;
  onReasonChange: (r: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  isPending: boolean;
}

export default function RejectDialog({
  open, title, description, reason, onReasonChange, onConfirm, onCancel, isPending,
}: Props) {
  return (
    <Dialog open={open} onClose={onCancel} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>{description}</DialogContentText>
        <TextField
          label="Rejection Reason"
          value={reason}
          onChange={(e) => onReasonChange(e.target.value)}
          fullWidth
          multiline
          rows={3}
          size="small"
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel}>Cancel</Button>
        <Button
          variant="contained"
          color="error"
          disabled={!reason.trim() || isPending}
          onClick={onConfirm}
        >
          {isPending ? 'Rejecting…' : 'Reject'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
