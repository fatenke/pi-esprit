/**
 * Base URL for API calls.
 * - Browser: '' so requests stay same-origin (`/api/...`) and `ng serve` proxy applies.
 * - SSR / prerender (Node): absolute URL because `fetch()` requires it server-side.
 */
export function apiOrigin(): string {
  return typeof document !== 'undefined' ? '' : 'http://localhost:8080';
}
