import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { InvestmentCriteria } from '../../models/investment-criteria.model';
import { InvestmentCriteriaService } from '../../services/investment-criteria.service';

type RoleView = 'ROLE_INVESTOR' | 'ROLE_ADMIN' | 'ROLE_STARTUP';
type StatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';

@Component({
  selector: 'app-criteria-list',
  templateUrl: './criteria-list.component.html',
  styleUrls: ['./criteria-list.component.css']
})
export class CriteriaListComponent implements OnInit, OnDestroy {
  readonly roleOptions: RoleView[] = ['ROLE_INVESTOR', 'ROLE_ADMIN', 'ROLE_STARTUP'];
  readonly pageSize = 6;

  selectedRole: RoleView = 'ROLE_INVESTOR';
  currentInvestorId = 'dev-investor';

  loading = true;
  error = '';

  allCriteria: InvestmentCriteria[] = [];
  visibleCriteria: InvestmentCriteria[] = [];
  paginatedCriteria: InvestmentCriteria[] = [];

  searchTerm = '';
  selectedSector = 'ALL';
  selectedStage = 'ALL';
  selectedStatus: StatusFilter = 'ALL';

  currentPage = 1;
  totalPages = 1;

  private readonly sub = new Subscription();

  constructor(
    private readonly criteriaService: InvestmentCriteriaService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const roleFromQuery = this.route.snapshot.queryParamMap.get('role');
    const storedRole = this.safeStorageGet('investment.criteria.role');
    const resolvedRole = (roleFromQuery || storedRole || 'ROLE_INVESTOR').toUpperCase();
    if (this.isRoleView(resolvedRole)) {
      this.selectedRole = resolvedRole;
    }

    const investorFromQuery = this.route.snapshot.queryParamMap.get('investorId');
    const storedInvestor = this.safeStorageGet('investment.criteria.investorId');
    this.currentInvestorId = investorFromQuery || storedInvestor || 'dev-investor';

    this.loadCriteria();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  get isInvestorView(): boolean {
    return this.selectedRole === 'ROLE_INVESTOR';
  }

  get isAdminView(): boolean {
    return this.selectedRole === 'ROLE_ADMIN';
  }

  get isStartupView(): boolean {
    return this.selectedRole === 'ROLE_STARTUP';
  }

  get investorCriteria(): InvestmentCriteria | null {
    return this.allCriteria.find((item) => item.investorId === this.currentInvestorId) ?? null;
  }

  get canCreateInvestorCriteria(): boolean {
    return !this.investorCriteria;
  }

  get availableSectors(): string[] {
    return this.uniqueValues(this.allCriteria.flatMap((item) => item.sectors));
  }

  get availableStages(): string[] {
    return this.uniqueValues(this.allCriteria.flatMap((item) => item.stages));
  }

  get emptyMessage(): string {
    if (this.error) return this.error;
    if (this.isInvestorView) {
      return 'Cet investisseur n a pas encore defini son profil d investissement.';
    }
    return 'Aucun critere disponible';
  }

  setRole(role: RoleView): void {
    this.selectedRole = role;
    this.safeStorageSet('investment.criteria.role', role);
    this.applyFilters();
  }

  setInvestorId(value: string): void {
    this.currentInvestorId = value;
    this.safeStorageSet('investment.criteria.investorId', value);
    this.applyFilters();
  }

  applyFilters(): void {
    const search = this.searchTerm.trim().toLowerCase();
    const byRole = this.allCriteria.filter((item) => this.matchesRole(item));

    this.visibleCriteria = byRole.filter((item) => {
      const matchesSearch = !search || item.name?.toLowerCase().includes(search);
      const matchesSector = this.selectedSector === 'ALL' || item.sectors.includes(this.selectedSector);
      const matchesStage = this.selectedStage === 'ALL' || item.stages.includes(this.selectedStage);
      const matchesStatus =
        this.selectedStatus === 'ALL'
        || (this.selectedStatus === 'ACTIVE' && this.criteriaService.isActive(item))
        || (this.selectedStatus === 'INACTIVE' && !this.criteriaService.isActive(item));

      return matchesSearch && matchesSector && matchesStage && matchesStatus;
    });

    this.currentPage = 1;
    this.updatePagination();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.selectedSector = 'ALL';
    this.selectedStage = 'ALL';
    this.selectedStatus = 'ALL';
    this.applyFilters();
  }

  deleteCriteria(criteria: InvestmentCriteria): void {
    if (!criteria.id) return;
    const confirmed = window.confirm('Etes-vous sur de vouloir supprimer ce critere ?');
    if (!confirmed) return;

    this.loading = true;
    this.error = '';
    this.sub.add(
      this.criteriaService.deleteCriteria(criteria.id).subscribe({
        next: () => {
          this.allCriteria = this.allCriteria.filter((item) => item.id !== criteria.id);
          this.loading = false;
          this.applyFilters();
        },
        error: () => {
          this.error = 'Impossible de supprimer ce critere.';
          this.loading = false;
        }
      })
    );
  }

  editCriteria(criteria: InvestmentCriteria): void {
    if (!criteria.id) return;
    this.router.navigate(['/investment/criteria/edit', criteria.id]);
  }

  previousPage(): void {
    if (this.currentPage <= 1) return;
    this.currentPage -= 1;
    this.updatePagination();
  }

  nextPage(): void {
    if (this.currentPage >= this.totalPages) return;
    this.currentPage += 1;
    this.updatePagination();
  }

  trackByCriteria(_: number, criteria: InvestmentCriteria): string {
    return criteria.id ?? criteria.name ?? `${criteria.investorId}-${criteria.location}`;
  }

  statusLabel(criteria: InvestmentCriteria): string {
    return this.criteriaService.isActive(criteria) ? 'Actif' : 'Inactif';
  }

  roleLabel(role: RoleView): string {
    switch (role) {
      case 'ROLE_INVESTOR':
        return 'Investisseur';
      case 'ROLE_ADMIN':
        return 'Administrateur';
      case 'ROLE_STARTUP':
        return 'Startup';
      default:
        return role;
    }
  }

  criteriaDate(criteria: InvestmentCriteria): string | undefined {
    return criteria.updatedAt || criteria.createdAt;
  }

  ticketLabel(criteria: InvestmentCriteria): string {
    const min = Number(criteria.minBudget ?? 0);
    const max = Number(criteria.maxBudget ?? 0);

    if (min > 0 && max > 0) {
      return `${this.formatBudget(min)} - ${this.formatBudget(max)}`;
    }
    if (max > 0) {
      return `Jusqu a ${this.formatBudget(max)}`;
    }
    if (min > 0) {
      return `A partir de ${this.formatBudget(min)}`;
    }
    return 'Non defini';
  }

  profileTitle(criteria: InvestmentCriteria): string {
    return criteria.name || `Profil de ${criteria.investorId}`;
  }

  private loadCriteria(): void {
    this.loading = true;
    this.error = '';

    this.sub.add(
      this.criteriaService.getAllCriteriaAdmin().subscribe({
        next: (criteria) => {
          this.allCriteria = criteria.sort((left, right) => {
            const leftDate = this.criteriaDate(left) ?? '';
            const rightDate = this.criteriaDate(right) ?? '';
            return rightDate.localeCompare(leftDate);
          });
          this.loading = false;
          this.applyFilters();
        },
        error: () => {
          this.error = 'Impossible de charger les criteres.';
          this.loading = false;
          this.allCriteria = [];
          this.visibleCriteria = [];
          this.paginatedCriteria = [];
        }
      })
    );
  }

  private updatePagination(): void {
    const safeTotal = Math.max(1, Math.ceil(this.visibleCriteria.length / this.pageSize));
    this.totalPages = safeTotal;
    this.currentPage = Math.min(this.currentPage, this.totalPages);

    const startIndex = (this.currentPage - 1) * this.pageSize;
    this.paginatedCriteria = this.visibleCriteria.slice(startIndex, startIndex + this.pageSize);
  }

  private matchesRole(criteria: InvestmentCriteria): boolean {
    if (this.isAdminView) return true;
    if (this.isInvestorView) return criteria.investorId === this.currentInvestorId;
    return this.criteriaService.isActive(criteria);
  }

  private uniqueValues(values: string[]): string[] {
    return [...new Set(values.filter(Boolean))].sort((left, right) => left.localeCompare(right));
  }

  private formatBudget(value: number): string {
    return new Intl.NumberFormat('fr-TN', { maximumFractionDigits: 0 }).format(value);
  }

  private isRoleView(value: string): value is RoleView {
    return this.roleOptions.includes(value as RoleView);
  }

  private safeStorageGet(key: string): string | null {
    if (typeof window === 'undefined') return null;
    return window.localStorage.getItem(key);
  }

  private safeStorageSet(key: string, value: string): void {
    if (typeof window === 'undefined') return;
    window.localStorage.setItem(key, value);
  }
}
