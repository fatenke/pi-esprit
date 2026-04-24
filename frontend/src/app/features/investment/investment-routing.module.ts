import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CriteriaFormComponent } from './components/criteria-form/criteria-form.component';
import { CriteriaListComponent } from './components/criteria-list/criteria-list.component';
import { KanbanBoardComponent } from './components/kanban-board/kanban-board.component';
import { InvestmentHoldingComponent } from './components/investment-holding/investment-holding.component';
import { RequestManagementComponent } from './components/request-management/request-management.component';
import { RequestFormComponent } from './components/request-form/request-form.component';
import { StartupListComponent } from './components/startup-list/startup-list.component';

/**
 * Routes du lazy module `InvestmentModule` (préfixe parent : `/investment`).
 *
 * Exemples d’URL :
 * - `/investment` — liste des critères
 * - `/investment/criteria` — formulaire critères
 * - `/investment/kanban` — pipeline Kanban (deals)
 * - `/investment/demandes` — demandes d’investissement
 * - `/investment/data-room/:roomId` — salle de données (lazy, même module que `/data-room/:roomId`)
 */
const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    component: CriteriaListComponent,
    data: { title: 'Critères' },
  },
  {
    path: 'startups',
    component: StartupListComponent,
    data: { title: 'Startups' },
  },
  {
    path: 'criteria',
    component: CriteriaFormComponent,
    data: { title: 'Nouveau critère' },
  },
  {
    path: 'kanban',
    component: KanbanBoardComponent,
    data: { title: 'Pipeline' },
  },
  {
    path: 'holding/request/:requestId',
    component: InvestmentHoldingComponent,
    data: { title: 'Investment Holding' },
  },
  {
    path: 'demandes',
    component: RequestManagementComponent,
    data: { title: 'Demandes' },
  },
  {
    path: 'request/:startupId',
    component: RequestFormComponent,
    data: { title: 'Demande d’investissement' },
  },
  { path: 'edit-request/:id', component: RequestFormComponent,data: { title: 'modifier la demande d’investissement' },},
  {
    path: 'data-room',
    loadChildren: () =>
      import('../data-room/data-room.module').then((m) => m.DataRoomModule),
    data: { title: 'Data room' },
  },
  {
    path: '**',
    redirectTo: '',
    pathMatch: 'full',
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class InvestmentRoutingModule {}
