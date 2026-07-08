export type InterviewType = 'PHONE' | 'VIDEO' | 'ONSITE' | 'TECHNICAL' | 'HR';
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'RESCHEDULED';

export interface Interview {
  id: string;
  applicationId: string;
  studentId: string;
  studentFirstName: string;
  studentLastName: string;
  jobId: string;
  jobTitle: string;
  companyName: string;
  roundNumber: number;
  interviewType: InterviewType;
  status: InterviewStatus;
  scheduledAt: string;
  durationMinutes?: number;
  meetingLink?: string;
  venue?: string;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
}
