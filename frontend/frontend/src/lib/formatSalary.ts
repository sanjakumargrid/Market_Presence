export const formatSalary = (min: number, max: number, currency: string): string => {
  if (currency === 'INR') {
    const fmt = (n: number) => {
      if (n >= 100000) return `₹${(n / 100000).toFixed(1)}L`;
      return `₹${(n / 1000).toFixed(0)}K`;
    };
    return `${fmt(min)} - ${fmt(max)} PA`;
  }
  const fmt = (n: number) => {
    if (n >= 1000) return `$${Math.round(n / 1000)}k`;
    return `$${n}`;
  };
  return `${fmt(min)} — ${fmt(max)}`;
};

export const formatEmploymentType = (type: string): string => {
  const map: Record<string, string> = {
    FULL_TIME: 'Full Time',
    PART_TIME: 'Part Time',
    CONTRACT: 'Contract',
    INTERNSHIP: 'Internship',
    FREELANCE: 'Freelance',
  };
  return map[type] ?? type;
};

export const formatExperience = (level: string): string => {
  const map: Record<string, string> = {
    ENTRY: 'Entry Level',
    JUNIOR: 'Junior',
    MID: 'Mid Level',
    SENIOR: 'Senior',
    LEAD: 'Lead',
    EXECUTIVE: 'Executive',
  };
  return map[level] ?? level;
};

export const formatWorkMode = (mode: string): string => {
  const map: Record<string, string> = {
    REMOTE: 'Remote',
    HYBRID: 'Hybrid',
    ONSITE: 'On-site',
    ON_SITE: 'On-site',
  };
  return map[mode] ?? mode;
};

export const formatLocation = (city: string, state: string, country: string): string => {
  return [city, state, country].filter(Boolean).join(', ');
};
