vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), patch: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { adminApi } from '../api/adminApi'

describe('adminApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getPlacementStats extracts data from ApiResponse', async () => {
    const data = { totalStudents: 200, placedStudents: 160, placementRate: 80 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data } })
    await expect(adminApi.getPlacementStats()).resolves.toEqual(data)
    expect(axiosClient.get).toHaveBeenCalledWith('/analytics/placement-stats')
  })

  it('getCompanyBreakdown extracts data array', async () => {
    const data = [{ companyName: 'Acme', placedCount: 10 }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data } })
    await expect(adminApi.getCompanyBreakdown()).resolves.toEqual(data)
    expect(axiosClient.get).toHaveBeenCalledWith('/analytics/companies')
  })

  it('getDepartmentBreakdown extracts data array', async () => {
    const data = [{ department: 'CS', placedCount: 50 }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data } })
    await expect(adminApi.getDepartmentBreakdown()).resolves.toEqual(data)
    expect(axiosClient.get).toHaveBeenCalledWith('/analytics/departments')
  })

  it('searchUsers passes params and returns paged data', async () => {
    const page = { content: [], totalPages: 0 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.searchUsers({ email: 'a@b.com', page: 0 })).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/users', {
      params: { email: 'a@b.com', page: 0, size: 20 },
    })
  })

  it('getUserById calls correct endpoint', async () => {
    const user = { id: 'u1', email: 'a@b.com' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: user })
    await expect(adminApi.getUserById('u1')).resolves.toEqual(user)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/users/u1')
  })

  it('updateUserStatus sends patch', async () => {
    const user = { id: 'u1', isActive: false }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: user })
    await expect(adminApi.updateUserStatus('u1', false)).resolves.toEqual(user)
    expect(axiosClient.patch).toHaveBeenCalledWith('/admin/users/u1/status', { isActive: false })
  })

  it('getPendingRecruiters calls paginated endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.getPendingRecruiters(1, 10)).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/recruiters/pending?page=1&size=10')
  })

  it('verifyRecruiter sends approve decision', async () => {
    const recruiter = { id: 'r1' }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: recruiter })
    await expect(adminApi.verifyRecruiter('r1', 'APPROVE')).resolves.toEqual(recruiter)
    expect(axiosClient.patch).toHaveBeenCalledWith('/admin/recruiters/r1/verify', {
      decision: 'APPROVE', rejectionReason: undefined,
    })
  })

  it('verifyRecruiter sends reject decision with reason', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await adminApi.verifyRecruiter('r1', 'REJECT', 'Incomplete profile')
    expect(axiosClient.patch).toHaveBeenCalledWith('/admin/recruiters/r1/verify', {
      decision: 'REJECT', rejectionReason: 'Incomplete profile',
    })
  })

  it('getPendingCompanies calls paginated endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.getPendingCompanies()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/companies/pending?page=0&size=20')
  })

  it('verifyCompany sends approve decision', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await adminApi.verifyCompany('c1', 'APPROVE')
    expect(axiosClient.patch).toHaveBeenCalledWith('/admin/companies/c1/verify', {
      decision: 'APPROVE', rejectionReason: undefined,
    })
  })

  it('getPendingJobs calls paginated endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.getPendingJobs()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/jobs/pending?page=0&size=20')
  })

  it('approveJob sends decision', async () => {
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: {} })
    await adminApi.approveJob('j1', 'REJECT', 'Spam')
    expect(axiosClient.patch).toHaveBeenCalledWith('/admin/jobs/j1/approve', {
      decision: 'REJECT', rejectionReason: 'Spam',
    })
  })

  it('getAllApplications passes params', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.getAllApplications({ status: 'APPLIED', page: 0 })).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/applications', {
      params: { status: 'APPLIED', page: 0, size: 20 },
    })
  })

  it('getAllInterviews calls paginated endpoint', async () => {
    const page = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: page })
    await expect(adminApi.getAllInterviews()).resolves.toEqual(page)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/interviews?page=0&size=20')
  })

  it('getAuditLog extracts nested data', async () => {
    const data = { content: [], totalPages: 1 }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: { success: true, data } })
    await expect(adminApi.getAuditLog({ page: 0 })).resolves.toEqual(data)
    expect(axiosClient.get).toHaveBeenCalledWith('/admin/audit-log', {
      params: { page: 0, size: 20, sort: 'createdAt,desc' },
    })
  })
})
