import { useQuery } from '@tanstack/react-query';
import { getJobs } from '../../../api/jobs.api';

export const useJobs = () =>
  useQuery({ queryKey: ['jobs'], queryFn: getJobs, staleTime: 5 * 60 * 1000 });
