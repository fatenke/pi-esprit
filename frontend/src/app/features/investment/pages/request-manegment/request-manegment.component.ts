import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { InvestmentRequestService } from '../../services/investment-request.service';
import { InvestmentRequest } from '../../models/investment-request';

@Component({
  selector: 'app-request-form',
  templateUrl: './request-form.component.html',
  styleUrl: './request-form.component.css'
})
export class RequestManegementComponent implements OnInit {

  request: InvestmentRequest = {
    id: '',
    introMessage: '',
    startupId: '',
    ticketProposed: undefined,
    investorId: '',
    investmentStatus: 'PENDING',
    sentAt: new Date()
  };

  investorDocFile: File | null = null;
  investorDocError = '';

  isEditMode = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private requestService: InvestmentRequestService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');

    if (id) {
      this.isEditMode = true;

      this.requestService.getById(id).subscribe(data => {
        this.request = data;
      });
    }
  }

  onDocSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) return;

    const isPdf =
      file.type === 'application/pdf' ||
      file.name.toLowerCase().endsWith('.pdf');

    if (!isPdf) {
      this.investorDocError = 'PDF uniquement';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.investorDocError = 'Max 5MB';
      return;
    }

    this.investorDocFile = file;
  }

  submit() {
    const form = new FormData();

    form.append('introMessage', this.request.introMessage ?? '');
    form.append('startupId', this.request.startupId ?? '');

    if (this.request.ticketProposed != null) {
      form.append('ticketProposed', String(this.request.ticketProposed));
    }

    if (this.investorDocFile) {
      form.append('investorDoc', this.investorDocFile);
    }

    this.requestService.create(form).subscribe(() => {
      alert('Demande envoyée');
      this.router.navigate(['/investment/demandes']);
    });
  }

  update() {
    this.requestService.updateRequest(this.request.id!)
      .subscribe(() => {
        alert('Mis à jour !');
        this.router.navigate(['/investment/demandes']);
      });
  }

  backToList() {
    this.router.navigate(['/investment/startups']);
  }
}