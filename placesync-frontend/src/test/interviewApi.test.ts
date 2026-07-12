vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { interviewApi } from '../api/interviewApi'

describe('interviewApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getMyInterviews calls paginated endpoint with defaults', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(interviewApi.getMyInterviews()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/interviews?page=0&size=50')
  })

  it('getMyInterviews accepts custom page and size', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { content: [] } })
    await interviewApi.getMyInterviews(1, 20)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/interviews?page=1&size=20')
  })
})
