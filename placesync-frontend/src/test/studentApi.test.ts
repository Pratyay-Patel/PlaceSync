vi.mock('../api/axiosClient', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), patch: vi.fn(), delete: vi.fn() },
}))

import axiosClient from '../api/axiosClient'
import { studentApi } from '../api/studentApi'

describe('studentApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getProfile calls correct endpoint', async () => {
    const profile = { id: 's1', firstName: 'Alice' }
    vi.mocked(axiosClient.get).mockResolvedValue({ data: profile })
    await expect(studentApi.getProfile()).resolves.toEqual(profile)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/profile')
  })

  it('updateProfile puts data', async () => {
    const update = { firstName: 'Alice', lastName: 'Smith' }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: update })
    await expect(studentApi.updateProfile(update as never)).resolves.toEqual(update)
    expect(axiosClient.put).toHaveBeenCalledWith('/students/profile', update)
  })

  it('uploadPicture posts multipart form', async () => {
    const profile = { id: 's1', profilePictureUrl: 'https://s3/pic.jpg' }
    vi.mocked(axiosClient.patch).mockResolvedValue({ data: profile })
    const file = new File(['img'], 'pic.jpg', { type: 'image/jpeg' })
    await expect(studentApi.uploadPicture(file)).resolves.toEqual(profile)
    expect(axiosClient.patch).toHaveBeenCalledWith(
      '/students/profile/picture',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
  })

  it('getSkills returns skill array', async () => {
    const skills = [{ id: 'sk1', skillName: 'Java' }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: skills })
    await expect(studentApi.getSkills()).resolves.toEqual(skills)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/profile/skills')
  })

  it('addSkill posts skillName', async () => {
    vi.mocked(axiosClient.post).mockResolvedValue({ data: { id: 'sk1' } })
    await studentApi.addSkill('Java')
    expect(axiosClient.post).toHaveBeenCalledWith('/students/profile/skills', { skillName: 'Java' })
  })

  it('removeSkill deletes correct endpoint', async () => {
    vi.mocked(axiosClient.delete).mockResolvedValue({ data: null })
    await studentApi.removeSkill('sk1')
    expect(axiosClient.delete).toHaveBeenCalledWith('/students/profile/skills/sk1')
  })

  it('getEducation returns education array', async () => {
    const education = [{ id: 'e1', institution: 'MIT' }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: education })
    await expect(studentApi.getEducation()).resolves.toEqual(education)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/profile/education')
  })

  it('addEducation posts data', async () => {
    const data = { institution: 'MIT', degree: 'B.Tech', field: 'CS', startYear: 2020, endYear: 2024 }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: { id: 'e1', ...data } })
    await studentApi.addEducation(data as never)
    expect(axiosClient.post).toHaveBeenCalledWith('/students/profile/education', data)
  })

  it('updateEducation puts data', async () => {
    const data = { institution: 'IIT', degree: 'B.Tech', field: 'CS', startYear: 2020, endYear: 2024 }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: { id: 'e1' } })
    await studentApi.updateEducation('e1', data as never)
    expect(axiosClient.put).toHaveBeenCalledWith('/students/profile/education/e1', data)
  })

  it('deleteEducation deletes correct endpoint', async () => {
    vi.mocked(axiosClient.delete).mockResolvedValue({ data: null })
    await studentApi.deleteEducation('e1')
    expect(axiosClient.delete).toHaveBeenCalledWith('/students/profile/education/e1')
  })

  it('getExperience returns experience array', async () => {
    const experience = [{ id: 'ex1', company: 'Google' }]
    vi.mocked(axiosClient.get).mockResolvedValue({ data: experience })
    await expect(studentApi.getExperience()).resolves.toEqual(experience)
    expect(axiosClient.get).toHaveBeenCalledWith('/students/profile/experience')
  })

  it('addExperience posts data', async () => {
    const data = { company: 'Google', role: 'SWE Intern', startDate: '2023-06-01' }
    vi.mocked(axiosClient.post).mockResolvedValue({ data: { id: 'ex1' } })
    await studentApi.addExperience(data as never)
    expect(axiosClient.post).toHaveBeenCalledWith('/students/profile/experience', data)
  })

  it('updateExperience puts data', async () => {
    const data = { company: 'Meta', role: 'SWE', startDate: '2024-01-01' }
    vi.mocked(axiosClient.put).mockResolvedValue({ data: { id: 'ex1' } })
    await studentApi.updateExperience('ex1', data as never)
    expect(axiosClient.put).toHaveBeenCalledWith('/students/profile/experience/ex1', data)
  })

  it('deleteExperience deletes correct endpoint', async () => {
    vi.mocked(axiosClient.delete).mockResolvedValue({ data: null })
    await studentApi.deleteExperience('ex1')
    expect(axiosClient.delete).toHaveBeenCalledWith('/students/profile/experience/ex1')
  })
})
