import api from './axios';

export interface AdminJobPosting {
  id: number;
  demandId?: number;
  title: string;
  slug: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED';
  description?: string;
  requirements?: string;
  responsibilities?: string;
  benefits?: string;
  employmentType?: string;
  workMode?: string;
  seniority: string;
  location?: string;
  locationCity?: string;
  locationState?: string;
  locationCountry?: string;
  department?: string;
  jobCategory?: string;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  showSalary?: boolean;
  metaTitle?: string;
  metaDescription?: string;
  applicationDeadline?: string;
  applicationsCount: number;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminJobListResponse {
  content: AdminJobPosting[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface JobPayload {
  title: string;
  description?: string;
  location?: string;
  seniority: string;
  applicationDeadline: string;
  requirements?: string;
  responsibilities?: string;
  benefits?: string;
  employmentType?: string;
  workMode?: string;
  locationCity?: string;
  locationState?: string;
  locationCountry?: string;
  department?: string;
  jobCategory?: string;
  salaryMin?: number;
  salaryMax?: number;
  currency?: string;
  showSalary?: boolean;
  metaTitle?: string;
  metaDescription?: string;
}

export const adminListJobs = (params?: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<AdminJobListResponse> =>
  api.get<AdminJobListResponse>('/job-postings', { params: { size: 100, ...params } }).then((r) => r.data);

export const adminGetJob = (id: number): Promise<AdminJobPosting> =>
  api.get<AdminJobPosting>(`/job-postings/${id}`).then((r) => r.data);

export const adminCreateJob = (payload: JobPayload): Promise<AdminJobPosting> =>
  api.post<AdminJobPosting>('/job-postings', payload).then((r) => r.data);

export const adminUpdateJob = (id: number, payload: JobPayload): Promise<AdminJobPosting> =>
  api.put<AdminJobPosting>(`/job-postings/${id}`, payload).then((r) => r.data);

export const adminUpdateStatus = (id: number, status: string): Promise<AdminJobPosting> =>
  api.patch<AdminJobPosting>(`/job-postings/${id}/status`, { status }).then((r) => r.data);

export const adminDeleteJob = (id: number): Promise<void> =>
  api.delete(`/job-postings/${id}`).then(() => undefined);

// ── Channel API (REQ-JP-03) ──────────────────────────────────────────────────

export interface AdminChannel {
  id?: number;
  jobPostingId: number;
  channelName: string;
  channelUrl?: string;
  status: string;
  errorMessage?: string;
  publishedAt?: string;
  unpublishedAt?: string;
  lastUpdatedAt?: string;
  expiresAt?: string;
}

export const adminGetChannels = (jobId: number): Promise<AdminChannel[]> =>
  api.get<AdminChannel[]>(`/job-postings/${jobId}/channels`).then((r) => r.data);

export const adminPublishChannel = (jobId: number, channel: string): Promise<AdminChannel> =>
  api.post<AdminChannel>(`/job-postings/${jobId}/channels/${channel}/publish`).then((r) => r.data);

export const adminUnpublishChannel = (jobId: number, channel: string): Promise<AdminChannel> =>
  api.post<AdminChannel>(`/job-postings/${jobId}/channels/${channel}/unpublish`).then((r) => r.data);

// ── Handoff API (REQ-JP-08) ──────────────────────────────────────────────────

export interface HandoffRecord {
  id: number;
  applicationIntakeId: number;
  candidateEmail: string;
  candidatePhone?: string;
  jobSlug: string;
  jobTitle: string;
  source: string;
  status: 'PENDING' | 'SENT' | 'FAILED';
  errorMessage?: string;
  team2ResponseId?: string;
  attemptedAt?: string;
  createdAt: string;
}

export interface HandoffListResponse {
  content: HandoffRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export const adminGetHandoffs = (params?: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<HandoffListResponse> =>
  api.get<HandoffListResponse>('/admin/handoffs', { params: { size: 50, ...params } }).then((r) => r.data);

export const adminRetryHandoff = (id: number): Promise<HandoffRecord> =>
  api.post<HandoffRecord>(`/admin/handoffs/${id}/retry`).then((r) => r.data);

export const adminRetryAllPending = (): Promise<{ retriedCount: number }> =>
  api.post<{ retriedCount: number }>('/admin/handoffs/retry-pending').then((r) => r.data);
