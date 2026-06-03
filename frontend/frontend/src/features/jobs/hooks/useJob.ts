import { useQuery } from '@tanstack/react-query';
import { getJobBySlug } from '../../../api/jobs.api';

export const useJob = (slug: string, channel = 'CAREERS_PORTAL') =>
  useQuery({
    queryKey: ['job', slug, channel],
    queryFn: () => getJobBySlug(slug, channel),
    enabled: !!slug,
  });
