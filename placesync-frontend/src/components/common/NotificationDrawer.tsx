import {
  Box, Drawer, Typography, IconButton, Button, Divider, CircularProgress,
  List, ListItem, ListItemText,
} from '@mui/material';
import { CloseRounded, DoneAllRounded } from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../api/notificationApi';

interface NotificationDrawerProps {
  open: boolean;
  onClose: () => void;
}

function timeAgo(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

export default function NotificationDrawer({ open, onClose }: NotificationDrawerProps) {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['notifications-drawer'],
    queryFn: () => notificationApi.getNotifications(0, 30),
    enabled: open,
  });

  const markReadMutation = useMutation({
    mutationFn: (id: string) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-drawer'] });
      queryClient.invalidateQueries({ queryKey: ['notification-count'] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications-drawer'] });
      queryClient.invalidateQueries({ queryKey: ['notification-count'] });
    },
  });

  const notifications = data?.content ?? [];

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      sx={{ '& .MuiDrawer-paper': { width: 360 } }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', px: 2, py: 1.5 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, flexGrow: 1 }}>
          Notifications
        </Typography>
        <Button
          size="small"
          startIcon={<DoneAllRounded fontSize="small" />}
          disabled={markAllMutation.isPending || notifications.every((n) => n.isRead)}
          onClick={() => markAllMutation.mutate()}
          sx={{ mr: 1 }}
        >
          Mark all read
        </Button>
        <IconButton size="small" onClick={onClose}>
          <CloseRounded fontSize="small" />
        </IconButton>
      </Box>

      <Divider />

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress size={28} />
        </Box>
      ) : notifications.length === 0 ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <Typography variant="body2" color="text.secondary">
            No notifications yet.
          </Typography>
        </Box>
      ) : (
        <List disablePadding>
          {notifications.map((n) => (
            <Box key={n.id}>
              <ListItem
                alignItems="flex-start"
                onClick={() => { if (!n.isRead) markReadMutation.mutate(n.id); }}
                sx={{
                  cursor: n.isRead ? 'default' : 'pointer',
                  bgcolor: n.isRead ? 'transparent' : 'action.hover',
                  '&:hover': { bgcolor: 'action.hover' },
                  px: 2,
                  py: 1.5,
                }}
              >
                <ListItemText
                  primary={
                    <Typography
                      variant="body2"
                      sx={{ fontWeight: n.isRead ? 400 : 600, lineHeight: 1.4 }}
                    >
                      {n.title}
                    </Typography>
                  }
                  secondary={
                    <>
                      <Typography component="span" variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
                        {n.body}
                      </Typography>
                      <Typography component="span" variant="caption" color="text.disabled" sx={{ mt: 0.5, display: 'block' }}>
                        {timeAgo(n.createdAt)}
                      </Typography>
                    </>
                  }
                />
              </ListItem>
              <Divider component="li" />
            </Box>
          ))}
        </List>
      )}
    </Drawer>
  );
}
