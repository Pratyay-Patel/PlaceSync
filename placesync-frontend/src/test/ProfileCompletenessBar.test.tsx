import { render, screen } from '@testing-library/react'
import ProfileCompletenessBar from '../components/common/ProfileCompletenessBar'
import type { StudentProfile, Skill, Education, Experience } from '../types/student'

const BASE_PROFILE: StudentProfile = {
  id: 's1',
  userId: 'u1',
  firstName: '',
  lastName: '',
  institution: 'IIT',
  department: 'CS',
  graduationYear: 2025,
  profilePublic: false,
  createdAt: '',
  updatedAt: '',
}

const FULL_PROFILE: StudentProfile = {
  ...BASE_PROFILE,
  firstName: 'Alice',
  lastName: 'Smith',
  phone: '9999999999',
  dateOfBirth: '2000-01-01',
  bio: 'Bio here',
  cgpa: 8.5,
  profilePictureUrl: 'https://s3/pic.jpg',
}

const SKILL: Skill = { id: 'sk1', skillName: 'Java' }
const EDU: Education = { id: 'e1', degree: 'B.Tech', institution: 'IIT', startYear: 2020, createdAt: '', updatedAt: '' }
const EXP: Experience = { id: 'ex1', companyName: 'Google', role: 'SWE', startDate: '2023-06-01', isCurrent: false, createdAt: '', updatedAt: '' }

describe('ProfileCompletenessBar', () => {
  it('shows 0% for empty profile with no skills/education/experience', () => {
    render(
      <ProfileCompletenessBar profile={BASE_PROFILE} skills={[]} education={[]} experience={[]} />,
    )
    expect(screen.getByText('0%')).toBeInTheDocument()
  })

  it('shows 100% for fully complete profile', () => {
    render(
      <ProfileCompletenessBar
        profile={FULL_PROFILE}
        skills={[SKILL]}
        education={[EDU]}
        experience={[EXP]}
      />,
    )
    expect(screen.getByText('100%')).toBeInTheDocument()
    expect(screen.queryByText(/Missing:/)).not.toBeInTheDocument()
  })

  it('shows warning-range percentage (50-79%) for partially complete profile', () => {
    const partialProfile: StudentProfile = {
      ...BASE_PROFILE,
      firstName: 'Alice',
      lastName: 'Smith',
      phone: '9999999999',
      bio: 'Bio',
      cgpa: 8.0,
    }
    render(
      <ProfileCompletenessBar
        profile={partialProfile}
        skills={[SKILL]}
        education={[EDU]}
        experience={[]}
      />,
    )
    const pct = Number(screen.getByText(/\d+%/).textContent?.replace('%', ''))
    expect(pct).toBeGreaterThanOrEqual(50)
    expect(pct).toBeLessThan(80)
  })

  it('shows missing fields in caption when profile is incomplete', () => {
    render(
      <ProfileCompletenessBar profile={BASE_PROFILE} skills={[]} education={[]} experience={[]} />,
    )
    expect(screen.getByText(/Missing:/)).toBeInTheDocument()
    expect(screen.getByText(/First & last name/)).toBeInTheDocument()
  })

  it('uses error color (below 50%) for very incomplete profile', () => {
    render(
      <ProfileCompletenessBar
        profile={{ ...BASE_PROFILE, firstName: 'Alice', lastName: 'Smith' }}
        skills={[]}
        education={[]}
        experience={[]}
      />,
    )
    const pct = Number(screen.getByText(/\d+%/).textContent?.replace('%', ''))
    expect(pct).toBeLessThan(50)
  })
})
