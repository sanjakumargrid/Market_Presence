export const JobCardSkeleton = () => (
  <div className="card p-5 animate-pulse">
    <div className="flex items-start justify-between mb-3">
      <div className="w-10 h-10 rounded-lg bg-slate-200" />
      <div className="w-16 h-5 rounded-full bg-slate-200" />
    </div>
    <div className="h-4 bg-slate-200 rounded w-3/4 mb-2" />
    <div className="h-3 bg-slate-200 rounded w-1/2 mb-4" />
    <div className="space-y-2 mb-4">
      <div className="h-3 bg-slate-200 rounded w-2/3" />
      <div className="h-3 bg-slate-200 rounded w-1/2" />
    </div>
    <div className="h-9 bg-slate-200 rounded-lg" />
  </div>
);

export const PageSkeleton = () => (
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
    {Array.from({ length: 6 }).map((_, i) => <JobCardSkeleton key={i} />)}
  </div>
);
