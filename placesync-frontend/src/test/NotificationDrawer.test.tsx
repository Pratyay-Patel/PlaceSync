import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import NotificationDrawer from '../components/common/NotificationDrawer';
import { notificationApi } from '../api/notificationApi';
import { createTestQueryClient } from './testUtils';
import type { PagedResponse } from '../types/common';
import type { Notification } from '../types/notification';

vi.mock('../api/notificationApi');

const makePage = (notifications: Partial<Notification>[]): PagedResponse<Notification> => ({
  content: notifications as Notification[],
  totalPages: 1,
  totalElements: notifications.length,
  page: 0,
  size: 30,
  last: true,
});

const NOTIFICATIONS: Partial<Notification>[] = [
  {
    id: 'n1',
    title: 'Application received',
    body: 'Your application for Frontend Engineer was submitted.',
    isRead: false,
    createdAt: new Date(Date.now() - 5 * 60_000).toISOString(),
  },
  {
    id: 'n2',
    title: 'Profile updated',
    body: 'Your profile was updated successfully.',
    isRead: true,
    createdAt: new Date(Date.now() - 60 * 60_000).toISOString(),
  },
];

function renderDrawer(open: boolean) {
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <NotificationDrawer open={open} onClose={vi.fn()} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.mocked(notificationApi.getNotifications).mockReset();
  vi.mocked(notificationApi.markAsRead).mockReset();
  vi.mocked(notificationApi.getNotifications).mockResolvedValue(makePage(NOTIFICATIONS));
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  vi.mocked(notificationApi.markAsRead).mockResolvedValue({ data: null, status: 200 } as any);
});

describe('NotificationDrawer', () => {
  it('renders notification titles when open', async () => {
    renderDrawer(true);
    await waitFor(() => {
      expect(screen.getByText('Application received')).toBeInTheDocument();
    });
    expect(screen.getByText('Profile updated')).toBeInTheDocument();
  });

  it('calls markAsRead when clicking an unread notification', async () => {
    renderDrawer(true);
    await waitFor(() => {
      expect(screen.getByText('Application received')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Application received'));

    await waitFor(() => {
      expect(vi.mocked(notificationApi.markAsRead)).toHaveBeenCalledWith('n1');
    });
  });

  it('shows empty state when there are no notifications', async () => {
    vi.mocked(notificationApi.getNotifications).mockResolvedValueOnce(makePage([]));
    renderDrawer(true);
    await waitFor(() => {
      expect(screen.getByText('No notifications yet.')).toBeInTheDocument();
    });
  });
});
