import { useNavigate, useParams, useSearchParams, Link } from 'react-router-dom';
import { MapPin, Briefcase, Clock, DollarSign, Calendar, ChevronLeft, Share2, Check } from 'lucide-react';
import { useJob } from '../features/jobs/hooks/useJob';
import { formatSalary, formatWorkMode, formatExperience, formatEmploymentType, formatLocation } from '../lib/formatSalary';
import { formatDate } from '../lib/formatDate';
import { useDocumentMeta } from '../lib/useDocumentMeta';
import { AlreadyAppliedModal } from '../components/application/AlreadyAppliedModal';
import { hasApplied } from '../store/localStorage';
import { useState } from 'react';
import type { Job } from '../features/jobs/types/job.types';
import { recordEvent } from '../api/analytics.api';

const CAREERS_BASE = 'http://localhost:5173';

const Section = ({ title, content }: { title: string; content: string }) => (
  <div className="mb-6">
    <h2 className="font-display font-semibold text-lg text-slate-900 mb-3">{title}</h2>
    <div className="text-sm text-slate-600 leading-relaxed whitespace-pre-line">{content}</div>
  </div>
);

function buildJsonLd(job: Job, canonicalUrl: string): Record<string, unknown> {
  const ld: Record<string, unknown> = {
    '@context': 'https://schema.org',
    '@type': 'JobPosting',
    title: job.title,
    description: job.description ?? '',
    datePosted: job.published_at?.split('T')[0] ?? '',
    validThrough: job.expires_at ?? '',
    employmentType: job.employment_type ?? 'FULL_TIME',
    url: canonicalUrl,
    hiringOrganization: {
      '@type': 'Organization',
      name: 'Forge AI',
      sameAs: CAREERS_BASE,
    },
    jobLocation: {
      '@type': 'Place',
      address: {
        '@type': 'PostalAddress',
        addressLocality: job.location_city ?? '',
        addressRegion: job.location_state ?? '',
        addressCountry: job.location_country ?? 'IN',
      },
    },
  };
  if (job.show_salary && job.salary_min && job.salary_max) {
    ld.baseSalary = {
      '@type': 'MonetaryAmount',
      currency: job.currency ?? 'INR',
      value: {
        '@type': 'QuantitativeValue',
        minValue: job.salary_min,
        maxValue: job.salary_max,
        unitText: 'YEAR',
      },
    };
  }
  return ld;
}

