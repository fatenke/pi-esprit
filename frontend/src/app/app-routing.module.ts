import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  { path: '', redirectTo: 'investment', pathMatch: 'full' },
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
