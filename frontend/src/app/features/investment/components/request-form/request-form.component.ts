import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { InvestmentRequestService } from '../../services/investment-request.service';
import { InvestmentRequest } from '../../models/investment-request';

@Component({
  selector: 'app-request-form',
  templateUrl: './request-form.component.html',
  styleUrls: ['./request-form.component.css']
})
export class RequestFormComponent implements OnInit {
  mode: 'add' | 'edit' = 'add';
  currentRequest: InvestmentRequest | null = null;

  isVisible = false;
  isSubmitting = false;
  submitSuccess = false;
  selectedFile: File | null = null;
  fileError = '';

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private requestService: InvestmentRequestService
  ) {}

  ngOnInit(): void {
    this.buildForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.mode = 'edit';
      this.requestService.getById(id).subscribe((data) => {
        this.currentRequest = data;
        this.openEdit(data);
      });
      return;
    }

    const startupId = this.route.snapshot.paramMap.get('startupId');
    this.openAdd(startupId ?? '');
  }

  private buildForm(): void {
    this.form = this.fb.group({
      introMessage: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(500)]],
      ticketProposed: [null, [Validators.min(1)]],
      investorId: ['dev-investor', Validators.required],
      startupId: ['', Validators.required],
    });
  }

  openAdd(startupId = ''): void {
    this.mode = 'add';
    this.currentRequest = null;
    this.isVisible = true;
    this.submitSuccess = false;
    this.form.reset({
      investorId: 'dev-investor',
      startupId,
      introMessage: '',
      ticketProposed: null,
    });
    this.form.get('investorId')?.enable();
    this.form.get('startupId')?.enable();
    this.selectedFile = null;
    this.fileError = '';
  }

  openEdit(req: InvestmentRequest): void {
    this.mode = 'edit';
    this.currentRequest = req;
    this.isVisible = true;
    this.submitSuccess = false;
    this.selectedFile = null;
    this.fileError = '';

    this.form.reset({
      investorId: req.investorId ?? 'dev-investor',
      startupId: req.startupId ?? '',
      introMessage: req.introMessage ?? '',
      ticketProposed: req.ticketProposed ?? null,
    });

    this.form.get('investorId')?.disable();
    this.form.get('startupId')?.disable();
  }

  closeForm(): void {
    this.isVisible = false;
    this.form.reset({
      investorId: 'dev-investor',
      startupId: '',
      introMessage: '',
      ticketProposed: null,
    });
    this.selectedFile = null;
    this.fileError = '';
    this.submitSuccess = false;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    if (this.mode === 'add') {
      const raw = this.form.getRawValue();
      const formData = new FormData();
      formData.append('startupId', raw.startupId);
      formData.append('introMessage', (raw.introMessage ?? '').trim());
      if (raw.ticketProposed != null && raw.ticketProposed !== '') {
        formData.append('ticketProposed', String(raw.ticketProposed));
      }
      if (this.selectedFile) {
        formData.append('investorDoc', this.selectedFile, this.selectedFile.name);
      }

      this.requestService.create(formData, raw.investorId).subscribe({
        next: () => this.handleSuccess(),
        error: () => {
          this.isSubmitting = false;
        }
      });
      return;
    }

    const raw = this.form.getRawValue();
    const payload: InvestmentRequest = {
      ...(this.currentRequest as InvestmentRequest),
      introMessage: (raw.introMessage ?? '').trim(),
      ticketProposed: raw.ticketProposed ?? undefined,
    };

    this.requestService.updateRequest(payload.id, payload).subscribe({
      next: () => this.handleSuccess(),
      error: () => {
        this.isSubmitting = false;
      }
    });
  }

  private handleSuccess(): void {
    this.isSubmitting = false;
    this.submitSuccess = true;
    setTimeout(() => this.router.navigate(['/investment/demandes']), 800);
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    this.fileError = '';
    this.selectedFile = null;
    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.fileError = 'Uniquement les fichiers PDF sont acceptes';
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.fileError = 'Taille maximale : 5 MB';
      return;
    }
    this.selectedFile = file;
  }

  removeFile(): void {
    this.selectedFile = null;
    this.fileError = '';
  }

  get isEditMode(): boolean { return this.mode === 'edit'; }
  get isAddMode(): boolean { return this.mode === 'add'; }

  get formTitle(): string {
    return this.mode === 'add'
      ? 'Nouvelle demande d\'investissement'
      : 'Modifier la demande';
  }

  get submitLabel(): string {
    if (this.isSubmitting) {
      return this.mode === 'add' ? 'Envoi en cours...' : 'Enregistrement...';
    }
    return this.mode === 'add'
      ? 'Envoyer la demande'
      : 'Enregistrer les modifications';
  }

  get successMessage(): string {
    return this.mode === 'add'
      ? 'Demande envoyee avec succes.'
      : 'Demande mise a jour avec succes.';
  }

  get charCount(): number {
    return this.form.get('introMessage')?.value?.length ?? 0;
  }

  isInvalid(field: string): boolean {
    const ctrl = this.form.get(field);
    return !!(ctrl?.invalid && ctrl?.touched);
  }

  getError(field: string): string {
    const errors = this.form.get(field)?.errors;
    if (!errors) return '';
    if (errors['required']) return 'Ce champ est obligatoire';
    if (errors['minlength']) {
      return `Minimum ${errors['minlength'].requiredLength} caracteres`;
    }
    if (errors['maxlength']) {
      return `Maximum ${errors['maxlength'].requiredLength} caracteres`;
    }
    if (errors['min']) return 'Le montant doit etre positif';
    return 'Valeur invalide';
  }

  trackById(_: number, req: InvestmentRequest): string {
    return req.id;
  }
}
