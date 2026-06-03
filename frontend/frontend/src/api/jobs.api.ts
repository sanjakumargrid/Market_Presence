import api from './axios';
import type { Job, JobFilters } from '../features/jobs/types/job.types';

export const getJobs = async (): Promise<Job[]> => {
  const { data } = await api.get<Job[]>('/public/jobs');
  return data;
};

export const getJobBySlug = async (slug: string, channel = 'CAREERS_PORTAL'): Promise<Job> => {
  const { data } = await api.get<Job>(`/public/jobs/${slug}`, { params: { channel } });
  return data;
};

export const searchJobsPaginated = async (
  filters: JobFilters & { page?: number; size?: number }
) => {
  const jobs = await getJobs();
  const filtered = filterJobs(jobs, filters);

  const page = filters.page ?? 0;
  const size = filters.size ?? 12;
  const start = page * size;

  return {
    content: filtered.slice(start, start + size),
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size),
  };
};

export const getDepartments = async (): Promise<string[]> => {
  const jobs = await getJobs();
  const departments = new Set(jobs.map((j) => j.department).filter(Boolean));
  return Array.from(departments).sort();
};

export const filterJobs = (jobs: Job[], filters: JobFilters): Job[] => {
  let result = [...jobs];

  if (filters.search) {
    const q = filters.search.toLowerCase();
    result = result.filter(
      (j) =>
        (j.title ?? '').toLowerCase().includes(q) ||
        (j.department ?? '').toLowerCase().includes(q) ||
        (j.location_city ?? '').toLowerCase().includes(q) ||
        (j.job_category ?? '').toLowerCase().includes(q) ||
        (j.description ?? '').toLowerCase().includes(q) ||
        (j.requirements ?? '').toLowerCase().includes(q)
    );
  }

  if (filters.work_mode) result = result.filter((j) => j.work_mode === filters.work_mode);
  if (filters.experience_level) result = result.filter((j) => j.experience_level === filters.experience_level);
  if (filters.employment_type) result = result.filter((j) => j.employment_type === filters.employment_type);
  if (filters.location_city) result = result.filter((j) => j.location_city === filters.location_city);
  if (filters.department) result = result.filter((j) => j.department === filters.department);

  if (filters.sort === 'latest') {
    result.sort((a, b) => {
      const timeA = a.published_at ? new Date(a.published_at).getTime() : 0;
      const timeB = b.published_at ? new Date(b.published_at).getTime() : 0;
      return timeB - timeA;
    });
  } else if (filters.sort === 'salary_high') {
    result.sort((a, b) => (b.salary_max ?? 0) - (a.salary_max ?? 0));
  } else if (filters.sort === 'salary_low') {
    result.sort((a, b) => (a.salary_min ?? 0) - (b.salary_min ?? 0));
  } else if (filters.sort === 'az') {
    result.sort((a, b) => (a.title ?? '').localeCompare(b.title ?? ''));
  }

  return result;
};
