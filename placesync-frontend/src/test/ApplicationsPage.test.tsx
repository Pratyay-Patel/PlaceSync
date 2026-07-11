import { screen, waitFor } from '@testing-library/react';
import StudentApplicationsPage from '../pages/student/ApplicationsPage';
import { applicationApi } from '../api/applicationApi';
import { renderWithProviders } from './testUtils';
import type { PagedResponse } from '../types/common';
import type { Application } from '../types/application';

vi.mock('../api/applicationApi');

const makePage = (apps: Partial<Application>[]): PagedResponse<Application> => ({
  content: apps as Application[],
  totalPages: 1,
  totalElements: apps.length,
  page: 0,
  size: 15,
  last: true,
});

const APPLICATIONS: Partial<Application>[] = [
  {
    id: 'app-1',
    jobTitle: 'Frontend Engineer',
    companyName: 'Acme Corp',
    status: 'APPLIED',
    appliedAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
  },
  {
    id: 'app-2',
    jobTitle: 'Backend Developer',
    companyName: 'Beta Ltd',
    status: 'SHORTLISTED',
    appliedAt: '2026-06-15T09:00:00Z',
    updatedAt: '2026-07-05T12:00:00Z',
  },
];

beforeEach(() => {
  vi.mocked(applicationApi.getMyApplications).mockReset();
  vi.mocked(applicationApi.getMyApplications).mockResolvedValue(makePage(APPLICATIONS));
});

describe('StudentApplicationsPage', () => {
  it('renders job titles from API', async () => {
    renderWithProviders(<StudentApplicationsPage />);
    await waitFor(() => {
      expect(screen.getByText('Frontend Engineer')).toBeInTheDocument();
    });
    expect(screen.getByText('Backend Developer')).toBeInTheDocument();
  });

  it('displays human-readable status labels via StatusChip', async () => {
    renderWithProviders(<StudentApplicationsPage />);
    await waitFor(() => {
      expect(screen.getByText('Applied')).toBeInTheDocument();
    });
    expect(screen.getByText('Shortlisted')).toBeInTheDocument();
  });
});
