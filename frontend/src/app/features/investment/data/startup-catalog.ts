export type StartupCatalogEntry = {
  id: string;
  name: string;
  tagline: string;
  sector: string;
  stage: string;
  location: string;
};

export const STARTUP_CATALOG: StartupCatalogEntry[] = [
  {
    id: 's-001',
    name: 'NovaPay',
    tagline: 'Payments infrastructure for emerging markets.',
    sector: 'Fintech',
    stage: 'Seed',
    location: 'Tunis',
  },
  {
    id: 's-002',
    name: 'MedAIriage',
    tagline: 'AI triage assistant for clinics.',
    sector: 'Healthtech',
    stage: 'Series A',
    location: 'Paris',
  },
  {
    id: 's-003',
    name: 'LogiFlow',
    tagline: 'Route optimization for last-mile delivery.',
    sector: 'Logistics',
    stage: 'Pre-Seed',
    location: 'Remote',
  },
  {
    id: 's-004',
    name: 'GreenPulse',
    tagline: 'Energy monitoring for industrial sites.',
    sector: 'CleanTech',
    stage: 'Seed',
    location: 'Casablanca',
  },
  {
    id: 's-005',
    name: 'SecureStack',
    tagline: 'Continuous security posture for SaaS teams.',
    sector: 'Cybersecurity',
    stage: 'Series B',
    location: 'Berlin',
  },
  {
    id: 's-006',
    name: 'ClassCrafted',
    tagline: 'Skills-focused learning for schools.',
    sector: 'Edtech',
    stage: 'Seed',
    location: 'Lyon',
  },
];

export const STARTUP_CATALOG_BY_ID = Object.fromEntries(
  STARTUP_CATALOG.map((startup) => [startup.id, startup])
) as Record<string, StartupCatalogEntry>;
