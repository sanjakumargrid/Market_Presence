import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { RefreshCw, RotateCcw, CheckCircle, Clock, XCircle, Info } from 'lucide-react';
import {
  adminGetHandoffs,
  adminRetryHandoff,
  adminRetryAllPending,
  type HandoffRecord,
} from '../../api/admin.api';
import { formatDate } from '../../lib/formatDate';

const STATUS_CHIP: Record<string, { cls: string; icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean | 'true' | 'false' }> }> = {
  SENT:    { cls: 'bg-green-100 text-green-700',  icon: CheckCircle },
  PENDING: { cls: 'bg-amber-100 text-amber-700',  icon: Clock },
  FAILED:  { cls: 'bg-red-100   text-red-600',    icon: XCircle },
};

const TABS = ['', 'PENDING', 'SENT', 'FAILED'] as const;

function StatCard({ label, value, cls }: { label: string; value: number; cls: string }) {
  return (
    <div className={`card p-4 flex flex-col gap-1 border-l-4 ${cls}`}>
      <p className="text-2xl font-bold text-slate-900">{value}</p>
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">{label}</p>
    </div>
  );
}

export const AdminHandoffsPage = () => {
  const qc = useQueryClient();
  const [tab, setTab] = useState<string>('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-handoffs', tab],
    queryFn: () => adminGetHandoffs({ status: tab || undefined, size: 100 }),
  });

  // Counts for stat cards — always fetch all statuses for summary
  const { data: allData } = useQuery({
    queryKey: ['admin-handoffs', ''],
    queryFn: () => adminGetHandoffs({ size: 500 }),
  });

  const pendingCount = allData?.content.filter(h => h.status === 'PENDING').length ?? 0;
  const sentCount    = allData?.content.filter(h => h.status === 'SENT').length   ?? 0;
  const failedCount  = allData?.content.filter(h => h.status === 'FAILED').length  ?? 0;

  const retryMut = useMutation({
    mutationFn: (id: number) => adminRetryHandoff(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-handoffs'] }),
  });

  const retryAllMut = useMutation({
    mutationFn: adminRetryAllPending,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-handoffs'] }),
  });

  const records: HandoffRecord[] = data?.content ?? [];

  return (
    <div className="max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display font-bold text-2xl text-slate-900">Team 2 Handoffs</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Application forwarding to Chennai Team 2 (REQ-JP-08)
          </p>
        </div>
        <button
          onClick={() => retryAllMut.mutate()}
          disabled={retryAllMut.isPending || pendingCount === 0}
          className="flex items-center gap-2 btn-primary text-sm px-4 py-2 disabled:opacity-50"
        >
          <RefreshCw aria-hidden="true" className={`w-4 h-4 ${retryAllMut.isPending ? 'animate-spin' : ''}`} />
          Retry All Pending {pendingCount > 0 && `(${pendingCount})`}
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3 mb-6">
        <StatCard label="Pending" value={pendingCount} cls="border-amber-400" />
        <StatCard label="Sent"    value={sentCount}    cls="border-green-500" />
        <StatCard label="Failed"  value={failedCount}  cls="border-red-400"  />
      </div>

      {/* Info box explaining demo stub */}
      <div className="mb-4 flex items-start gap-2 rounded-lg bg-blue-50 border border-blue-200 px-4 py-3 text-sm text-blue-700">
        <Info aria-hidden="true" className="w-4 h-4 mt-0.5 shrink-0" />
        <span>
          Handoffs currently route to the local demo stub at{' '}
          <code className="text-xs bg-blue-100 px-1 rounded">
            /api/stub/team2/applications/intake
          </code>
          . Set <code className="text-xs bg-blue-100 px-1 rounded">app.team2.api-base-url</code> in{' '}
          <code className="text-xs bg-blue-100 px-1 rounded">application.yml</code> to Team 2's real URL.
        </span>
      </div>

      {retryAllMut.isSuccess && (
        <div className="mb-4 text-sm text-green-600 font-medium">
          Bulk retry complete — {retryAllMut.data?.retriedCount ?? 0} record(s) processed.
        </div>
      )}

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
          {[1, 2, 3].map(i => (
            <div key={i} className="h-14 bg-slate-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : records.length === 0 ? (
        <div className="text-center py-16 text-slate-400">No handoff records found.</div>
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                {['Candidate', 'Job', 'Status', 'Team 2 ID', 'Attempted', ''].map(h => (
                  <th key={h} className={`px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wide ${
                    h === 'Team 2 ID' ? 'hidden lg:table-cell' : h === 'Attempted' ? 'hidden md:table-cell' : ''
                  }`}>
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {records.map(h => {
                const chip = STATUS_CHIP[h.status] ?? STATUS_CHIP['PENDING'];
                const Icon = chip.icon;
                const canRetry = h.status === 'PENDING' || h.status === 'FAILED';
                return (
                  <tr key={h.id} className="border-b border-slate-50 last:border-0 hover:bg-slate-50 transition-colors">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-800 text-xs">{h.candidateEmail}</div>
                      <div className="text-[11px] text-slate-400">intake #{h.applicationIntakeId}</div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-800 text-xs line-clamp-1">{h.jobTitle}</div>
                      <div className="text-[11px] text-slate-400">{h.source}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold ${chip.cls}`}>
                        <Icon aria-hidden="true" className="w-3 h-3" />
                        {h.status}
                      </span>
                      {h.errorMessage && (
                        <p className="text-[10px] text-red-500 mt-0.5 max-w-[180px] truncate" title={h.errorMessage}>
                          {h.errorMessage}
                        </p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500 hidden lg:table-cell">
                      {h.team2ResponseId ?? '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-slate-500 hidden md:table-cell">
                      {h.attemptedAt ? formatDate(h.attemptedAt) : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {canRetry && (
                        <button
                          onClick={() => retryMut.mutate(h.id)}
                          disabled={retryMut.isPending}
                          title="Retry this handoff"
                          className="p-1.5 rounded text-slate-400 hover:text-primary-600 hover:bg-primary-50 transition-colors disabled:opacity-40"
                        >
                          <RotateCcw aria-hidden="true" className="w-4 h-4" />
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <p className="text-xs text-slate-400 mt-4 text-right">
        {data?.totalElements ?? 0} total record{data?.totalElements !== 1 ? 's' : ''}
      </p>
    </div>
  );
};
