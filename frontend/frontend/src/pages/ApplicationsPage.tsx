import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMyApplications, getApplicationStats } from '../api/applications.api';
import { getAppliedJobs } from '../store/localStorage';
import { formatDate } from '../lib/formatDate';
import { Briefcase, Users, Star, XCircle, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { clsx } from 'clsx';
import { useAuthStore } from '../store/authStore';

const statusColors: Record<string, string> = {
  Applied: 'bg-blue-100 text-blue-700',
  APPLIED: 'bg-blue-100 text-blue-700',
  'Under Review': 'bg-yellow-100 text-yellow-700',
  UNDER_REVIEW: 'bg-yellow-100 text-yellow-700',
  'Technical Interview': 'bg-indigo-100 text-indigo-700',
  TECHNICAL_INTERVIEW: 'bg-indigo-100 text-indigo-700',
  Offer: 'bg-green-100 text-green-700',
  OFFER: 'bg-green-100 text-green-700',
  Rejected: 'bg-red-100 text-red-700',
  REJECTED: 'bg-red-100 text-red-700',
};

const statusLabel: Record<string, string> = {
  APPLIED: 'Applied', UNDER_REVIEW: 'Under Review',
  TECHNICAL_INTERVIEW: 'Technical Interview', HR_INTERVIEW: 'HR Interview',
  OFFER: 'Offer', REJECTED: 'Rejected', WITHDRAWN: 'Withdrawn',
};

const StatCard = ({ label, value, sub, icon, color }: { label: string; value: number; sub?: string; icon: React.ReactNode; color: string }) => (
  <div className="card p-5">
    <div className="flex items-start justify-between mb-2">
      <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${color}`}>{icon}</div>
      <span className="text-xs text-slate-400 font-medium">{label}</span>
    </div>
    <p className="font-display font-bold text-3xl text-slate-900">{value}</p>
    {sub && <p className="text-xs text-slate-400 mt-0.5">{sub}</p>}
  </div>
);

export const ApplicationsPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const [selected, setSelected] = useState<any>(null);

  // Try backend first, fall back to localStorage
  const { data: backendApps } = useQuery({
    queryKey: ['my-applications'],
    queryFn: getMyApplications,
    enabled: isAuthenticated,
  });

  const { data: stats } = useQuery({
    queryKey: ['app-stats'],
    queryFn: getApplicationStats,
    enabled: isAuthenticated,
  });

  // Merge: backend data if available, else localStorage
  const localApps = getAppliedJobs();
  const apps = backendApps && backendApps.length > 0
    ? backendApps.map((a: any) => ({
        id: a.id,
        jobId: a.job_id ?? a.jobId,
        jobTitle: a.job_title ?? a.jobTitle,
        jobSlug: a.job_slug ?? a.jobSlug,
        department: a.department,
        location: a.location,
        appliedDate: a.applied_at ?? a.appliedDate,
        status: statusLabel[a.status] ?? a.status,
        nextStep: a.next_step ?? a.nextStep,
      }))
    : localApps;

  const totalApps = stats?.total ?? apps.length;
  const interviewing = stats?.interviewing ?? apps.filter((a) => a.status.includes('Interview')).length;
  const offers = stats?.offers ?? apps.filter((a) => String(a.status) === 'Offer' || String(a.status) === 'OFFER').length;
  const rejected = stats?.rejected ?? apps.filter((a) => String(a.status) === 'Rejected' || String(a.status) === 'REJECTED').length;

  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="font-display font-bold text-2xl text-slate-900 mb-1">Application Tracker</h1>
      <p className="text-slate-500 text-sm mb-6">Manage and track your active career opportunities.</p>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        <StatCard label="Applied" value={totalApps} sub="+2 this week" icon={<Briefcase className="w-4 h-4 text-blue-600" />} color="bg-blue-50" />
        <StatCard label="Interviewing" value={interviewing} sub="Next: Tech Interview" icon={<Users className="w-4 h-4 text-indigo-600" />} color="bg-indigo-50" />
        <StatCard label="Offers" value={offers} sub="Expiring in 48h" icon={<Star className="w-4 h-4 text-green-600" />} color="bg-green-50" />
        <StatCard label="Rejected" value={rejected} sub="Past 12 months" icon={<XCircle className="w-4 h-4 text-slate-500" />} color="bg-slate-100" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 card overflow-hidden">
          <div className="flex items-center justify-between p-5 border-b border-slate-100">
            <h2 className="font-semibold text-slate-800">Current Pipeline</h2>
          </div>
          {apps.length === 0 ? (
            <div className="p-10 text-center">
              <p className="text-slate-400 text-sm">No applications yet.</p>
              <button onClick={() => navigate('/jobs')} className="btn-primary mt-4 text-sm px-5 py-2">Browse Jobs</button>
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-xs text-slate-400 uppercase tracking-wide border-b border-slate-100">
                  <th className="text-left p-4 font-medium">Job Title</th>
                  <th className="text-left p-4 font-medium hidden sm:table-cell">Applied Date</th>
                  <th className="text-left p-4 font-medium">Status</th>
                  <th className="text-left p-4 font-medium hidden md:table-cell">Next Step</th>
                </tr>
              </thead>
              <tbody>
                {apps.map((app) => (
                  <tr key={app.id} onClick={() => setSelected(app)}
                    className="border-b border-slate-50 hover:bg-slate-50 cursor-pointer transition-colors">
                    <td className="p-4">
                      <p className="font-medium text-slate-900 text-xs">{app.jobTitle}</p>
                      <p className="text-[11px] text-slate-400">{app.department} · {app.location}</p>
                    </td>
                    <td className="p-4 text-xs text-slate-500 hidden sm:table-cell">{formatDate(app.appliedDate)}</td>
                    <td className="p-4">
                      <span className={clsx('badge text-[10px]', statusColors[app.status] ?? 'bg-slate-100 text-slate-600')}>
                        {app.status}
                      </span>
                    </td>
                    <td className="p-4 text-xs text-slate-500 hidden md:table-cell">{app.nextStep ?? 'Awaiting Feedback'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card p-5">
          {selected ? (
            <>
              <div className="rounded-xl bg-gradient-to-br from-slate-700 to-slate-900 h-28 mb-4 flex items-center justify-center">
                <Briefcase className="w-8 h-8 text-slate-400" />
              </div>
              <h3 className="font-semibold text-slate-900 mb-0.5">{selected.jobTitle}</h3>
              <p className="text-xs text-slate-500 mb-4">{selected.location}</p>
              <div className="space-y-3">
                {[
                  { label: 'Application Submitted', date: selected.appliedDate },
                  { label: 'HR Screening', date: '' },
                  { label: 'Technical Interview', date: '' },
                ].map((ev, i) => (
                  <div key={i} className="flex gap-3 items-start">
                    <div className={clsx('w-2.5 h-2.5 rounded-full mt-1 shrink-0', i === 0 ? 'bg-primary-500' : 'bg-slate-200')} />
                    <div>
                      <p className="text-xs font-medium text-slate-800">{ev.label}</p>
                      {ev.date && <p className="text-[11px] text-slate-400">{formatDate(ev.date)}</p>}
                    </div>
                  </div>
                ))}
              </div>
              <button onClick={() => navigate(`/jobs/${selected.jobSlug}`)}
                className="mt-4 flex items-center gap-1.5 text-xs text-primary-600 hover:text-primary-700">
                <ExternalLink className="w-3 h-3" /> View Job Listing
              </button>
            </>
          ) : (
            <div className="text-center py-10 text-slate-400 text-sm">Select an application to view details</div>
          )}
        </div>
      </div>
    </div>
  );
};
