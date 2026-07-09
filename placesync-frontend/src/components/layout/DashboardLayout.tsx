import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Avatar,
  Badge,
  Tooltip,
  Menu,
  MenuItem,
  Divider,
} from '@mui/material';
import {
  DashboardRounded,
  PersonRounded,
  DescriptionRounded,
  WorkRounded,
  AssignmentRounded,
  EventNoteRounded,
  GroupRounded,
  BusinessRounded,
  BarChartRounded,
  HistoryRounded,
  AddRounded,
  ChevronLeftRounded,
  MenuRounded,
  NotificationsRounded,
  LogoutRounded,
  HowToRegRounded,
  PendingActionsRounded,
} from '@mui/icons-material';
import { AnimatePresence, motion } from 'framer-motion';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../../store/authStore';
import { queryClient } from '../../lib/queryClient';
import { notificationApi } from '../../api/notificationApi';
import type { UserRole } from '../../types/auth';

const DRAWER_WIDTH = 240;
const COLLAPSED_WIDTH = 68;

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
}

const NAV_ITEMS: Record<UserRole, NavItem[]> = {
  ROLE_STUDENT: [
    { label: 'Dashboard', path: '/student/dashboard', icon: <DashboardRounded fontSize="small" /> },
    { label: 'My Profile', path: '/student/profile', icon: <PersonRounded fontSize="small" /> },
    { label: 'Resumes', path: '/student/resumes', icon: <DescriptionRounded fontSize="small" /> },
    { label: 'Browse Jobs', path: '/student/jobs', icon: <WorkRounded fontSize="small" /> },
    { label: 'Applications', path: '/student/applications', icon: <AssignmentRounded fontSize="small" /> },
    { label: 'Interviews', path: '/student/interviews', icon: <EventNoteRounded fontSize="small" /> },
  ],
  ROLE_RECRUITER: [
    { label: 'Dashboard', path: '/recruiter/dashboard', icon: <DashboardRounded fontSize="small" /> },
    { label: 'My Profile', path: '/recruiter/profile', icon: <PersonRounded fontSize="small" /> },
    { label: 'My Jobs', path: '/recruiter/jobs', icon: <WorkRounded fontSize="small" /> },
    { label: 'Post a Job', path: '/recruiter/jobs/create', icon: <AddRounded fontSize="small" /> },
  ],
  ROLE_ADMIN: [
    { label: 'Dashboard', path: '/admin/dashboard', icon: <DashboardRounded fontSize="small" /> },
    { label: 'Users', path: '/admin/users', icon: <GroupRounded fontSize="small" /> },
    { label: 'Recruiter Approvals', path: '/admin/recruiters/pending', icon: <HowToRegRounded fontSize="small" /> },
    { label: 'Company Approvals', path: '/admin/companies/pending', icon: <BusinessRounded fontSize="small" /> },
    { label: 'Job Approvals', path: '/admin/jobs/pending', icon: <PendingActionsRounded fontSize="small" /> },
    { label: 'Applications', path: '/admin/applications', icon: <AssignmentRounded fontSize="small" /> },
    { label: 'Interviews', path: '/admin/interviews', icon: <EventNoteRounded fontSize="small" /> },
    { label: 'Analytics', path: '/admin/analytics', icon: <BarChartRounded fontSize="small" /> },
    { label: 'Audit Log', path: '/admin/audit-log', icon: <HistoryRounded fontSize="small" /> },
  ],
};

const ROLE_LABELS: Record<UserRole, string> = {
  ROLE_STUDENT: 'Student',
  ROLE_RECRUITER: 'Recruiter',
  ROLE_ADMIN: 'Admin',
};

const pageVariants = {
  initial: { opacity: 0, y: 8 },
  in: { opacity: 1, y: 0 },
  out: { opacity: 0, y: -8 },
};

const pageTransition = { type: 'tween' as const, ease: 'easeOut' as const, duration: 0.18 };

