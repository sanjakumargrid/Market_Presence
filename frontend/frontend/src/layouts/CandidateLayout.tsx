import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Navbar } from '../components/common/Navbar';
import { Sidebar } from '../components/common/Sidebar';
import { Menu } from 'lucide-react';

export const CandidateLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col bg-slate-50">
      {/* Skip navigation — first focusable element on the page (WCAG 2.4.1) */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-50 focus:bg-white focus:text-primary-700 focus:font-medium focus:text-sm focus:px-4 focus:py-2 focus:rounded-lg focus:shadow-lg focus:outline-none focus:ring-2 focus:ring-primary-400"
      >
        Skip to main content
      </a>

      <Navbar />

      <div className="flex flex-1 overflow-hidden">
        {/* Mobile hamburger */}
        <button
          aria-label="Open navigation menu"
          aria-expanded={sidebarOpen}
          aria-controls="sidebar-nav"
          className="lg:hidden fixed bottom-4 left-4 z-40 bg-primary-600 text-white p-3 rounded-full shadow-lg focus:outline-none focus:ring-2 focus:ring-primary-400 focus:ring-offset-2"
          onClick={() => setSidebarOpen(true)}
        >
          <Menu aria-hidden="true" className="w-5 h-5" />
        </button>

        <Sidebar id="sidebar-nav" isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

        <main
          id="main-content"
          tabIndex={-1}
          className="flex-1 overflow-y-auto p-6 lg:p-8 focus:outline-none"
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
};
