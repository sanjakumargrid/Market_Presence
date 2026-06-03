import { Bell, HelpCircle, Moon, Sun, Search, LogIn } from 'lucide-react';
import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

export const Navbar = () => {
  const [search, setSearch] = useState('');
  const [isDark, setIsDark] = useState(false);
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (
      localStorage.getItem('theme') === 'dark' ||
      (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches)
    ) {
      setIsDark(true);
      document.documentElement.classList.add('dark');
    } else {
      setIsDark(false);
      document.documentElement.classList.remove('dark');
    }
  }, []);

  const toggleDarkMode = () => {
    if (isDark) {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
      setIsDark(false);
    } else {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
      setIsDark(true);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (search.trim()) navigate(`/jobs?q=${encodeURIComponent(search)}`);
  };

  const initials = user?.name
    ? user.name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2)
    : user?.email?.[0]?.toUpperCase() ?? 'U';

  return (
    <header className="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 h-14 flex items-center px-4 gap-4 sticky top-0 z-30 transition-colors">
      {/* Logo — home link */}
      <Link
        to="/"
        className="font-display font-bold text-xl text-primary-700 dark:text-primary-500 mr-2 shrink-0 focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
        aria-label="Forge AI — go to home"
      >
        FORGE
      </Link>

      {/* Global search */}
      <form
        role="search"
        onSubmit={handleSearch}
        className="flex-1 max-w-md relative"
        aria-label="Search all roles"
      >
        <label htmlFor="navbar-search" className="sr-only">Search roles</label>
        <Search aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 dark:text-slate-500" />
        <input
          id="navbar-search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search roles..."
          className="w-full pl-9 pr-4 py-1.5 text-sm bg-slate-100 dark:bg-slate-800 rounded-lg border-0 focus:outline-none focus:ring-2 focus:ring-primary-300 dark:focus:ring-primary-600 dark:text-white dark:placeholder-slate-400 transition-colors"
        />
      </form>

      <div className="ml-auto flex items-center gap-3 text-slate-500 dark:text-slate-400">
        <button
          aria-label="Help and support"
          className="hover:text-slate-700 dark:hover:text-slate-300 transition-colors hidden sm:block focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
        >
          <HelpCircle aria-hidden="true" className="w-5 h-5" />
        </button>

        <button
          aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
          aria-pressed={isDark}
          onClick={toggleDarkMode}
          className="hover:text-slate-700 dark:hover:text-slate-300 transition-colors hidden sm:block focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
        >
          {isDark
            ? <Sun aria-hidden="true" className="w-5 h-5" />
            : <Moon aria-hidden="true" className="w-5 h-5" />}
        </button>

        <button
          aria-label="Notifications"
          className="hover:text-slate-700 dark:hover:text-slate-300 transition-colors hidden sm:block focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
        >
          <Bell aria-hidden="true" className="w-5 h-5" />
        </button>

        {isAuthenticated ? (
          <button
            aria-label={`View profile for ${user?.name ?? user?.email ?? 'user'}`}
            title={user?.email}
            onClick={() => navigate('/profile')}
            className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900 flex items-center justify-center text-primary-700 dark:text-primary-300 font-semibold text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
          >
            {initials}
          </button>
        ) : (
          <button
            onClick={() => navigate('/login')}
            className="flex items-center gap-1.5 text-sm text-primary-600 dark:text-primary-500 hover:text-primary-700 dark:hover:text-primary-400 font-medium focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
            aria-label="Sign in to your account"
          >
            <LogIn aria-hidden="true" className="w-4 h-4" /> Sign In
          </button>
        )}
      </div>
    </header>
  );
};
