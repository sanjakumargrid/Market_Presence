export interface Job {
  id: number;
  demand_id: number;
  title: string;
  slug: string;
  description: string;
  requirements: string;
  responsibilities: string;
  benefits: string;
  employment_type: string;
  experience_level: string;
  work_mode: string;
  location_city: string;
  location_state: string;
  location_country: string;
  department: string;
  job_category: string;
  salary_min: number;
  salary_max: number;
  currency: string;
  show_salary: boolean;
  posting_status: string;
  meta_title: string;
  meta_description: string;
  published_at: string | null;
  closed_at?: string | null;
  expires_at: string | null;
  created_by?: number;
  updated_by?: number;
  is_deleted?: boolean;
  created_at: string;
  updated_at: string;
}

export interface JobFilters {
  search?: string;
  work_mode?: string;
  experience_level?: string;
  employment_type?: string;
  location_city?: string;
  department?: string;
  sort?: 'latest' | 'salary_high' | 'salary_low' | 'az';
}

export interface Application {
  id: string;
  jobId: number;
  jobTitle: string;
  jobSlug: string;
  department: string;
  location: string;
  appliedDate: string;
  status: 'Applied' | 'Under Review' | 'Technical Interview' | 'Offer' | 'Rejected';
  nextStep?: string;
}

export interface ExperienceForm {
  companyName: string;
  designation: string;
  startDate: string;
  endDate: string;
  currentEmployer: boolean;
  responsibilities: string;
}

export interface EducationForm {
  institution: string;
  degree: string;
  specialization: string;
  startYear: string;
  endYear: string;
  cgpa: string;
}

export interface SkillForm {
  skillName: string;
  proficiency: string;
  yearsOfExperience: string;
}

export interface CertificationForm {
  certificationName: string;
  issuingOrganization: string;
  issueDate: string;
  expiryDate: string;
}

export interface ProjectForm {
  projectName: string;
  description: string;
  technologiesUsed: string;
  projectUrl: string;
}

export interface ApplicationForm {
  firstName: string;
  middleName: string;
  lastName: string;
  email: string;
  phone: string;
  currentLocation: string;
  linkedinUrl: string;
  portfolioUrl: string;
  githubUrl: string;
  
  experiences: ExperienceForm[];
  educations: EducationForm[];
  skills: SkillForm[];
  certifications: CertificationForm[];
  projects: ProjectForm[];
  
  screeningVisa: string;
  screeningNotice: string;
  screeningExpectedCtc: string;
  screeningRelocate: string;
  
  gender: string;
  veteranStatus: string;
  disabilityStatus: string;

  gdprConsent: boolean;
}

export interface CandidateProfile {
  name: string;
  email: string;
  phone: string;
  bio: string;
  professionalTitle: string;
  resumeFileName?: string;
  skills: string[];
  preferredLocations: string[];
  salaryExpectation: string;
  workMode: string;
  smartJobAlerts: boolean;
  applicationStatusUpdates: boolean;
  employerMessaging: boolean;
}
