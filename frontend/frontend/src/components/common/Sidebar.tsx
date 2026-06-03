import { Briefcase, Home, LayoutDashboard, User, X, Settings2, ArrowRightLeft } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { clsx } from 'clsx';

interface SidebarProps {
  id?: string;
  isOpen?: boolean;
  onClose?: () => void;
}

const navItems = [
  { label: 'Career Portal', icon: Home, to: '/' },
  { label: 'Browse Jobs', icon: Briefcase, to: '/jobs' },
  { label: 'My Applications', icon: LayoutDashboard, to: '/applications' },
  { label: 'Profile', icon: User, to: '/profile' },
  { label: 'Admin Portal', icon: Settings2, to: '/admin' },
  { label: 'Handoffs', icon: ArrowRightLeft, to: '/admin/handoffs' },
];

export const Sidebar = ({ id, isOpen = true, onClose }: SidebarProps) => {
  const location = useLocation();

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black/30 z-20 lg:hidden"
          aria-hidden="true"
          onClick={onClose}
        />
      )}

      <aside
        id={id}
        aria-label="Site navigation"
        className={clsx(
          'bg-white border-r border-slate-200 w-52 shrink-0 flex flex-col',
          'lg:static lg:translate-x-0',
          'fixed top-14 left-0 bottom-0 z-30 transition-transform duration-300',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {onClose && (
          <button
            aria-label="Close navigation menu"
            onClick={onClose}
            className="lg:hidden p-3 self-end text-slate-400 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-primary-400 rounded"
          >
            <X aria-hidden="true" className="w-5 h-5" />
          </button>
        )}

        <nav aria-label="Main navigation" className="flex-1 py-4 px-2 space-y-0.5">
          {navItems.map(({ label, icon: Icon, to }) => {
            const active = to === '/' ? location.pathname === '/' : location.pathname.startsWith(to);
            return (
              <Link
                key={to}
                to={to}
                onClick={onClose}
                aria-current={active ? 'page' : undefined}
                className={clsx(
                  'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-primary-400',
                  active
                    ? 'bg-primary-600 text-white'
                    : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                )}
              >
                <Icon aria-hidden="true" className="w-4 h-4 shrink-0" />
                {label}
              </Link>
            );
          })}
        </nav>
      </aside>
    </>
  );
};
