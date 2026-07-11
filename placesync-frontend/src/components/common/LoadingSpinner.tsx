import { Box, CircularProgress } from '@mui/material';

interface LoadingSpinnerProps {
  py?: number;
  size?: number;
}

export default function LoadingSpinner({ py = 6, size }: LoadingSpinnerProps) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py }}>
      <CircularProgress size={size} />
    </Box>
  );
}
