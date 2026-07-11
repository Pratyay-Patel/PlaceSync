import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { ErrorOutlineRounded } from '@mui/icons-material';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  message: string;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, message: '' };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  handleReset = () => {
    this.setState({ hasError: false, message: '' });
    window.location.href = '/';
  };

  render() {
    if (this.state.hasError) {
      return (
        <Box
          sx={{
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 2,
            p: 4,
            textAlign: 'center',
          }}
        >
          <ErrorOutlineRounded sx={{ fontSize: 56, color: 'error.main' }} />
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            Something went wrong
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 480 }}>
            An unexpected error occurred. The error has been logged. Please return to the
            home page and try again.
          </Typography>
          {this.state.message && (
            <Typography
              variant="caption"
              sx={{
                fontFamily: 'monospace',
                bgcolor: 'action.hover',
                px: 2,
                py: 1,
                borderRadius: 1,
                maxWidth: 560,
                wordBreak: 'break-all',
              }}
            >
              {this.state.message}
            </Typography>
          )}
          <Button variant="contained" onClick={this.handleReset} sx={{ mt: 1 }}>
            Go to Home
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}
