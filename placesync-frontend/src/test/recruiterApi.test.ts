vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { recruiterApi } from '../api/recruiterApi'

describe('recruiterApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getProfile calls correct endpoint', async () => {
    const profile = { id: 'r1', firstName: 'Jane' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: profile })
    await expect(recruiterApi.getProfile()).resolves.toEqual(profile)
    expect(axiosClient.get).toHaveBeenCalledWith('/recruiters/profile')
  })

  it('updateProfile puts updated data', async () => {
    const update = { firstName: 'Jane', lastName: 'Doe', designation: 'HR' }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: update })
    await expect(recruiterApi.updateProfile(update as never)).resolves.toEqual(update)
    expect(axiosClient.put).toHaveBeenCalledWith('/recruiters/profile', update)
  })

  it('getMyJobs calls paginated endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(recruiterApi.getMyJobs()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith(
      '/recruiters/jobs?page=0&size=20&sort=createdAt,desc',
    )
  })

  it('getStats extracts nested data from ApiResponse', async () => {
    const stats = { totalJobs: 5, totalApplications: 30 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data: stats } })
    await expect(recruiterApi.getStats()).resolves.toEqual(stats)
    expect(axiosClient.get).toHaveBeenCalledWith('/analytics/recruiter')
  })

  it('getJobApplications calls correct endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(recruiterApi.getJobApplications('j1')).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith(
      '/recruiters/jobs/j1/applications?page=0&size=20',
    )
  })

  it('updateApplicationStatus patches with status and note', async () => {
    const app = { id: 'a1', status: 'SHORTLISTED' }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: app })
    await expect(
      recruiterApi.updateApplicationStatus('a1', 'SHORTLISTED' as never, 'Good fit'),
    ).resolves.toEqual(app)
    expect(axiosClient.patch).toHaveBeenCalledWith('/recruiters/applications/a1/status', {
      status: 'SHORTLISTED', note: 'Good fit',
    })
  })

  it('getApplicationInterviews returns array', async () => {
    const interviews = [{ id: 'i1' }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: interviews })
    await expect(recruiterApi.getApplicationInterviews('a1')).resolves.toEqual(interviews)
    expect(axiosClient.get).toHaveBeenCalledWith('/recruiters/applications/a1/interviews')
  })

  it('scheduleInterview posts to correct endpoint', async () => {
    const data = {
      roundNumber: 1, interviewType: 'TECHNICAL',
      scheduledAt: '2025-01-01T10:00:00Z', durationMinutes: 60,
    }
    const interview = { id: 'i1', ...data }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: interview })
    await expect(recruiterApi.scheduleInterview('a1', data)).resolves.toEqual(interview)
    expect(axiosClient.post).toHaveBeenCalledWith('/recruiters/applications/a1/interviews', data)
  })

  it('rescheduleInterview puts updated schedule', async () => {
    const data = { scheduledAt: '2025-02-01T10:00:00Z' }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: { id: 'i1' } })
    await recruiterApi.rescheduleInterview('i1', data)
    expect(axiosClient.put).toHaveBeenCalledWith('/recruiters/interviews/i1', data)
  })

  it('cancelInterview patches with reason', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await recruiterApi.cancelInterview('i1', 'Rescheduled')
    expect(axiosClient.patch).toHaveBeenCalledWith('/recruiters/interviews/i1/cancel', {
      cancellationReason: 'Rescheduled',
    })
  })

  it('completeInterview patches correct endpoint', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await recruiterApi.completeInterview('i1')
    expect(axiosClient.patch).toHaveBeenCalledWith('/recruiters/interviews/i1/complete', {})
  })

  it('getResumeDownloadUrl returns download URL', async () => {
    const result = { downloadUrl: 'https://s3/file.pdf', expiresAt: '2025-01-01T00:00:00Z' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: result })
    await expect(recruiterApi.getResumeDownloadUrl('res1')).resolves.toEqual(result)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/resumes/res1/url')
  })
})
