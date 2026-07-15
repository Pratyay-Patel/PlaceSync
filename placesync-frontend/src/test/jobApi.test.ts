vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { jobApi } from '../api/jobApi'
import type { JobFormData } from '../api/jobApi'

const FORM_DATA: JobFormData = {
  title: 'SWE',
  description: 'desc',
  locationType: 'REMOTE',
  jobType: 'FULL_TIME',
  applicationDeadline: '2025-12-31',
  requiredSkills: ['Java'],
  eligibleDepartments: ['CS'],
}

describe('jobApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('list builds URL params and returns paged data', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(jobApi.list({ keyword: 'java', locationType: 'REMOTE' })).resolves.toEqual(page)
    const url = vi.mocked(axiosClient.get).mock.calls[0][0] as string
    expect(url).toContain('keyword=java')
    expect(url).toContain('locationType=REMOTE')
  })

  it('list uses default page and size', async () => {
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { content: [] } })
    await jobApi.list()
    const url = vi.mocked(axiosClient.get).mock.calls[0][0] as string
    expect(url).toContain('page=0')
    expect(url).toContain('size=12')
  })

  it('getById calls correct endpoint', async () => {
    const job = { id: 'j1', title: 'SWE' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: job })
    await expect(jobApi.getById('j1')).resolves.toEqual(job)
    expect(axiosClient.get).toHaveBeenCalledWith('/jobs/j1')
  })

  it('create posts job data', async () => {
    const job = { id: 'j1', ...FORM_DATA }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: job })
    await expect(jobApi.create(FORM_DATA)).resolves.toEqual(job)
    expect(axiosClient.post).toHaveBeenCalledWith('/jobs', FORM_DATA)
  })

  it('update puts job data', async () => {
    const job = { id: 'j1', ...FORM_DATA }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: job })
    await expect(jobApi.update('j1', FORM_DATA)).resolves.toEqual(job)
    expect(axiosClient.put).toHaveBeenCalledWith('/jobs/j1', FORM_DATA)
  })

  it('close patches correct endpoint', async () => {
    const job = { id: 'j1' }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: job })
    await expect(jobApi.close('j1')).resolves.toEqual(job)
    expect(axiosClient.patch).toHaveBeenCalledWith('/jobs/j1/close', {})
  })
})
