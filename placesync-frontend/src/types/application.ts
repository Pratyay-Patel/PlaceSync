export type ApplicationStatus =
  | 'APPLIED'
  | 'UNDER_REVIEW'
  | 'SHORTLISTED'
  | 'INTERVIEW_SCHEDULED'
  | 'OFFERED'
  | 'REJECTED';

export interface Application {
  id: string;
  studentId: string;
  studentFirstName: string;
  studentLastName: string;
  jobId: string;
  jobTitle: string;
  companyName: string;
  resumeId: string;
  resumeLabel: string;
  status: ApplicationStatus;
  appliedAt: string;
  updatedAt: string;
  statusUpdatedAt: string;
}