export default function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [hovered, setHovered] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const isExpanded = !collapsed || hovered;
  const location = useLocation();
  const navigate = useNavigate();
  const { role, email, logout } = useAuthStore();

  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['notification-count'],
    queryFn: notificationApi.getUnreadCount,
    refetchInterval: 60_000,
  });

  const navItems = role ? NAV_ITEMS[role] : [];
  const initials = email ? email[0].toUpperCase() : 'U';
  const roleLabel = role ? ROLE_LABELS[role] : '';

  const handleLogout = () => {
    setAnchorEl(null);
    queryClient.clear();
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* ── Sidebar ── */}
      <Drawer
        variant="permanent"
        onMouseEnter={() => { if (collapsed) setHovered(true); }}
        onMouseLeave={() => setHovered(false)}
        sx={{
          width: isExpanded ? DRAWER_WIDTH : COLLAPSED_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: isExpanded ? DRAWER_WIDTH : COLLAPSED_WIDTH,
            overflowX: 'hidden',
            transition: `width ${hovered ? 160 : 225}ms cubic-bezier(0.4, 0, 0.6, 1)`,
            display: 'flex',
            flexDirection: 'column',
          },
        }}
      >
        {/* Logo row */}
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: isExpanded ? 'space-between' : 'center',
            px: isExpanded ? 2 : 1,
            py: 1.5,
            minHeight: 64,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, overflow: 'hidden' }}>
            <Box
              sx={{
                width: 34,
                height: 34,
                borderRadius: '10px',
                background: 'linear-gradient(135deg, #4F46E5 0%, #4338CA 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                boxShadow: '0 2px 8px rgba(79, 70, 229, 0.35)',
              }}
            >
              <Typography sx={{ color: '#FFF', fontWeight: 700, fontSize: '0.8125rem', lineHeight: 1 }}>
                PS
              </Typography>
            </Box>

            <Typography
              variant="h6"
              sx={{
                fontWeight: 700,
                fontSize: '1rem',
                color: 'text.primary',
                whiteSpace: 'nowrap',
                opacity: isExpanded ? 1 : 0,
                transition: 'opacity 200ms',
              }}
            >
              PlaceSync
            </Typography>
          </Box>

          {!collapsed && (
            <Tooltip title="Collapse sidebar" placement="right">
              <IconButton size="small" onClick={() => setCollapsed(true)} sx={{ color: 'text.secondary' }}>
                <ChevronLeftRounded fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>

        <Divider />

        {/* Nav items */}
        <List sx={{ px: 1, py: 1.5, flexGrow: 1 }}>
          {navItems.map((item) => {
            const isActive =
              location.pathname === item.path ||
              (item.path !== '/' && location.pathname.startsWith(item.path) &&
                item.path.split('/').length >= location.pathname.split('/').length - 1);

            return (
              <Tooltip
                key={item.path}
                title={!isExpanded ? item.label : ''}
                placement="right"
                arrow
              >
                <ListItemButton
                  selected={isActive}
                  onClick={() => navigate(item.path)}
                  sx={{
                    mx: 0,
                    mb: 0.25,
                    minHeight: 42,
                    justifyContent: isExpanded ? 'flex-start' : 'center',
                    px: 1.5,
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: isExpanded ? 34 : 'auto',
                      justifyContent: 'center',
                      color: isActive ? 'primary.main' : 'text.secondary',
                    }}
                  >
                    {item.icon}
                  </ListItemIcon>

                  <ListItemText
                    primary={item.label}
                    sx={{
                      '& .MuiListItemText-primary': { fontSize: '0.875rem', fontWeight: 500, whiteSpace: 'nowrap' },
                      opacity: isExpanded ? 1 : 0,
                      transition: 'opacity 200ms',
                      ml: 0.5,
                    }}
                  />
                </ListItemButton>
              </Tooltip>
            );
          })}
        </List>

        <Divider />

        {/* User info footer */}
        <Box
          sx={{
            px: isExpanded ? 2 : 1,
            py: 1.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            justifyContent: isExpanded ? 'flex-start' : 'center',
          }}
        >
          <Avatar
            sx={{
              width: 34,
              height: 34,
              fontSize: '0.8125rem',
              flexShrink: 0,
              cursor: 'pointer',
            }}
            onClick={(e) => setAnchorEl(e.currentTarget)}
          >
            {initials}
          </Avatar>

          {isExpanded && (
            <Box sx={{ overflow: 'hidden', flexGrow: 1 }}>
              <Typography
                variant="body2"
                noWrap
                sx={{ fontWeight: 600, color: 'text.primary', lineHeight: 1.3 }}
              >
                {email?.split('@')[0]}
              </Typography>
              <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                {roleLabel}
              </Typography>
            </Box>
          )}
        </Box>
      </Drawer>

      {/* ── Main area ── */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
          overflow: 'hidden',
        }}
      >
        {/* Topbar */}
        <AppBar position="sticky" sx={{ zIndex: 10 }}>
          <Toolbar sx={{ gap: 1 }}>
            {collapsed && (
              <Tooltip title="Expand sidebar">
                <IconButton size="small" onClick={() => setCollapsed(false)} sx={{ color: 'text.secondary', mr: 1 }}>
                  <MenuRounded fontSize="small" />
                </IconButton>
              </Tooltip>
            )}

            <Box sx={{ flexGrow: 1 }} />

            {/* Notification bell — count wired in 6.3 */}
            <Tooltip title="Notifications">
              <IconButton size="small" sx={{ color: 'text.secondary' }}>
                <Badge badgeContent={unreadCount} color="error" invisible={unreadCount === 0}>
                  <NotificationsRounded fontSize="small" />
                </Badge>
              </IconButton>
            </Tooltip>

            {/* User avatar + menu */}
            <Tooltip title="Account">
              <IconButton
                size="small"
                onClick={(e) => setAnchorEl(e.currentTarget)}
                sx={{ ml: 0.5 }}
              >
                <Avatar sx={{ width: 32, height: 32, fontSize: '0.8125rem' }}>
                  {initials}
                </Avatar>
              </IconButton>
            </Tooltip>
          </Toolbar>
        </AppBar>

        {/* User menu */}
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={() => setAnchorEl(null)}
          transformOrigin={{ horizontal: 'right', vertical: 'top' }}
          anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
          slotProps={{ paper: { sx: { minWidth: 220, mt: 0.5 } } }}
        >
          <Box sx={{ px: 1.5, py: 1, mb: 0.5 }}>
            <Typography variant="body2" sx={{ fontWeight: 600, color: 'text.primary' }}>
              {email?.split('@')[0]}
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
              {email}
            </Typography>
            <Box
              sx={{
                display: 'inline-block',
                mt: 0.75,
                px: 1,
                py: 0.25,
                bgcolor: '#EEF2FF',
                borderRadius: '4px',
              }}
            >
              <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: '#4F46E5' }}>
                {roleLabel}
              </Typography>
            </Box>
          </Box>

          <Divider sx={{ my: 0.5 }} />

          <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
            <LogoutRounded fontSize="small" />
            Sign out
          </MenuItem>
        </Menu>

        {/* Page content with Framer Motion transition */}
        <Box sx={{ flexGrow: 1, p: 3, overflow: 'auto' }}>
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              variants={pageVariants}
              initial="initial"
              animate="in"
              exit="out"
              transition={pageTransition}
              style={{ minHeight: '100%' }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </Box>
      </Box>
    </Box>
  );
}
