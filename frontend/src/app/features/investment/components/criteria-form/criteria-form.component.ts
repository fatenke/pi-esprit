import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InvestmentCriteria } from '../../models/investment-criteria.model';
import { InvestmentCriteriaService } from '../../services/investment-criteria.service';

@Component({
  selector: 'app-criteria-form',
  templateUrl: './criteria-form.component.html',
  styleUrl: './criteria-form.component.css'
})
export class CriteriaFormComponent implements OnInit {
  criteria: InvestmentCriteria = {
    investorId: '',
    sectors: [],
    stages: [],
    minBudget: 0,
    maxBudget: 0,
    location: '',
    active: true,
  };

  sectorDraft = '';
  stageDraft = '';
  isEditMode = false;
  criteriaId = '';
  existingCriteriaId = '';
  submitting = false;
  error = '';

  constructor(
    private service: InvestmentCriteriaService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  readonly sectorSuggestions = [
    'Fintech',
    'Healthtech',
    'Edtech',
    'E-commerce',
    'SaaS',
    'AI / ML',
    'Cybersecurity',
    'Blockchain / Web3',
    'Biotech',
    'CleanTech',
    'Energy',
    'Mobility',
    'Real Estate / PropTech',
    'Logistics',
    'Gaming',
    'Media',
    'Agritech',
    'Manufacturing',
    'Retail',
    'Insurtech'
  ] as const;

  readonly stageSuggestions = ['Pre-Seed', 'Seed', 'Series A', 'Series B', 'Series C', 'Growth'] as const;

  ngOnInit(): void {
    this.criteriaId = this.route.snapshot.paramMap.get('id') ?? '';
    this.isEditMode = !!this.criteriaId;

    if (this.isEditMode) {
      this.service.getById(this.criteriaId).subscribe({
        next: (criteria) => {
          this.criteria = {
            ...criteria,
            sectors: [...criteria.sectors],
            stages: [...criteria.stages],
          };
        },
        error: () => {
          this.error = 'Impossible de charger ce profil d investissement.';
        }
      });
      return;
    }

    const investorFromQuery = this.route.snapshot.queryParamMap.get('investorId');
    const storedInvestor = this.safeStorageGet('investment.criteria.investorId');
    this.criteria.investorId = investorFromQuery || storedInvestor || 'dev-investor';
    this.lookupExistingCriteria();
  }

  get filteredSectorSuggestions(): string[] {
    const q = this.sectorDraft.trim().toLowerCase();
    if (!q) return [];

    const chosen = new Set(this.criteria.sectors.map((s) => s.toLowerCase()));
    return this.sectorSuggestions
      .filter((s) => s.toLowerCase().includes(q))
      .filter((s) => !chosen.has(s.toLowerCase()))
      .slice(0, 8);
  }

  get filteredStageSuggestions(): string[] {
    const q = this.stageDraft.trim().toLowerCase();
    if (!q) return [];

    const chosen = new Set(this.criteria.stages.map((s) => s.toLowerCase()));
    return this.stageSuggestions
      .filter((s) => s.toLowerCase().includes(q))
      .filter((s) => !chosen.has(s.toLowerCase()))
      .slice(0, 8);
  }

  selectSectorSuggestion(value: string): void {
    this.sectorDraft = value;
    this.addSector();
  }

  selectStageSuggestion(value: string): void {
    this.stageDraft = value;
    this.addStage();
  }

  addSector(): void {
    const value = this.sectorDraft.trim();
    if (!value) return;

    if (!this.criteria.sectors.some((sector) => sector.toLowerCase() === value.toLowerCase())) {
      this.criteria.sectors = [...this.criteria.sectors, value];
    }
    this.sectorDraft = '';
  }

  removeSector(sector: string): void {
    this.criteria.sectors = this.criteria.sectors.filter((item) => item !== sector);
  }

  addStage(): void {
    const value = this.stageDraft.trim();
    if (!value) return;

    if (!this.criteria.stages.some((stage) => stage.toLowerCase() === value.toLowerCase())) {
      this.criteria.stages = [...this.criteria.stages, value];
    }
    this.stageDraft = '';
  }

  removeStage(stage: string): void {
    this.criteria.stages = this.criteria.stages.filter((item) => item !== stage);
  }

  submit(): void {
    if (
      this.criteria.minBudget != null &&
      this.criteria.maxBudget != null &&
      this.criteria.maxBudget > 0 &&
      this.criteria.minBudget > this.criteria.maxBudget
    ) {
      alert('Le budget minimum ne peut pas depasser le budget maximum.');
      return;
    }

    if (!this.isEditMode) {
      this.lookupExistingCriteria(true);
      return;
    }

    this.persistInvestorId();
    this.saveCriteria();
  }

  openExistingCriteria(): void {
    if (!this.existingCriteriaId) return;
    this.router.navigate(['/investment/criteria/edit', this.existingCriteriaId]);
  }

  private lookupExistingCriteria(submitAfterCheck = false): void {
    const investorId = this.criteria.investorId.trim();
    if (!investorId) {
      this.error = 'L identifiant investisseur est obligatoire.';
      return;
    }

    this.criteria.investorId = investorId;
    this.submitting = submitAfterCheck;
    this.error = '';

    this.service.getInvestorCriteria(investorId).subscribe({
      next: (criteriaList) => {
        const existing = criteriaList.find((item) => item.id !== this.criteriaId) ?? null;
        this.existingCriteriaId = existing?.id ?? '';

        if (existing) {
          this.submitting = false;
          this.error = 'Un investisseur ne peut avoir qu un seul critere d investissement. Modifie le profil existant.';
          return;
        }

        if (submitAfterCheck) {
          this.persistInvestorId();
          this.saveCriteria();
        }
      },
      error: () => {
        this.submitting = false;
        this.error = 'Impossible de verifier les criteres existants pour cet investisseur.';
      }
    });
  }

  private saveCriteria(): void {
    this.submitting = true;
    this.error = '';

    const request$ = this.isEditMode
      ? this.service.update({ ...this.criteria, id: this.criteriaId })
      : this.service.create(this.criteria);

    request$.subscribe({
      next: () => {
        this.submitting = false;
        alert(this.isEditMode ? 'Critere mis a jour !' : 'Critere enregistre !');
        this.router.navigate(['/investment']);
      },
      error: () => {
        this.submitting = false;
        this.error = this.isEditMode
          ? 'Impossible de mettre a jour ce profil d investissement.'
          : 'Impossible d enregistrer ce profil d investissement.';
      }
    });
  }

  private persistInvestorId(): void {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem('investment.criteria.investorId', this.criteria.investorId);
  }

  private safeStorageGet(key: string): string | null {
    if (typeof window === 'undefined') return null;
    return window.localStorage.getItem(key);
  }
}
