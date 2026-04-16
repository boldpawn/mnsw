export type UserRole = 'SCHEEPSAGENT' | 'LADINGAGENT' | 'HAVENAUTORITEIT' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  portLocode?: string; // Alleen relevant voor HAVENAUTORITEIT
  active?: boolean;
}

export interface AuthResult {
  token: string;
  expiresAt: string; // ISO 8601
  user: User;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  fullName: string;
  role: UserRole;
  portLocode?: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  role?: UserRole;
  portLocode?: string;
  active?: boolean;
}
