import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { InvestmentHolding } from '../../features/investment/models/investment-holding.model';
import { InvestmentHoldingService } from '../../features/investment/services/investment-holding.service';

@Component({
  selector: 'app-payment-success',
  template: `
    <div class="payment-state success">
      <div class="eyebrow">Stripe Checkout</div>
      <h1>Paiement confirme</h1>
      <p *ngIf="loading">Confirmation de votre paiement Stripe de test...</p>
      <p class="message error" *ngIf="!loading && error">{{ error }}</p>
      <div class="message ok" *ngIf="!loading && message">{{ message }}</div>

      <div class="summary" *ngIf="holding">
        <div>
          <span>Holding</span>
          <strong>{{ holding.id }}</strong>
        </div>
        <div>
          <span>Statut</span>
          <strong>{{ holding.status }}</strong>
        </div>
        <div>
          <span>Montant</span>
          <strong>{{ holding.amountTnd | number }} {{ holding.currencyDisplayed }}</strong>
        </div>
      </div>

      <div class="actions">
        <a class="primary" *ngIf="holding" [href]="'/investment/holding/request/' + holding.investmentRequestId">Ouvrir le holding</a>
        <a class="ghost" href="/investment/kanban">Retour au kanban</a>
      </div>
    </div>
  `,
  styles: [`
    .payment-state {
      min-height: 100vh;
      display: grid;
      align-content: center;
      gap: 16px;
      padding: 32px;
      background: linear-gradient(180deg, #f7fbff 0%, #eaf3ff 100%);
      color: #0f172a;
    }
    .eyebrow {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: #2563eb;
    }
    h1 {
      margin: 0;
      font-size: 40px;
    }
    p {
      margin: 0;
      color: rgba(15, 23, 42, 0.66);
    }
    .message {
      max-width: 720px;
      padding: 14px 16px;
      border-radius: 16px;
      font-size: 14px;
      border: 1px solid transparent;
    }
    .message.ok {
      background: rgba(236, 253, 245, 0.96);
      color: #15803d;
      border-color: rgba(22, 163, 74, 0.12);
    }
    .message.error {
      background: rgba(254, 242, 242, 0.96);
      color: #b91c1c;
      border-color: rgba(220, 38, 38, 0.12);
    }
    .summary {
      max-width: 860px;
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
    }
    .summary div {
      padding: 18px;
      border-radius: 22px;
      background: rgba(255, 255, 255, 0.82);
      border: 1px solid rgba(59, 130, 246, 0.12);
      box-shadow: 0 18px 40px rgba(37, 99, 235, 0.08);
    }
    .summary span {
      display: block;
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: rgba(15, 23, 42, 0.48);
    }
    .summary strong {
      display: block;
      margin-top: 8px;
      font-size: 18px;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
    }
    .actions a {
      text-decoration: none;
      padding: 12px 16px;
      border-radius: 14px;
      font-weight: 800;
    }
    .actions .primary {
      background: linear-gradient(180deg, #2563eb, #1d4ed8);
      color: #fff;
    }
    .actions .ghost {
      background: rgba(15, 23, 42, 0.06);
      color: #334155;
    }
    @media (max-width: 900px) {
      .summary {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class PaymentSuccessComponent implements OnInit, OnDestroy {
  loading = true;
  message = '';
  error = '';
  holding: InvestmentHolding | null = null;

  private sub = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private holdingService: InvestmentHoldingService
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.queryParamMap.get('session_id') ?? '';
    if (!sessionId) {
      this.error = 'Le session_id Stripe Checkout est manquant dans l URL de succes.';
      this.loading = false;
      return;
    }

    this.sub.add(
      this.holdingService.confirmCheckout({ sessionId }).subscribe({
        next: (holding) => {
          this.holding = holding;
          this.message = 'Paiement confirme, fonds marques comme FUNDS_HELD.';
          this.loading = false;
        },
        error: (err) => {
          this.error = err?.error?.error ?? 'Impossible de confirmer le paiement Stripe Checkout.';
          this.loading = false;
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
