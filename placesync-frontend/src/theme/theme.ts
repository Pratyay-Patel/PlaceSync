import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#4F46E5',
      dark: '#4338CA',
      light: '#6366F1',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#64748B',
      contrastText: '#FFFFFF',
    },
    success: {
      main: '#10B981',
      light: '#D1FAE5',
      dark: '#059669',
      contrastText: '#FFFFFF',
    },
    warning: {
      main: '#F59E0B',
      light: '#FEF3C7',
      dark: '#D97706',
      contrastText: '#FFFFFF',
    },
    error: {
      main: '#EF4444',
      light: '#FEE2E2',
      dark: '#DC2626',
      contrastText: '#FFFFFF',
    },
    info: {
      main: '#3B82F6',
      light: '#DBEAFE',
      dark: '#2563EB',
      contrastText: '#FFFFFF',
    },
    background: {
      default: '#F8FAFC',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#0F172A',
      secondary: '#64748B',
      disabled: '#94A3B8',
    },
    divider: '#E2E8F0',
  },

  typography: {
    fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    h1: { fontWeight: 700, fontSize: '2.25rem', lineHeight: 1.2, letterSpacing: '-0.02em' },
    h2: { fontWeight: 700, fontSize: '1.875rem', lineHeight: 1.25, letterSpacing: '-0.015em' },
    h3: { fontWeight: 600, fontSize: '1.5rem', lineHeight: 1.3, letterSpacing: '-0.01em' },
    h4: { fontWeight: 600, fontSize: '1.25rem', lineHeight: 1.35 },
    h5: { fontWeight: 600, fontSize: '1.125rem', lineHeight: 1.4 },
    h6: { fontWeight: 600, fontSize: '1rem', lineHeight: 1.5 },
    subtitle1: { fontWeight: 500, fontSize: '0.9375rem', lineHeight: 1.5 },
    subtitle2: { fontWeight: 500, fontSize: '0.875rem', lineHeight: 1.5 },
    body1: { fontSize: '0.9375rem', lineHeight: 1.6 },
    body2: { fontSize: '0.875rem', lineHeight: 1.57 },
    caption: { fontSize: '0.75rem', fontWeight: 500, letterSpacing: '0.02em' },
    overline: { fontSize: '0.6875rem', fontWeight: 600, letterSpacing: '0.08em', textTransform: 'uppercase' },
    button: { fontWeight: 600, textTransform: 'none', fontSize: '0.875rem' },
  },

  shape: { borderRadius: 8 },

  components: {
    MuiCssBaseline: {
      styleOverrides: {
        '*, *::before, *::after': { boxSizing: 'border-box' },
        body: {
          scrollbarWidth: 'thin',
          scrollbarColor: '#CBD5E1 transparent',
          '&::-webkit-scrollbar': { width: '6px', height: '6px' },
          '&::-webkit-scrollbar-track': { background: 'transparent' },
          '&::-webkit-scrollbar-thumb': {
            background: '#CBD5E1',
            borderRadius: '3px',
            '&:hover': { background: '#94A3B8' },
          },
        },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
      },
    },

    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: '1px solid #E2E8F0',
          borderRadius: '12px',
          transition: 'box-shadow 200ms ease, transform 200ms ease',
          '&:hover': {
            boxShadow: '0 4px 20px rgba(79, 70, 229, 0.1)',
          },
        },
      },
    },

    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: '20px',
          '&:last-child': { paddingBottom: '20px' },
        },
      },
    },

    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          borderRadius: '8px',
          fontWeight: 600,
          textTransform: 'none',
          padding: '8px 18px',
        },
        sizeSmall: { padding: '4px 12px', fontSize: '0.8125rem' },
        sizeLarge: { padding: '12px 28px' },
      },
      variants: [
        {
          props: { variant: 'contained', color: 'primary' },
          style: {
            background: 'linear-gradient(135deg, #4F46E5 0%, #4338CA 100%)',
            '&:hover': {
              background: 'linear-gradient(135deg, #4338CA 0%, #3730A3 100%)',
              boxShadow: '0 4px 14px rgba(79, 70, 229, 0.35)',
            },
          },
        },
        {
          props: { variant: 'outlined', color: 'primary' },
          style: {
            borderColor: '#4F46E5',
            '&:hover': { backgroundColor: '#EEF2FF', borderColor: '#4338CA' },
          },
        },
      ],
    },

    MuiIconButton: {
      styleOverrides: {
        root: { borderRadius: '8px' },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: { borderRadius: '6px', fontWeight: 500, fontSize: '0.75rem' },
      },
    },

    MuiTextField: {
      defaultProps: { size: 'small' },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          '&:hover:not(.Mui-disabled):not(.Mui-focused) .MuiOutlinedInput-notchedOutline': {
            borderColor: '#4F46E5',
          },
        },
      },
    },

    MuiAppBar: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          backgroundColor: '#FFFFFF',
          borderBottom: '1px solid #E2E8F0',
          color: '#0F172A',
        },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: {
          border: 'none',
          borderRight: '1px solid #E2E8F0',
          backgroundColor: '#FFFFFF',
        },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: '8px',
          padding: '8px 12px',
          '&.Mui-selected': {
            backgroundColor: '#EEF2FF',
            color: '#4F46E5',
            '& .MuiListItemIcon-root': { color: '#4F46E5' },
            '&:hover': { backgroundColor: '#E0E7FF' },
          },
          '&:hover': { backgroundColor: '#F8FAFC' },
        },
      },
    },

    MuiListItemIcon: {
      styleOverrides: {
        root: { minWidth: '36px', color: '#64748B' },
      },
    },

    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-root': {
            backgroundColor: '#F8FAFC',
            fontWeight: 600,
            fontSize: '0.6875rem',
            textTransform: 'uppercase',
            letterSpacing: '0.07em',
            color: '#64748B',
            borderBottom: '1px solid #E2E8F0',
          },
        },
      },
    },

    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid #F1F5F9',
          padding: '12px 16px',
          fontSize: '0.875rem',
        },
      },
    },

    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': { backgroundColor: '#FAFBFD' },
          '&:last-child .MuiTableCell-root': { borderBottom: 'none' },
        },
      },
    },

    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: '#0F172A',
          borderRadius: '6px',
          fontSize: '0.75rem',
          padding: '6px 10px',
        },
        arrow: { color: '#0F172A' },
      },
    },

    MuiMenu: {
      styleOverrides: {
        paper: {
          border: '1px solid #E2E8F0',
          borderRadius: '10px',
          boxShadow: '0 10px 25px rgba(0,0,0,0.08)',
        },
        list: { padding: '6px' },
      },
    },

    MuiMenuItem: {
      styleOverrides: {
        root: {
          borderRadius: '6px',
          fontSize: '0.875rem',
          padding: '8px 12px',
          gap: '10px',
        },
      },
    },

    MuiDivider: {
      styleOverrides: {
        root: { borderColor: '#F1F5F9' },
      },
    },

    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: '4px', height: '6px' },
      },
    },

    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: '10px' },
      },
    },

    MuiSkeleton: {
      defaultProps: { animation: 'wave' },
      styleOverrides: {
        root: { borderRadius: '6px' },
      },
    },

    MuiBadge: {
      styleOverrides: {
        badge: { fontWeight: 600, fontSize: '0.625rem', minWidth: '18px', height: '18px' },
      },
    },

    MuiAvatar: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.875rem',
          background: 'linear-gradient(135deg, #4F46E5, #6366F1)',
        },
      },
    },
  },
});
