export type GenderType = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export interface StudentProfile {
  id: string;
  userId: string;
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: GenderType;
  institution: string;
  department: string;
  graduationYear: number;
  cgpa?: number;
  bio?: string;
  profilePictureUrl?: string;
  profilePublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: GenderType;
  institution: string;
  department: string;
  graduationYear: number;
  cgpa?: number;
  bio?: string;
  isProfilePublic?: boolean;
}

export interface Skill {
  id: string;
  skillName: string;
}

export interface Education {
  id: string;
  degree: string;
  institution: string;
  fieldOfStudy?: string;
  startYear: number;
  endYear?: number;
  percentageOrCgpa?: number;
  createdAt: string;
  updatedAt: string;
}

export interface EducationRequest {
  degree: string;
  institution: string;
  fieldOfStudy?: string;
  startYear: number;
  endYear?: number;
  percentageOrCgpa?: number;
}

export interface Experience {
  id: string;
  companyName: string;
  role: string;
  description?: string;
  startDate: string;
  endDate?: string;
  isCurrent: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ExperienceRequest {
  companyName: string;
  role: string;
  description?: string;
  startDate: string;
  endDate?: string;
  isCurrent: boolean;
}

export interface Resume {
  id: string;
  label: string;
  originalFilename: string;
  fileSizeBytes: number;
  isDefault: boolean;
  uploadedAt: string;
}
