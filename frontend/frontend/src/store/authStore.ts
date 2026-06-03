import { create } from 'zustand';

interface AuthUser {
  userId: string;
  email: string;
  name: string;
  role: string;
  token: string;
}

interface AuthState {
  user: AuthUser | null;
  isAuthenticated: boolean;
  setUser: (user: AuthUser) => void;
  logout: () => void;
  loadFromStorage: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,

  setUser: (user) => {
    localStorage.setItem('forge_token', user.token);
    localStorage.setItem('forge_user', JSON.stringify(user));
    set({ user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('forge_token');
    localStorage.removeItem('forge_user');
    set({ user: null, isAuthenticated: false });
  },

  loadFromStorage: () => {
    try {
      const stored = localStorage.getItem('forge_user');
      const token = localStorage.getItem('forge_token');
      if (stored && token) {
        const user = JSON.parse(stored) as AuthUser;
        set({ user, isAuthenticated: true });
      }
    } catch {
      localStorage.removeItem('forge_user');
      localStorage.removeItem('forge_token');
    }
  },
}));
