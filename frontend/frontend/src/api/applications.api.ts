import { isAxiosError } from 'axios';
import api from './axios';
import type { Application } from '../features/jobs/types/job.types';

// ── Types sent by ApplicationPage ────────────────────────────────────────────

export interface ExperienceSubmit {
  company_name: string;
  designation: string;
  start_date: string;
  end_date: string | null;
  current_employer: boolean;
  responsibilities: string;
}

export interface EducationSubmit {
  institution: string;
  degree: string;
  specialization: string;
  start_year: number;
  end_year: number;
  cgpa: string;
}

export interface SkillSubmit {
  skill_name: string;
  proficiency: string;
  years_of_experience: number;
}

export interface CertificationSubmit {
  certification_name: string;
  issuing_organization: string;
  issue_date: string | null;
  expiry_date: string | null;
}

export interface ProjectSubmit {
  project_name: string;
  description: string;
  technologies_used: string;
  project_url: string;
}

export interface ApplicationSubmit {
  first_name: string;
  middle_name?: string;
  last_name: string;
  email: string;
  phone: string;
  current_location?: string;
  linkedin_url?: string;
  portfolio_url?: string;
  github_url?: string;

  experiences?: ExperienceSubmit[];
  educations?: EducationSubmit[];
  skills?: SkillSubmit[];
  certifications?: CertificationSubmit[];
  projects?: ProjectSubmit[];

  screening_answers?: Record<string, string>;

  gdpr_consent: boolean;

  gender?: string;
  veteran_status?: string;
  disability_status?: string;
}

export interface ApplicationStats {
  total: number;
  applied: number;
  interviewing: number;
  offers: number;
  rejected: number;
}

// ── Backend response shape ────────────────────────────────────────────────────

// ── Submit application ────────────────────────────────────────────────────────

/**
 * Sends the application as multipart/form-data:
 *   Part "application": JSON blob with full 10-step form data
 *   Part "resume"      : optional file
 *   ... other documents
 */
export const submitApplication = async (
  jobSlug: string,
  formData: ApplicationSubmit,
  files: {
    resume?: File;
    coverLetter?: File;
    certifications?: File;
    transcripts?: File;
    portfolio?: File;
  },
  referralCode?: string
): Promise<Application> => {
  // Build FormData — "application" blob must carry application/json content type
  // so Spring's @RequestPart can deserialise it with Jackson.
  const body = new FormData();
  body.append(
    'application',
    new Blob([JSON.stringify({ ...formData, referralCode })], { type: 'application/json' }),
    'application.json'
  );
  
  if (files.resume) body.append('resume', files.resume);
  if (files.coverLetter) body.append('coverLetter', files.coverLetter);
  if (files.certifications) body.append('certifications', files.certifications);
  if (files.transcripts) body.append('transcripts', files.transcripts);
  if (files.portfolio) body.append('portfolio', files.portfolio);

  try {
    const { data } = await api.post<{ data: any }>(
      `/applications/jobs/${jobSlug}/apply`,
      body,
      { headers: { 'Content-Type': undefined } }
    );

    return {
      id: data.data.id,
      jobId: data.data.jobId,
      jobTitle: data.data.jobTitle,
      jobSlug,
      department: data.data.department,
      location: data.data.location,
      appliedDate: data.data.appliedAt,
      status: data.data.status,
      nextStep: data.data.nextStep || 'HR Screening',
    };
  } catch (err: unknown) {
    if (isAxiosError(err)) {
      if (err.response?.status === 409) throw new Error('already applied');
      const serverError = err.response?.data?.error || err.response?.data?.message;
      if (serverError) throw new Error(serverError);
    }
    throw err;
  }
};

// ── Remaining API functions ───────────────────────────────────────────────────
// These endpoints exist in the frontend/backend service.
// They are kept here so that future wiring does not require touching ApplicationPage.

export const getMyApplications = async (): Promise<Application[]> => {
  const { data } = await api.get<{ data: Application[] }>('/applications/mine');
  return data.data;
};

export const getApplicationStats = async (): Promise<ApplicationStats> => {
  const { data } = await api.get<{ data: ApplicationStats }>('/applications/mine/stats');
  return data.data;
};

export const checkAlreadyApplied = async (jobSlug: string): Promise<boolean> => {
  try {
    const { data } = await api.get<{ data: { applied: boolean } }>(
      `/applications/jobs/${jobSlug}/check`
    );
    return data.data.applied;
  } catch {
    return false;
  }
};

export const updateApplicationStatus = async (
  applicationId: string,
  status: string,
  nextStep?: string
): Promise<Application> => {
  const { data } = await api.patch<{ data: Application }>(
    `/applications/${applicationId}/status`,
    { status, next_step: nextStep }
  );
  return data.data;
};
