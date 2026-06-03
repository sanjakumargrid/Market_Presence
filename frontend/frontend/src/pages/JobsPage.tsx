import { Search, ChevronDown, Zap } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useJobs } from '../features/jobs/hooks/useJobs';
import { filterJobs } from '../api/jobs.api';
import type { JobFilters } from '../features/jobs/types/job.types';
import { JobCard } from '../components/jobs/JobCard';
import { JobFiltersBar } from '../components/jobs/JobFilters';
import { PageSkeleton } from '../components/common/Skeleton';

const PAGE_SIZE = 12;

export const JobsPage = () => {
  const [searchParams] = useSearchParams();
  const [searchText, setSearchText] = useState(searchParams.get('q') ?? '');
  const [filters, setFilters] = useState<JobFilters>({ search: searchParams.get('q') ?? '' });
  const [page, setPage] = useState(1);
  const { data: allJobs, isLoading, isError } = useJobs();

  useEffect(() => {
    const q = searchParams.get('q') ?? '';
    setSearchText(q);
    setFilters((f) => ({ ...f, search: q }));
  }, [searchParams]);

  const departments = useMemo(
    () => [...new Set(allJobs?.map((j) => j.department).filter(Boolean) ?? [])].sort(),
    [allJobs]
  );

  const locations = useMemo(
    () => [...new Set(allJobs?.map((j) => j.location_city).filter(Boolean) ?? [])].sort(),
    [allJobs]
  );

  const filtered = useMemo(() => {
    if (!allJobs) return [];
    return filterJobs(allJobs.filter((j) => j.posting_status === 'PUBLISHED'), filters);
  }, [allJobs, filters]);

  const paginated = filtered.slice(0, page * PAGE_SIZE);
  const hasMore = paginated.length < filtered.length;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((f) => ({ ...f, search: searchText }));
    setPage(1);
  };

  return (
    <div className="max-w-5xl mx-auto">

      {/* Culture callout — REQ-JP-06 company culture section */}
      <section aria-label="About Forge AI" className="mb-6">
        <div className="rounded-xl bg-gradient-to-r from-primary-700 to-primary-900 px-5 py-4 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <Zap aria-hidden="true" className="w-5 h-5 text-primary-300 shrink-0" />
            <div className="min-w-0">
              <p className="text-white font-semibold text-sm">Building the Future of Workforce Intelligence</p>
              <p className="text-primary-200 text-xs mt-0.5 leading-relaxed">
                Join a team of elite engineers reimagining how AI meets HR — across Chennai, Bangalore, and Hyderabad.
              </p>
            </div>
          </div>
          <Link
            to="/"
            className="text-white text-xs font-medium underline underline-offset-2 shrink-0 hover:text-primary-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-800 rounded"
          >
            Our culture
          </Link>
        </div>
      </section>

      {/* Search bar */}
      <form
        role="search"
        onSubmit={handleSearch}
        className="flex gap-2 mb-4"
        aria-label="Search jobs"
      >
        <div className="flex-1 relative">
          <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
          <label htmlFor="jobs-search" className="sr-only">Search by job title, skill, or keyword</label>
          <input
            id="jobs-search"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            placeholder="Search by job title, skill, or keyword..."
            className="w-full pl-9 pr-3 py-2.5 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-300"
          />
        </div>
        <button type="submit" className="btn-primary px-5 py-2.5 text-sm">Search</button>
      </form>

      {/* Filters */}
      <div className="mb-6">
        <JobFiltersBar
          filters={filters}
          onChange={(f) => { setFilters(f); setPage(1); }}
          departments={departments}
          locations={locations}
        />
      </div>

      {/* Screen-reader live region — announces result count on filter change */}
      <p aria-live="polite" aria-atomic="true" className="sr-only">
        {!isLoading && !isError
          ? `${filtered.length} job${filtered.length !== 1 ? 's' : ''} found`
          : ''}
      </p>

      {/* Results */}
      {isLoading ? (
        <PageSkeleton />
      ) : isError ? (
        <div role="alert" className="text-center py-20">
          <p className="text-slate-500 text-lg font-medium">Unable to load jobs</p>
          <p className="text-slate-400 text-sm mt-1">Please check your connection and try again.</p>
        </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-20">
          <p className="text-slate-500 text-lg font-medium">No jobs match your filters</p>
          <p className="text-slate-400 text-sm mt-1">Try adjusting your search term or clearing a filter.</p>
        </div>
      ) : (
        <>
          <div
            className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6"
            aria-label={`${filtered.length} open positions`}
          >
            {paginated.map((job) => <JobCard key={job.id} job={job} />)}
          </div>
          <div className="text-center">
            <p className="text-sm text-slate-500 mb-3" aria-live="polite" aria-atomic="true">
              Showing {paginated.length} of {filtered.length} open opportunities
            </p>
            {hasMore && (
              <button
                onClick={() => setPage((p) => p + 1)}
                className="btn-secondary px-6 py-2.5 text-sm inline-flex items-center gap-2"
                aria-label={`Load more jobs — showing ${paginated.length} of ${filtered.length}`}
              >
                Load More Jobs <ChevronDown aria-hidden="true" className="w-4 h-4" />
              </button>
            )}
          </div>
        </>
      )}
    </div>
  );
};
