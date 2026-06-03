import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Globe, BriefcaseBusiness, FileText, ExternalLink, Copy, Check, Info } from 'lucide-react';
import { useState } from 'react';
import {
  adminGetChannels,
  adminPublishChannel,
  adminUnpublishChannel,
  type AdminChannel,
} from '../../api/admin.api';

const CHANNEL_META: Record<
  string,
  { label: string; icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean | 'true' | 'false' }>; description: string }
> = {
  CAREERS_PORTAL: {
    label: 'Careers Portal',
    icon: Globe,
    description: 'Your public-facing careers site (/careers/:slug)',
  },
  LINKEDIN: {
    label: 'LinkedIn',
    icon: BriefcaseBusiness,
    description: 'LinkedIn Jobs — manual post until API is configured',
  },
  INDEED: {
    label: 'Indeed',
    icon: FileText,
    description: 'Indeed XML feed — submit feed URL to Indeed Job Distributor',
  },
};

const STATUS_CHIP: Record<string, string> = {
  DRAFT: 'bg-slate-100 text-slate-500',
  PENDING: 'bg-amber-100 text-amber-700',
  LIVE: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-600',
  UNPUBLISHED: 'bg-slate-100 text-slate-400',
};

const ALL_CHANNELS = ['CAREERS_PORTAL', 'LINKEDIN', 'INDEED'];

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  const handle = () => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };
  return (
    <button
      type="button"
      onClick={handle}
      className="inline-flex items-center gap-1 text-[11px] text-slate-500 hover:text-slate-700 transition-colors focus:outline-none focus:underline"
      title="Copy to clipboard"
    >
      {copied ? (
        <><Check aria-hidden="true" className="w-3 h-3 text-green-600" /> Copied</>
      ) : (
        <><Copy aria-hidden="true" className="w-3 h-3" /> Copy</>
      )}
    </button>
  );
}

interface ChannelCardProps {
  channelName: string;
  data?: AdminChannel;
  jobId: number;
}

function ChannelCard({ channelName, data, jobId }: ChannelCardProps) {
  const qc = useQueryClient();
  const meta = CHANNEL_META[channelName];
  const Icon = meta.icon;
  const status = data?.status ?? 'DRAFT';
  const isLive = status === 'LIVE';

  const invalidate = () => qc.invalidateQueries({ queryKey: ['admin-channels', jobId] });

  const publishMut = useMutation({
    mutationFn: () => adminPublishChannel(jobId, channelName),
    onSuccess: invalidate,
  });

  const unpublishMut = useMutation({
    mutationFn: () => adminUnpublishChannel(jobId, channelName),
    onSuccess: invalidate,
  });

  const busy = publishMut.isPending || unpublishMut.isPending;

  return (
    <div className="card p-4 flex flex-col gap-3">
      {/* Channel identity */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center shrink-0">
            <Icon aria-hidden="true" className="w-4 h-4 text-slate-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-800">{meta.label}</p>
            <p className="text-[11px] text-slate-400 leading-tight">{meta.description}</p>
          </div>
        </div>
        <span
          className={`shrink-0 text-[11px] font-semibold px-2 py-0.5 rounded ${STATUS_CHIP[status] ?? STATUS_CHIP['DRAFT']}`}
        >
          {status}
        </span>
      </div>

      {/* Channel URL — shown when live */}
      {isLive && data?.channelUrl && (
        <a
          href={data.channelUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center gap-1 text-[11px] text-primary-600 hover:underline truncate focus:outline-none focus:underline"
        >
          <ExternalLink aria-hidden="true" className="w-3 h-3 shrink-0" />
          <span className="truncate">{data.channelUrl}</span>
        </a>
      )}

      {/* Instruction / info message (PENDING for LinkedIn, feed info for Indeed) */}
      {data?.errorMessage && (
        <div className="rounded-lg bg-amber-50 border border-amber-100 p-2.5 flex flex-col gap-1.5">
          <div className="flex items-start gap-1.5">
            <Info aria-hidden="true" className="w-3.5 h-3.5 text-amber-500 mt-0.5 shrink-0" />
            <p className="text-[11px] text-amber-800 leading-relaxed">{data.errorMessage}</p>
          </div>
          {data.channelUrl && <CopyButton text={data.channelUrl} />}
        </div>
      )}

      {/* Error from mutation */}
      {(publishMut.isError || unpublishMut.isError) && (
        <p className="text-[11px] text-red-500">Action failed — check backend is running.</p>
      )}

      {/* Action button */}
      <div className="mt-auto">
        {isLive ? (
          <button
            type="button"
            disabled={busy}
            onClick={() => unpublishMut.mutate()}
            className="text-xs px-3 py-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 transition-colors disabled:opacity-50 focus:outline-none focus:ring-1 focus:ring-slate-300"
          >
            Unpublish
          </button>
        ) : (
          <button
            type="button"
            disabled={busy}
            onClick={() => publishMut.mutate()}
            className="text-xs px-3 py-1.5 rounded-lg bg-primary-600 text-white hover:bg-primary-700 transition-colors disabled:opacity-50 focus:outline-none focus:ring-1 focus:ring-primary-400"
          >
            {status === 'PENDING' ? 'Retry Publish' : 'Publish'}
          </button>
        )}
      </div>
    </div>
  );
}

interface Props {
  jobId: number;
}

export const ChannelPanel = ({ jobId }: Props) => {
  const { data: channels = [], isLoading } = useQuery({
    queryKey: ['admin-channels', jobId],
    queryFn: () => adminGetChannels(jobId),
  });

  const byName = Object.fromEntries(channels.map((c) => [c.channelName, c]));

  return (
    <section aria-labelledby="channels-heading" className="mt-10">
      <h2
        id="channels-heading"
        className="font-semibold text-slate-800 text-sm uppercase tracking-wide mb-3 pb-1 border-b border-slate-100"
      >
        Distribution Channels (REQ-JP-03)
      </h2>
      <p className="text-xs text-slate-500 mb-4">
        Publish this posting to individual channels. LinkedIn requires manual copy-paste until the
        API credential is configured; Indeed publishes automatically via the XML feed.
      </p>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-36 bg-slate-100 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {ALL_CHANNELS.map((name) => (
            <ChannelCard
              key={name}
              channelName={name}
              data={byName[name]}
              jobId={jobId}
            />
          ))}
        </div>
      )}
    </section>
  );
};
