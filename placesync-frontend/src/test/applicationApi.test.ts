vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), post: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { applicationApi } from '../api/applicationApi'

describe('applicationApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getMyApplications uses default page and size', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(applicationApi.getMyApplications()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/applications?page=0&size=20')
  })

  it('getMyApplications accepts custom page and size', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { content: [] } })
    await applicationApi.getMyApplications(2, 5)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/applications?page=2&size=5')
  })

  it('apply sends jobId and resumeId', async () => {
    const application = { id: 'a1', jobId: 'j1', resumeId: 'r1' }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: application })
    await expect(applicationApi.apply('j1', 'r1')).resolves.toEqual(application)
    expect(axiosClient.post).toHaveBeenCalledWith('/applications', { jobId: 'j1', resumeId: 'r1' })
  })
})
