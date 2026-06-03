import api from './axios';

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  email: string;
  role: string;
  userId: string;
  name: string;
}

export const register = async (req: RegisterRequest): Promise<AuthResponse> => {
  const { data } = await api.post<{ data: AuthResponse }>('/auth/register', req);
  return data.data;
};

export const login = async (req: LoginRequest): Promise<AuthResponse> => {
  const { data } = await api.post<{ data: AuthResponse }>('/auth/login', req);
  return data.data;
};
