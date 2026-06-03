import { Search, Zap, Users, TrendingUp, ChevronRight, Mail } from 'lucide-react';
import { branding } from '../data/branding';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useJobs } from '../features/jobs/hooks/useJobs';
import { JobCard } from '../components/jobs/JobCard';
import { JobCardSkeleton } from '../components/common/Skeleton';

const CultureCard = ({ icon, title, desc, bg }: { icon: React.ReactNode; title: string; desc: string; bg: string }) => (
  <div className={`rounded-2xl p-6 ${bg} dark:bg-slate-800 transition-colors`}>
    <div className="w-10 h-10 rounded-lg bg-white/80 dark:bg-slate-700 flex items-center justify-center mb-4 transition-colors">{icon}</div>
    <h3 className="font-semibold text-slate-900 dark:text-slate-100 mb-2 transition-colors">{title}</h3>
    <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed transition-colors">{desc}</p>
  </div>
);

export const HomePage = () => {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [email, setEmail] = useState('');
  const { data: jobs, isLoading } = useJobs();
  const featured = jobs?.filter((j) => j.posting_status === 'PUBLISHED').slice(0, 6) ?? [];

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    navigate(`/jobs${search ? `?q=${encodeURIComponent(search)}` : ''}`);
  };

  const lifeImages = [
    { src: "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80", alt: "Forge AI team collaborating around a table" },
    { src: "https://images.unsplash.com/photo-1600880292203-757bb62b4baf?w=800&q=80", alt: "Engineers working together at standing desks" },
    { src: "https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&q=80", alt: "Team member presenting at a whiteboard" },
  ];

  return (
    <div className="max-w-5xl mx-auto space-y-12">
      {/* Hero */}
      <div className="relative rounded-2xl overflow-hidden bg-gradient-to-br from-slate-900 via-slate-800 to-primary-900 min-h-[280px] flex items-end p-8">
        <div className="absolute inset-0 opacity-20"
          style={{ backgroundImage: 'radial-gradient(circle at 20% 50%, #3b82f6 0%, transparent 50%), radial-gradient(circle at 80% 20%, #1e40af 0%, transparent 50%)' }}
        />
        <div className="relative z-10 max-w-lg">
          <div className="inline-flex items-center gap-2 bg-white/10 backdrop-blur-sm border border-white/20 rounded-full px-3 py-1 mb-4">
            <Zap aria-hidden="true" className="w-3.5 h-3.5 text-blue-300" />
            <span className="text-blue-200 text-xs font-medium tracking-wide">AI-NATIVE CAREERS</span>
          </div>
          <h1 className="font-display font-bold text-3xl md:text-4xl text-white mb-3 leading-tight">
            Build the Future with Forge AI
          </h1>
          <p className="text-slate-300 text-sm leading-relaxed mb-6 max-w-sm">
            Join a team of elite engineers and designers redefining workforce intelligence through clinical precision and cutting-edge generative AI.
          </p>
          <div className="flex gap-3">
            <button onClick={() => navigate('/jobs')} className="btn-primary px-5 py-2.5 text-sm">
              Explore Jobs
            </button>
            <button className="bg-white/10 hover:bg-white/20 text-white border border-white/30 font-medium px-5 py-2.5 rounded-lg text-sm transition-colors">
              Learn About Us
            </button>
          </div>
        </div>
      </div>

      {/* Search */}
      <form onSubmit={handleSearch} className="card p-4 flex gap-3 items-center dark:bg-slate-800 dark:border-slate-700 transition-colors">
        <Search aria-hidden="true" className="w-5 h-5 text-slate-400 shrink-0 ml-1" />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by role, skill, or keyword..."
          className="flex-1 text-sm bg-transparent border-0 focus:outline-none text-slate-800 dark:text-slate-100 placeholder-slate-400 transition-colors"
        />
        <button type="submit" className="btn-primary px-5 py-2 text-sm shrink-0">Search</button>
      </form>

      {/* About Us — REQ-JP-10 */}
      <section aria-label="About Forge AI">
        <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-2 transition-colors">ABOUT US</p>
        <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 mb-3 transition-colors">{branding.companyName}</h2>
        <p className="text-slate-600 dark:text-slate-400 text-sm leading-relaxed mb-6 max-w-2xl">{branding.aboutUs}</p>
      </section>

      {/* Culture Values — REQ-JP-10 */}
      <section aria-label="Culture values">
        <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-2 transition-colors">OUR DNA</p>
        <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 mb-6 transition-colors">The Forge Culture</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="relative rounded-2xl overflow-hidden row-span-2 min-h-[240px] bg-slate-800 flex items-end p-6"
            style={{ background: 'linear-gradient(135deg, #1e293b, #0f172a)' }}>
            <div className="absolute inset-0 opacity-30"
              style={{ backgroundImage: 'linear-gradient(to bottom, transparent 40%, rgba(0,0,0,0.8))' }} />
            <div className="relative z-10">
              <h3 className="text-white font-semibold mb-1">Our Mission</h3>
              <p className="text-slate-400 text-sm">{branding.missionStatement}</p>
            </div>
          </div>
          <CultureCard icon={<Zap aria-hidden="true" className="w-5 h-5 text-primary-600 dark:text-primary-400" />} title={branding.cultureValues[0].title} desc={branding.cultureValues[0].description} bg="bg-primary-50" />
          <CultureCard icon={<Users aria-hidden="true" className="w-5 h-5 text-slate-600 dark:text-slate-400" />} title={branding.cultureValues[2].title} desc={branding.cultureValues[2].description} bg="bg-slate-50" />
        </div>
        <div className="mt-4 rounded-2xl bg-primary-600 dark:bg-primary-700 p-6 flex items-center justify-between transition-colors">
          <div>
            <h3 className="text-white font-semibold text-lg mb-1">{branding.cultureValues[3].title}</h3>
            <p className="text-primary-100 text-sm max-w-md">{branding.cultureValues[3].description}</p>
          </div>
          <TrendingUp aria-hidden="true" className="w-10 h-10 text-primary-200 shrink-0 ml-4" />
        </div>
      </section>

      {/* Benefits — REQ-JP-10 */}
      <section aria-label="Benefits and perks">
        <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-2 transition-colors">PERKS & BENEFITS</p>
        <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 mb-6 transition-colors">Why You'll Love It Here</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {branding.benefits.map((b) => (
            <div key={b.title} className="card p-4 dark:bg-slate-800 dark:border-slate-700 transition-colors">
              <p className="font-semibold text-slate-800 dark:text-slate-100 text-sm mb-1 transition-colors">{b.title}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed transition-colors">{b.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Employee Story Cards — REQ-JP-10 */}
      <section aria-label="Employee stories">
        <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-2 transition-colors">TEAM VOICES</p>
        <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 mb-6 transition-colors">Heard from the Team</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {branding.employeeStories.map((story) => (
            <blockquote key={story.name} className="card p-5 flex flex-col dark:bg-slate-800 dark:border-slate-700 transition-colors">
              <p className="text-sm text-slate-600 dark:text-slate-300 leading-relaxed mb-4 flex-1 transition-colors">
                "{story.quote}"
              </p>
              <footer className="flex items-center gap-3">
                <div className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${story.color}`}>
                  {story.initials}
                </div>
                <div>
                  <p className="text-sm font-semibold text-slate-800 dark:text-slate-100 transition-colors">{story.name}</p>
                  <p className="text-xs text-slate-400 dark:text-slate-500 transition-colors">{story.role} · {story.location}</p>
                </div>
              </footer>
            </blockquote>
          ))}
        </div>
      </section>

      {/* Life at Forge */}
      <div>
        <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-2 transition-colors">CULTURE & SPACES</p>
        <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 mb-4 transition-colors">Life at FORGE</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {lifeImages.map(({ src, alt }, i) => (
            <div key={i} className="rounded-xl h-36 flex items-end overflow-hidden group">
              <img src={src} alt={alt} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
            </div>
          ))}
        </div>
      </div>

      {/* Featured Jobs */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="text-xs font-semibold text-primary-600 dark:text-primary-400 uppercase tracking-widest mb-1 transition-colors">OPEN POSITIONS</p>
            <h2 className="font-display font-bold text-2xl text-slate-900 dark:text-slate-100 transition-colors">Featured Opportunities</h2>
          </div>
          <Link to="/jobs" className="flex items-center gap-1 text-sm text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium transition-colors">
            View all {jobs?.length ?? ''} roles <ChevronRight aria-hidden="true" className="w-4 h-4" />
          </Link>
        </div>
        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {Array.from({ length: 6 }).map((_, i) => <JobCardSkeleton key={i} />)}
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {featured.map((job) => <JobCard key={job.id} job={job} featured />)}
          </div>
        )}
      </div>

      {/* CTA */}
      <div className="card p-8 text-center dark:bg-slate-800 dark:border-slate-700 transition-colors">
        <h2 className="font-display font-bold text-xl text-slate-900 dark:text-slate-100 mb-2 transition-colors">Don't see your fit?</h2>
        <p className="text-slate-500 dark:text-slate-400 text-sm mb-6 max-w-sm mx-auto transition-colors">Join our talent community to get notified about new openings that match your skills as soon as they launch.</p>
        <div className="flex gap-3 max-w-sm mx-auto">
          <div className="flex-1 relative">
            <Mail aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 dark:text-slate-500" />
            <label htmlFor="talent-community-email" className="sr-only">Email address for talent community</label>
            <input
              id="talent-community-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email"
              className="w-full pl-9 pr-3 py-2.5 text-sm border border-slate-200 dark:border-slate-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-300 dark:bg-slate-700 dark:text-white transition-colors"
            />
          </div>
          <button className="btn-primary px-4 py-2.5 text-sm whitespace-nowrap">Join Community</button>
        </div>
      </div>
    </div>
  );
};
