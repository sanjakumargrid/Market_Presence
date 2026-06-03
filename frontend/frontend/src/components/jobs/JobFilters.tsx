import { ChevronDown } from 'lucide-react';
import type { JobFilters } from '../../features/jobs/types/job.types';

interface JobFiltersProps {
  filters: JobFilters;
  onChange: (filters: JobFilters) => void;
  departments: string[];
  locations: string[];
}

const workModes = ['REMOTE', 'HYBRID', 'ONSITE'];
const experienceLevels = ['ENTRY', 'JUNIOR', 'MID', 'SENIOR', 'LEAD', 'EXECUTIVE'];

const labelMap: Record<string, string> = {
  REMOTE: 'Remote', HYBRID: 'Hybrid', ONSITE: 'On-site',
  ENTRY: 'Entry Level', JUNIOR: 'Junior', MID: 'Mid Level',
  SENIOR: 'Senior', LEAD: 'Lead', EXECUTIVE: 'Executive',
  FULL_TIME: 'Full Time', PART_TIME: 'Part Time',
  CONTRACT: 'Contract', INTERNSHIP: 'Internship',
};

interface SelectProps {
  id: string;
  label: string;
  value: string;
  options: string[];
  onChange: (v: string) => void;
}

const FilterSelect = ({ id, label, value, options, onChange }: SelectProps) => (
  <div className="relative">
    {/* Visually hidden label — associates the label text with the control for screen readers */}
    <label htmlFor={id} className="sr-only">{label}</label>
    <select
      id={id}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="appearance-none bg-white border border-slate-200 rounded-lg pl-3 pr-8 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary-300 cursor-pointer"
    >
      <option value="">{label}</option>
      {options.map((o) => (
        <option key={o} value={o}>{labelMap[o] ?? o}</option>
      ))}
    </select>
    <ChevronDown
      aria-hidden="true"
      className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none"
    />
  </div>
);

export const JobFiltersBar = ({ filters, onChange, departments, locations }: JobFiltersProps) => {
  const update = (key: keyof JobFilters, val: string) =>
    onChange({ ...filters, [key]: val || undefined });

  return (
    <div
      role="group"
      aria-label="Filter job listings"
      className="flex flex-wrap gap-2 items-center"
    >
      <FilterSelect
        id="filter-work-mode"
        label="Work Mode"
        value={filters.work_mode ?? ''}
        options={workModes}
        onChange={(v) => update('work_mode', v)}
      />
      <FilterSelect
        id="filter-seniority"
        label="Seniority"
        value={filters.experience_level ?? ''}
        options={experienceLevels}
        onChange={(v) => update('experience_level', v)}
      />
      <FilterSelect
        id="filter-location"
        label="Location"
        value={filters.location_city ?? ''}
        options={locations}
        onChange={(v) => update('location_city', v)}
      />
      <FilterSelect
        id="filter-department"
        label="Department"
        value={filters.department ?? ''}
        options={departments}
        onChange={(v) => update('department', v)}
      />

      <div className="ml-auto relative">
        <label htmlFor="filter-sort" className="sr-only">Sort jobs</label>
        <select
          id="filter-sort"
          value={filters.sort ?? ''}
          onChange={(e) => update('sort', e.target.value)}
          className="appearance-none bg-white border border-slate-200 rounded-lg pl-3 pr-8 py-2 text-sm text-slate-700 focus:outline-none focus:ring-2 focus:ring-primary-300 cursor-pointer"
        >
          <option value="">Sort by</option>
          <option value="latest">Latest</option>
          <option value="salary_high">Highest Salary</option>
          <option value="salary_low">Lowest Salary</option>
          <option value="az">A–Z</option>
        </select>
        <ChevronDown
          aria-hidden="true"
          className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none"
        />
      </div>
    </div>
  );
};
