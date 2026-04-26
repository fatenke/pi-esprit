import { Component } from '@angular/core';

@Component({
  selector: 'app-payment-cancel',
  template: `
    <div class="payment-state cancel">
      <div class="eyebrow">Stripe Checkout</div>
      <h1>Paiement annule</h1>
      <p>La session Stripe Checkout a ete annulee. Aucun paiement reel n a ete effectue et le holding reste en attente.</p>
      <div class="actions">
        <a class="primary" href="/investment/kanban">Retour au kanban</a>
        <a class="ghost" href="/investment">Retour a l accueil investissement</a>
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
      background: linear-gradient(180deg, #fffaf6 0%, #fef3e8 100%);
      color: #0f172a;
    }
    .eyebrow {
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: #d97706;
    }
    h1 {
      margin: 0;
      font-size: 40px;
    }
    p {
      margin: 0;
      max-width: 720px;
      color: rgba(15, 23, 42, 0.66);
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
      background: linear-gradient(180deg, #f59e0b, #d97706);
      color: #fff;
    }
    .actions .ghost {
      background: rgba(15, 23, 42, 0.06);
      color: #334155;
    }
  `]
})
export class PaymentCancelComponent {}
