/**
 * Authentication utility functions
 * Handles localStorage-based auth state management
 */

export const isLoggedIn = (): boolean => {
  return localStorage.getItem('auth_username') !== null;
};

export const getAuthHeader = (): string | null => {
  const username = localStorage.getItem('auth_username');
  const password = localStorage.getItem('auth_password');

  if (username && password) {
    const credentials = btoa(`${username}:${password}`);
    return `Basic ${credentials}`;
  }
  return null;
};

export const login = (username: string, password: string, roles: string[]): void => {
  localStorage.setItem('auth_username', username);
  localStorage.setItem('auth_password', password);
  localStorage.setItem('auth_roles', JSON.stringify(roles));
};

export const logout = (): void => {
  localStorage.removeItem('auth_username');
  localStorage.removeItem('auth_password');
  localStorage.removeItem('auth_roles');
};

export const getUsername = (): string | null => {
  return localStorage.getItem('auth_username');
};

export const getRoles = (): string[] => {
  const roles = localStorage.getItem('auth_roles');
  return roles ? JSON.parse(roles) : [];
};

