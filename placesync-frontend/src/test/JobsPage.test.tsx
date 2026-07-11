import { screen, waitFor, fireEvent } from '@testing-library/react';
import StudentJobsPage from '../pages/student/JobsPage';
import { jobApi } from '../api/jobApi';
import { renderWithProviders } from './testUtils';
import type { PagedResponse } from '../types/common';
import type { JobSummary } from '../types/job';

vi.mock('../api/jobApi');

const makePage = (jobs: Partial<JobSummary>[]): PagedResponse<JobSummary> => ({
  content: jobs as JobSummary[],
  totalPages: 1,
  totalElements: jobs.length,
  page: 0,
  size: 12,
  last: true,
});

const JOBS: Partial<JobSummary>[] = [
  {
    id: 'j1',
    title: 'Frontend Engineer',
    companyName: 'Acme Corp',
    locationType: 'REMOTE',
    jobType: 'FULL_TIME',
    applicationDeadline: '2026-12-31',
    status: 'OPEN',
  },
  {
    id: 'j2',
    title: 'Backend Developer',
    companyName: 'Beta Ltd',
    locationType: 'ONSITE',
    jobType: 'INTERNSHIP',
    applicationDeadline: '2026-11-30',
    status: 'OPEN',
  },
];

beforeEach(() => {
  vi.mocked(jobApi.list).mockReset();
  vi.mocked(jobApi.list).mockResolvedValue(makePage(JOBS));
});

describe('StudentJobsPage', () => {
  it('renders job titles from API', async () => {
    renderWithProviders(<StudentJobsPage />);
    await waitFor(() => {
      expect(screen.getByText('Frontend Engineer')).toBeInTheDocument();
    });
    expect(screen.getByText('Backend Developer')).toBeInTheDocument();
  });

  it('calls jobApi.list with keyword when Apply Filters is clicked', async () => {
    renderWithProviders(<StudentJobsPage />);
    await waitFor(() => {
      expect(screen.getByText('Frontend Engineer')).toBeInTheDocument();
    });

    vi.mocked(jobApi.list).mockResolvedValueOnce(makePage([JOBS[0]]));

    const keywordInput = screen.getByLabelText(/keyword/i);
    fireEvent.change(keywordInput, { target: { value: 'frontend' } });
    fireEvent.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() => {
      expect(vi.mocked(jobApi.list)).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'frontend' }),
        0,
        12,
      );
    });
  });
});
