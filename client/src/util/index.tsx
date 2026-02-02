import { IHttpMethod } from '../types';
import { getAuthHeader } from './auth';

declare var __API_SERVER_URL__;
const BACKEND_URL = (typeof __API_SERVER_URL__ === 'undefined' ? 'http://localhost:9966/petclinic' : __API_SERVER_URL__);

export const url = (path: string): string => `${BACKEND_URL}/${path}`;

/**
 * Creates headers object with optional Authorization header if user is logged in
 */
const createHeaders = (additionalHeaders: Record<string, string> = {}): Record<string, string> => {
  const headers: Record<string, string> = {
    'Accept': 'application/json',
    ...additionalHeaders
  };

  const authHeader = getAuthHeader();
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  return headers;
};

/**
 * Fetch wrapper that automatically includes Authorization header if user is logged in
 */
export const fetchWithAuth = (requestUrl: string, options: RequestInit = {}): Promise<Response> => {
  const headers = createHeaders(options.headers as Record<string, string> || {});

  return fetch(requestUrl, {
    ...options,
    headers
  });
};

/**
 * path: relative PATH without host and port (i.e. '/api/123')
 * data: object that will be passed as request body
 * onSuccess: callback handler if request succeeded. Succeeded means it could technically be handled (i.e. valid json is returned)
 * regardless of the HTTP status code.
 */
export const submitForm = (method: IHttpMethod, path: string, data: any, onSuccess: (status: number, response: any) => void) => {
  const requestUrl = url(path);

  const headers = createHeaders({
    'Content-Type': 'application/json'
  });

  const fetchParams = {
    method: method,
    headers: headers,
    body: JSON.stringify(data)
  };

  console.log('Submitting to ' + method + ' ' + requestUrl);
  return fetch(requestUrl, fetchParams)
    .then(response => response.status === 204 ? onSuccess(response.status, {}) : response.json().then(result => onSuccess(response.status, result)));
};
