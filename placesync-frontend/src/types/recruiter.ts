export type VerificationStatus = 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED';

export interface RecruiterProfile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  jobTitle?: string;
  contactEmail?: string;
  phone?: string;
  companyId?: string;
  companyName?: string;
  verificationStatus: VerificationStatus;
  verifiedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateRecruiterProfileRequest {
  firstName: string;
  lastName: string;
  jobTitle?: string;
  contactEmail?: string;
  phone?: string;
  companyId?: string;
}

export interface RecruiterStats {
  jobsPosted: number;
  totalApplications: number;
  shortlisted: number;
  offers: number;
}

export interface Company {
  id: string;
  name: string;
  description?: string;
  websiteUrl?: string;
  industry?: string;
  headquarters?: string;
  logoUrl?: string;
  status: string;
  createdAt: string;
}
