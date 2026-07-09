export type JobStatus = 'PENDING_APPROVAL' | 'OPEN' | 'CLOSED' | 'EXPIRED';
export type JobLocationType = 'ONSITE' | 'REMOTE' | 'HYBRID';
export type JobType = 'FULL_TIME' | 'INTERNSHIP' | 'CONTRACT';

export interface JobSummary {
  id: string;
  title: string;
  companyId: string;
  companyName: string;
  locationType: JobLocationType;
  jobType: JobType;
  locationCity?: string;
  compensation?: string;
  applicationDeadline: string;
  minCgpa?: number;
  status: JobStatus;
  createdAt: string;
}

export interface Job {
  id: string;
  title: string;
  description: string;
  companyId: string;
  companyName: string;
  recruiterId: string;
  recruiterFirstName: string;
  recruiterLastName: string;
  locationType: JobLocationType;
  jobType: JobType;
  locationCity?: string;
  compensation?: string;
  applicationDeadline: string;
  minCgpa?: number;
  status: JobStatus;
  requiredSkills: string[];
  eligibleDepartments: string[];
  approvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface JobFilters {
  keyword?: string;
  locationType?: string;
  jobType?: string;
}
