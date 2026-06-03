import type { Application, CandidateProfile } from '../features/jobs/types/job.types';

const APPLIED_KEY = 'applied_jobs';
const PROFILE_KEY = 'candidate_profile';

export const getAppliedJobs = (): Application[] => {
  try {
    return JSON.parse(localStorage.getItem(APPLIED_KEY) ?? '[]');
  } catch {
    return [];
  }
};

export const addApplication = (app: Application): void => {
  const apps = getAppliedJobs();
  apps.push(app);
  localStorage.setItem(APPLIED_KEY, JSON.stringify(apps));
};

export const hasApplied = (jobSlug: string): boolean => {
  return getAppliedJobs().some((a) => a.jobSlug === jobSlug);
};

export const getProfile = (): CandidateProfile => {
  try {
    const stored = localStorage.getItem(PROFILE_KEY);
    if (stored) return JSON.parse(stored);
  } catch {}
  return {
    name: '',
    email: '',
    phone: '',
    bio: '',
    professionalTitle: '',
    skills: [],
    preferredLocations: [],
    salaryExpectation: '',
    workMode: 'REMOTE',
    smartJobAlerts: true,
    applicationStatusUpdates: true,
    employerMessaging: false,
  };
};

export const saveProfile = (profile: CandidateProfile): void => {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
};
