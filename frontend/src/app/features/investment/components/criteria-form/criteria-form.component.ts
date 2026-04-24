import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { InvestmentCriteriaService } from '../../services/investment-criteria.service';

@Component({
  selector: 'app-criteria-form',
  templateUrl: './criteria-form.component.html',
  styleUrl: './criteria-form.component.css'
})
export class CriteriaFormComponent {
   criteria = {
    investorId: '',
    sectors: [] as string[],
    stages: [] as string[],
    minBudget: 0,
    maxBudget: 0,
    location: ''
  };

  sectorDraft = '';
  stageDraft = '';

  constructor(private service: InvestmentCriteriaService, private router: Router) {}

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

  get filteredSectorSuggestions(): string[] {
    const q = this.sectorDraft.trim().toLowerCase();
    if (!q) return [];

    const chosen = new Set(this.criteria.sectors.map(s => s.toLowerCase()));
    return this.sectorSuggestions
      .filter(s => s.toLowerCase().includes(q))
      .filter(s => !chosen.has(s.toLowerCase()))
      .slice(0, 8);
  }

  get filteredStageSuggestions(): string[] {
    const q = this.stageDraft.trim().toLowerCase();
    if (!q) return [];

    const chosen = new Set(this.criteria.stages.map(s => s.toLowerCase()));
    return this.stageSuggestions
      .filter(s => s.toLowerCase().includes(q))
      .filter(s => !chosen.has(s.toLowerCase()))
      .slice(0, 8);
  }

  selectSectorSuggestion(value: string) {
    this.sectorDraft = value;
    this.addSector();
  }

  selectStageSuggestion(value: string) {
    this.stageDraft = value;
    this.addStage();
  }

  addSector() {
    const value = this.sectorDraft.trim();
    if (!value) return;

    if (!this.criteria.sectors.some(s => s.toLowerCase() === value.toLowerCase())) {
      this.criteria.sectors = [...this.criteria.sectors, value];
    }
    this.sectorDraft = '';
  }

  removeSector(sector: string) {
    this.criteria.sectors = this.criteria.sectors.filter(s => s !== sector);
  }

  addStage() {
    const value = this.stageDraft.trim();
    if (!value) return;

    if (!this.criteria.stages.some(s => s.toLowerCase() === value.toLowerCase())) {
      this.criteria.stages = [...this.criteria.stages, value];
    }
    this.stageDraft = '';
  }

  removeStage(stage: string) {
    this.criteria.stages = this.criteria.stages.filter(s => s !== stage);
  }

  submit() {
    if (
      this.criteria.minBudget != null &&
      this.criteria.maxBudget != null &&
      this.criteria.maxBudget > 0 &&
      this.criteria.minBudget > this.criteria.maxBudget
    ) {
      alert('Min budget cannot be greater than max budget.');
      return;
    }

    this.service.create(this.criteria).subscribe(res => {
      console.log('Saved!', res);
      alert('Criteria saved!');
      this.router.navigate(['/investment/list']);
      
    });
  }


}
