vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { resumeApi } from '../api/resumeApi'

describe('resumeApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('list returns resume array', async () => {
    const resumes = [{ id: 'r1', label: 'Main CV' }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: resumes })
    await expect(resumeApi.list()).resolves.toEqual(resumes)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/resumes')
  })

  it('upload posts multipart form with label and isDefault', async () => {
    const resume = { id: 'r1', label: 'CV' }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: resume })
    const file = new File(['pdf'], 'cv.pdf', { type: 'application/pdf' })
    await expect(resumeApi.upload(file, 'CV', true)).resolves.toEqual(resume)
    expect(axiosClient.post).toHaveBeenCalledWith(
      '/students/resumes?label=CV&isDefault=true',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  })

  it('upload uses isDefault=false by default', async () => {
    vi.mocked(axiosClient.post).mockResolvedValue({ data: {} })
    const file = new File(['pdf'], 'cv.pdf')
    await resumeApi.upload(file, 'My CV')
    expect(axiosClient.post).toHaveBeenCalledWith(
      '/students/resumes?label=My%20CV&isDefault=false',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  })

  it('setDefault patches correct endpoint', async () => {
    const resume = { id: 'r1', isDefault: true }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: resume })
    await expect(resumeApi.setDefault('r1')).resolves.toEqual(resume)
    expect(axiosClient.patch).toHaveBeenCalledWith('/students/resumes/r1/default')
  })

  it('delete calls delete endpoint', async () => {
    vi.mocked(axiosClient.delete).mockResolvedValue({ data: null })
    await resumeApi.delete('r1')
    expect(axiosClient.delete).toHaveBeenCalledWith('/students/resumes/r1')
  })

  it('getDownloadUrl returns signed URL object', async () => {
    const result = { downloadUrl: 'https://s3/file.pdf', expiresAt: '2025-01-01T00:00:00Z' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: result })
    await expect(resumeApi.getDownloadUrl('r1')).resolves.toEqual(result)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/resumes/r1/url')
  })
})
