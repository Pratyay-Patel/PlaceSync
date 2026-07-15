vi.mock('../api/axiosClient', () => ({
  default: { post: vi.fn() },
}))

import axios from 'axios'
import axiosClient from '../api/axiosClient'
import { authApi } from '../api/authApi'

describe('authApi', () => {
  afterEach(() => vi.restoreAllMocks())

  it('login uses bare axios (not axiosClient) to avoid refresh loop', async () => {
    const response = { accessToken: 'tok', refreshToken: 'rt', userId: 'u1', email: 'a@b.com', role: 'ROLE_STUDENT', expiresIn: 3600 }
    vi.spyOn(axios, 'post').mockResolvedValue({ data: response })
    await expect(authApi.login({ email: 'a@b.com', password: 'pass' })).resolves.toEqual(response)
    expect(axios.post).toHaveBeenCalledWith('/api/v1/auth/login', { email: 'a@b.com', password: 'pass' })
  })

  it('register uses bare axios', async () => {
    const response = { accessToken: 'tok', refreshToken: 'rt', userId: 'u2', email: 'b@c.com', role: 'ROLE_RECRUITER', expiresIn: 3600 }
    vi.spyOn(axios, 'post').mockResolvedValue({ data: response })
    const req = { email: 'b@c.com', password: 'P@ssw0rd', role: 'ROLE_RECRUITER' as const, firstName: 'Jane', lastName: 'Doe' }
    await expect(authApi.register(req)).resolves.toEqual(response)
    expect(axios.post).toHaveBeenCalledWith('/api/v1/auth/register', req)
  })

  it('logout calls axiosClient with refresh token', async () => {
    vi.mocked(axiosClient.post).mockResolvedValue({ data: {} })
    await authApi.logout('rt-abc')
    expect(axiosClient.post).toHaveBeenCalledWith('/auth/logout', { refreshToken: 'rt-abc' })
  })

  it('verifyEmail uses bare axios with token in query string', async () => {
    vi.spyOn(axios, 'get').mockResolvedValue({ data: {} })
    await authApi.verifyEmail('email-token-123')
    expect(axios.get).toHaveBeenCalledWith('/api/v1/auth/verify-email?token=email-token-123')
  })

  it('forgotPassword posts email', async () => {
    vi.spyOn(axios, 'post').mockResolvedValue({ data: {} })
    await authApi.forgotPassword('user@example.com')
    expect(axios.post).toHaveBeenCalledWith('/api/v1/auth/forgot-password', { email: 'user@example.com' })
  })

  it('resetPassword posts token and new password', async () => {
    vi.spyOn(axios, 'post').mockResolvedValue({ data: {} })
    await authApi.resetPassword('reset-token', 'NewP@ss1')
    expect(axios.post).toHaveBeenCalledWith('/api/v1/auth/reset-password', {
      token: 'reset-token', newPassword: 'NewP@ss1',
    })
  })
})
