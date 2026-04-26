import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PaymentCancelComponent } from './components/payment-cancel/payment-cancel.component';
import { PaymentSuccessComponent } from './components/payment-success/payment-success.component';

const routes: Routes = [
  { path: '', redirectTo: 'investment', pathMatch: 'full' },
  { path: 'payment-success', component: PaymentSuccessComponent },
  { path: 'payment-cancel', component: PaymentCancelComponent },
  {
    path: 'investment',
    loadChildren: () =>
      import('./features/investment/investment.module').then(
        (m) => m.InvestmentModule
      ),
  },
  {
    path: 'data-room',
    loadChildren: () =>
      import('./features/data-room/data-room.module').then(
        (m) => m.DataRoomModule
      ),
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
