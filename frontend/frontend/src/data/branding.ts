/**
 * Static employer branding configuration (REQ-JP-10).
 *
 * This file is the single source of truth for all employer branding content
 * shown on the careers portal: About Us, culture values, benefits, and
 * employee story cards.
 *
 * For a production deployment, replace this static export with an API call
 * (GET /api/admin/branding) that admin/HR users can edit through a CMS.
 * The component interfaces are already typed — swap the import without
 * changing any JSX.
 */

export interface CultureValue {
  title: string;
  description: string;
  icon: 'zap' | 'users' | 'trending-up' | 'shield' | 'globe';
  bg: string;
}

export interface Benefit {
  title: string;
  description: string;
}

export interface EmployeeStory {
  name: string;
  role: string;
  location: string;
  quote: string;
  initials: string;
  color: string;
}

export interface BrandingConfig {
  companyName: string;
  tagline: string;
  missionStatement: string;
  aboutUs: string;
  cultureValues: CultureValue[];
  benefits: Benefit[];
  employeeStories: EmployeeStory[];
}

export const branding: BrandingConfig = {
  companyName: 'Forge AI',
  tagline: 'Engineering the talent supply chain — intelligently.',

  missionStatement:
    'We exist to empower the world\'s most complex workforces through ' +
    'automated intelligence and decisive engineering systems.',

  aboutUs:
    'Forge AI is Grid Dynamics\' internal workforce intelligence platform — ' +
    'built by 30 engineers across Chennai, Bangalore, and Hyderabad to the same ' +
    'standards we deliver to Fortune 500 clients. We are AI-native from the ground ' +
    'up: event-driven, semantically matched, and LLM-augmented across every workflow.',

  cultureValues: [
    {
      title: 'Engineering First',
      description:
        'Shipping 10× faster by treating every workflow as a first-class engineering ' +
        'problem. We apply production standards to internal tools.',
      icon: 'zap',
      bg: 'bg-primary-50',
    },
    {
      title: 'Radical Transparency',
      description:
        'All decisions are documented in ADRs within 24 hours. No decision is made ' +
        'in a meeting without a follow-up written summary.',
      icon: 'shield',
      bg: 'bg-amber-50',
    },
    {
      title: 'Global Perspective',
      description:
        'A team spread across three cities with engineers from 20+ countries. We ' +
        'value cognitive diversity above all else.',
      icon: 'globe',
      bg: 'bg-emerald-50',
    },
    {
      title: 'Peer-Led Growth',
      description:
        'All 30 engineers are peers. Mentors hold the authority layer, not the hierarchy. ' +
        'Your trajectory is data-driven and yours to own.',
      icon: 'trending-up',
      bg: 'bg-violet-50',
    },
  ],

  benefits: [
    { title: 'Competitive Compensation', description: 'Market-rate salaries benchmarked quarterly + performance bonus.' },
    { title: 'Learning Budget', description: '₹50,000 / $2,000 annual per engineer for courses, certifications, and conferences.' },
    { title: 'Flexible Work', description: 'Hybrid-first policy. Remote roles available for select positions.' },
    { title: 'Health & Wellness', description: 'Full health, dental, and vision cover for you and your family.' },
    { title: 'Equity Participation', description: 'Stock options for senior and lead roles.' },
    { title: 'Paid Certifications', description: 'AWS, GCP, CKA, and Grid Dynamics internal certifications fully sponsored.' },
  ],

  employeeStories: [
    {
      name: 'Ananya Krishnan',
      role: 'Senior Backend Engineer',
      location: 'Bangalore',
      quote:
        'Forge is the first place I\'ve worked where the internal tooling is held to the same ' +
        'engineering bar as client projects. The code review culture here made me a better engineer in six months.',
      initials: 'AK',
      color: 'bg-primary-100 text-primary-700',
    },
    {
      name: 'Rajan Mehta',
      role: 'AI/ML Engineer',
      location: 'Chennai',
      quote:
        'Building RAG pipelines and pgvector semantic search as internal infrastructure — ' +
        'not as a consulting deliverable — changes how you think about AI systems entirely.',
      initials: 'RM',
      color: 'bg-emerald-100 text-emerald-700',
    },
    {
      name: 'Divya Prasad',
      role: 'DevOps Engineer',
      location: 'Hyderabad',
      quote:
        'The observability-first culture is real. By Week 2 we had OTel traces, Loki logs, ' +
        'and Grafana dashboards live — not as an afterthought but as a requirement.',
      initials: 'DP',
      color: 'bg-violet-100 text-violet-700',
    },
  ],
};
