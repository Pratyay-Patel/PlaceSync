vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), patch: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { notificationApi } from '../api/notificationApi'

describe('notificationApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getUnreadCount extracts count from ApiResponse', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data: 7 } })
    await expect(notificationApi.getUnreadCount()).resolves.toBe(7)
    expect(axiosClient.get).toHaveBeenCalledWith('/notifications/unread-count')
  })

  it('getNotifications uses default params and extracts paged data', async () => {
    const data = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data } })
    await expect(notificationApi.getNotifications()).resolves.toEqual(data)
    expect(axiosClient.get).toHaveBeenCalledWith('/notifications', {
      params: { page: 0, size: 20, unreadOnly: false },
    })
  })

  it('getNotifications passes unreadOnly flag', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data: { content: [] } } })
    await notificationApi.getNotifications(1, 10, true)
    expect(axiosClient.get).toHaveBeenCalledWith('/notifications', {
      params: { page: 1, size: 10, unreadOnly: true },
    })
  })

  it('markAsRead patches correct endpoint', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await notificationApi.markAsRead('n1')
    expect(axiosClient.patch).toHaveBeenCalledWith('/notifications/n1/read')
  })

  it('markAllAsRead patches bulk endpoint', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await notificationApi.markAllAsRead()
    expect(axiosClient.patch).toHaveBeenCalledWith('/notifications/read-all')
  })
})
