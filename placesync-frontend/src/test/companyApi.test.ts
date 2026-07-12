vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { companyApi } from '../api/companyApi'

describe('companyApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('list calls paginated endpoint with defaults', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(companyApi.list()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/companies?page=0&size=100')
  })

  it('list accepts custom page and size', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { content: [] } })
    await companyApi.list(2, 10)
    expect(axiosClient.get).toHaveBeenCalledWith('/companies?page=2&size=10')
  })
})
