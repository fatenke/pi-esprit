/**
 * Base URL for API calls.
 * - Browser: keep requests same-origin so the ingress can route `/api/...` without CORS.
 * - SSR / Node: allow an explicit API origin, otherwise fall back to the Kubernetes service.
 */
export function apiOrigin(): string {
  if (typeof document !== 'undefined') {
    return '';
  }

  if (typeof process !== 'undefined' && process.env?.['API_ORIGIN']) {
    return process.env['API_ORIGIN'];
  }

  return 'http://api-gateway:8091';
}
