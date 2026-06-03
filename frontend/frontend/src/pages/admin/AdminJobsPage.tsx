import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Pencil, Trash2, CheckCircle, XCircle, ExternalLink } from 'lucide-react';
import {
  adminListJobs,
  adminUpdateStatus,
  adminDeleteJob,
  type AdminJobPosting,
} from '../../api/admin.api';
import { formatDate } from '../../lib/formatDate';

const STATUS_CHIP: Record<string, string> = {
  DRAFT: 'bg-amber-100 text-amber-700',
  PUBLISHED: 'bg-green-100 text-green-700',
  CLOSED: 'bg-slate-100 text-slate-500',
};

const TABS = ['', 'DRAFT', 'PUBLISHED', 'CLOSED'] as const;

function nextStatus(current: string): string | null {
  if (current === 'DRAFT') return 'PUBLISHED';
  if (current === 'PUBLISHED') return 'CLOSED';
  return null;
}

export const AdminJobsPage = () => {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [tab, setTab] = useState<string>('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-jobs', tab],
    queryFn: () => adminListJobs({ status: tab || undefined, size: 100 }),
  });

  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      adminUpdateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-jobs'] }),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => adminDeleteJob(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-jobs'] }),
  });

  const jobs: AdminJobPosting[] = data?.content ?? [];

  return (
    <div className="max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display font-bold text-2xl text-slate-900">Job Postings</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Admin editor — create and manage all postings (REQ-JP-02)
          </p>
        </div>
        <button
          onClick={() => navigate('/admin/jobs/new')}
          className="btn-primary flex items-center gap-2 text-sm px-4 py-2"
        >
          <Plus aria-hidden="true" className="w-4 h-4" />
          New Posting
        </button>
      </div>

      {/* Status tabs */}
      <div className="flex gap-0 mb-4 border-b border-slate-200">
        {TABS.map((s) => (
          <button
            key={s}
            onClick={() => setTab(s)}
            className={`px-4 py-2 text-sm font-medium transition-colors focus:outline-none ${
              tab === s
                ? 'border-b-2 border-primary-600 text-primary-700'
                : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {s || 'All'}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-14 bg-slate-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <div className="text-center py-20 text-slate-400">
          <p className="mb-3">No postings found.</p>
          <button
            onClick={() => navigate('/admin/jobs/new')}
            className="btn-primary text-sm px-4 py-2"
          >
            Create first posting
          </button>
        </div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                {['Title', 'Status', 'Deadline', 'Apps', ''].map((h) => (
                  <th
                    key={h}
                    className={`px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide ${
                      h === 'Deadline' ? 'hidden md:table-cell' : h === 'Apps' ? 'hidden lg:table-cell' : ''
                    }`}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {jobs.map((job) => {
                const next = nextStatus(job.status);
                return (
                  <tr
                    key={job.id}
                    className="border-b border-slate-50 last:border-0 hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900 leading-tight">{job.title}</div>
                      <div className="text-xs text-slate-400 mt-0.5">
                        {job.seniority} · {job.locationCity || job.location || '—'}
                      </div>
                    </td>

                    <td className="px-4 py-3">
                      <span
                        className={`inline-flex px-2 py-0.5 rounded text-[11px] font-semibold ${
                          STATUS_CHIP[job.status] ?? 'bg-slate-100 text-slate-500'
                        }`}
                      >
                        {job.status}
                      </span>
                    </td>

                    <td className="px-4 py-3 text-slate-500 hidden md:table-cell">
                      {job.applicationDeadline ? formatDate(job.applicationDeadline) : '—'}
                    </td>

                    <td className="px-4 py-3 text-slate-500 hidden lg:table-cell">
                      {job.applicationsCount}
                    </td>

                    <td className="px-4 py-3">
                      <div className="flex items-center gap-0.5 justify-end">
                        {job.status === 'PUBLISHED' && job.slug && (
                          <a
                            href={`/careers/${job.slug}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            title="View on portal"
                            className="p-1.5 rounded text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                          >
                            <ExternalLink aria-hidden="true" className="w-4 h-4" />
                          </a>
                        )}

                        <Link
                          to={`/admin/jobs/${job.id}/edit`}
                          title="Edit"
                          className="p-1.5 rounded text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                        >
                          <Pencil aria-hidden="true" className="w-4 h-4" />
                        </Link>

                        {next && (
                          <button
                            onClick={() => statusMut.mutate({ id: job.id, status: next })}
                            disabled={statusMut.isPending}
                            title={next === 'PUBLISHED' ? 'Publish' : 'Close posting'}
                            className={`p-1.5 rounded transition-colors disabled:opacity-40 ${
                              next === 'PUBLISHED'
                                ? 'text-slate-400 hover:text-green-600 hover:bg-green-50'
                                : 'text-slate-400 hover:text-orange-500 hover:bg-orange-50'
                            }`}
                          >
                            {next === 'PUBLISHED' ? (
                              <CheckCircle aria-hidden="true" className="w-4 h-4" />
                            ) : (
                              <XCircle aria-hidden="true" className="w-4 h-4" />
                            )}
                          </button>
                        )}

                        <button
                          onClick={() => {
                            if (window.confirm(`Delete "${job.title}"?`))
                              deleteMut.mutate(job.id);
                          }}
                          disabled={deleteMut.isPending}
                          title="Delete"
                          className="p-1.5 rounded text-slate-400 hover:text-red-500 hover:bg-red-50 transition-colors disabled:opacity-40"
                        >
                          <Trash2 aria-hidden="true" className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <p className="text-xs text-slate-400 mt-4 text-right">
        {data?.totalElements ?? 0} total posting{data?.totalElements !== 1 ? 's' : ''}
      </p>
    </div>
  );
};