export const JobDetailsPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const [searchParams] = useSearchParams();
  const refCode  = searchParams.get('ref') ?? '';
  const channel  = searchParams.get('channel') ?? 'CAREERS_PORTAL';
  const navigate = useNavigate();
  const { data: job, isLoading, isError } = useJob(slug!, channel);
  const [showAlreadyApplied, setShowAlreadyApplied] = useState(false);
  const [copied, setCopied] = useState(false);

  // ── Canonical URL uses /careers/{slug} (REQ-JP-09) ───────────────────────
  const canonicalUrl = `${CAREERS_BASE}/careers/${slug}`;
  const shareUrl = refCode
    ? `${canonicalUrl}?ref=${refCode}`
    : canonicalUrl;

  // ── SEO metadata (REQ-JP-09) ─────────────────────────────────────────────
  const metaDesc = job
    ? (job.meta_description ?? `${job.title} at Forge AI. ${(job.description ?? '').slice(0, 140)}...`)
    : 'Forge AI Careers — open positions';

  useDocumentMeta({
    title: job ? `${job.title} | Forge AI Careers` : 'Forge AI Careers',
    description: metaDesc,
    jsonLd: job ? buildJsonLd(job, canonicalUrl) : undefined,
  });

  const handleApply = () => {
    if (hasApplied(slug!)) {
      setShowAlreadyApplied(true);
    } else {
      // REQ-JP-05: record CLICK before navigating — fire-and-forget
      recordEvent(slug!, 'CLICK', channel);
      navigate(`/careers/${slug}/apply`, { state: { referralCode: refCode || null, channel } });
    }
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(shareUrl).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  if (isLoading) return (
    <div className="max-w-3xl mx-auto">
      <div className="card p-6 animate-pulse space-y-3">
        <div className="h-3 bg-slate-200 rounded w-1/4" />
        <div className="h-7 bg-slate-200 rounded w-2/3" />
        <div className="h-4 bg-slate-200 rounded w-1/3" />
      </div>
    </div>
  );

  if (isError || !job) return (
    <div className="text-center py-20">
      <p className="text-slate-500">Job not found.</p>
      <button onClick={() => navigate('/jobs')} className="btn-secondary mt-4 text-sm">Back to Jobs</button>
    </div>
  );

  const tags = job.requirements?.split(/[,\n]/).filter(Boolean).map((t) => t.trim()).slice(0, 8) ?? [];

  return (
    <div className="max-w-3xl mx-auto">
      {/* Referral banner — shown when candidate arrives via a referral link (REQ-JP-11) */}
      {refCode && (
        <div className="mb-4 rounded-xl bg-amber-50 border border-amber-200 px-4 py-3 flex items-center gap-3 text-sm">
          <span className="text-amber-600 font-medium">Referral link active</span>
          <span className="text-amber-500 text-xs">Code: {refCode} — your application will be tracked as a referral</span>
        </div>
      )}

      <button
        onClick={() => navigate('/jobs')}
        className="flex items-center gap-1.5 text-sm text-slate-500 hover:text-slate-700 mb-4 transition-colors focus:outline-none focus:underline"
        aria-label="Back to all jobs"
      >
        <ChevronLeft aria-hidden="true" className="w-4 h-4" /> Back to Jobs
      </button>

      <div className="card p-6 mb-4">
        <p className="text-xs font-medium text-primary-600 mb-2">{job.department}</p>
        <h1 className="font-display font-bold text-2xl text-slate-900 mb-2">{job.title}</h1>
        <div className="flex items-center gap-1.5 text-sm text-slate-500 mb-4">
          <MapPin aria-hidden="true" className="w-4 h-4" />
          <span>{formatLocation(job.location_city, job.location_state, job.location_country)} ({formatWorkMode(job.work_mode)})</span>
        </div>
        <div className="flex flex-wrap gap-3">
          <button onClick={handleApply} className="btn-primary px-6 py-2.5 text-sm">Apply Now</button>
          {/* Referral share button — REQ-JP-11 */}
          <button
            onClick={handleCopy}
            aria-label="Copy referral link to clipboard"
            className="flex items-center gap-2 btn-secondary px-4 py-2.5 text-sm"
          >
            {copied
              ? <><Check aria-hidden="true" className="w-4 h-4 text-green-600" /> Copied!</>
              : <><Share2 aria-hidden="true" className="w-4 h-4" /> Share / Refer</>}
          </button>
        </div>
      </div>

      {/* Referral link display box — REQ-JP-11 */}
      <div className="card p-4 mb-4 dark:bg-slate-800 dark:border-slate-700">
        <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
          Referral Link
        </p>
        <div className="flex items-center gap-2">
          <code className="flex-1 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded px-3 py-2 text-slate-700 dark:text-slate-300 truncate">
            {canonicalUrl}?ref=YOUR_CODE
          </code>
          <Link
            to={`/jobs/${slug}`}
            className="text-xs text-primary-600 hover:text-primary-700 whitespace-nowrap focus:outline-none focus:underline"
          >
            Generate your code →
          </Link>
        </div>
        <p className="text-xs text-slate-400 mt-2">
          Share your unique referral link. Candidates who apply via your link will be tagged as referrals.
        </p>
      </div>

      {/* Banner */}
      <div className="rounded-xl bg-gradient-to-br from-slate-800 to-slate-900 h-48 flex items-end p-5 mb-6" aria-hidden="true">
        <p className="text-slate-300 text-sm font-medium">Join the team building the future of AI-powered workforce intelligence.</p>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { icon: Briefcase, label: 'Type', val: formatEmploymentType(job.employment_type) },
          { icon: Clock, label: 'Level', val: formatExperience(job.experience_level) },
          { icon: DollarSign, label: 'Salary', val: job.show_salary ? formatSalary(job.salary_min, job.salary_max, job.currency) : 'Not disclosed' },
          { icon: Calendar, label: 'Posted', val: formatDate(job.published_at) },
        ].map(({ icon: Icon, label, val }) => (
          <div key={label} className="card p-3 text-center">
            <Icon aria-hidden="true" className="w-4 h-4 text-primary-600 mx-auto mb-1" />
            <p className="text-[10px] text-slate-400 uppercase tracking-wide mb-0.5">{label}</p>
            <p className="text-xs font-medium text-slate-800">{val}</p>
          </div>
        ))}
      </div>

      <div className="card p-6">
        <Section title="Role Summary" content={job.description} />

        {job.responsibilities && (
          <div className="mb-6">
            <h2 className="font-display font-semibold text-lg text-slate-900 mb-3">Key Responsibilities</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {job.responsibilities.split('\n').filter(Boolean).slice(0, 4).map((r, i) => (
                <div key={i} className="border border-slate-200 rounded-xl p-4">
                  <p className="text-sm text-slate-700 font-medium mb-1">Point {i + 1}</p>
                  <p className="text-xs text-slate-500 leading-relaxed">{r.replace(/^[-•*]\s*/, '')}</p>
                </div>
              ))}
            </div>
          </div>
        )}

        {tags.length > 0 && (
          <div className="mb-6">
            <h2 className="font-display font-semibold text-lg text-slate-900 mb-3">Technical Requirements</h2>
            <div className="flex flex-wrap gap-2">
              {tags.map((tag) => (
                <span key={tag} className="text-[10px] font-semibold tracking-wider text-slate-600 bg-slate-100 px-3 py-1.5 rounded uppercase">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        )}

        {job.benefits && <Section title="Benefits" content={job.benefits} />}

        <div className="border-t border-slate-100 pt-4 flex items-center justify-between text-xs text-slate-400">
          <span>Posted: {formatDate(job.published_at)}</span>
          <span>Expires: {formatDate(job.expires_at)}</span>
        </div>
      </div>

      {/* Canonical URL for SEO (hidden, included in JSON-LD and meta link) */}
      <link rel="canonical" href={canonicalUrl} />

      <div className="mt-4 text-center">
        <button onClick={handleApply} className="btn-primary px-10 py-3">Apply for this Role</button>
      </div>

      {showAlreadyApplied && (
        <AlreadyAppliedModal jobTitle={job.title} onClose={() => setShowAlreadyApplied(false)} />
      )}
    </div>
  );
};
