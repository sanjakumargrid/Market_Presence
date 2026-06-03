import api from './axios';
import type { CandidateProfile } from '../features/jobs/types/job.types';

export const getProfile = async (): Promise<CandidateProfile> => {
  const { data } = await api.get<{ data: CandidateProfile }>('/profile');
  return data.data;
};

export const updateProfile = async (profile: Partial<CandidateProfile>): Promise<CandidateProfile> => {
  const { data } = await api.put<{ data: CandidateProfile }>('/profile', profile);
  return data.data;
};

export const uploadResume = async (file: File): Promise<CandidateProfile> => {
  const fd = new FormData();
  fd.append('file', file);
  const { data } = await api.post<{ data: CandidateProfile }>('/profile/resume', fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data.data;
};
