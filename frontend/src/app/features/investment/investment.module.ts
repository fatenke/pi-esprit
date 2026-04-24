import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InvestmentRoutingModule } from './investment-routing.module';
import { CriteriaFormComponent } from './components/criteria-form/criteria-form.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CriteriaListComponent } from './components/criteria-list/criteria-list.component';
import { KanbanBoardComponent } from './components/kanban-board/kanban-board.component';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DealCardComponent } from './components/deal-card/deal-card.component';
import { KanbanColumnComponent } from './components/kanban-column/kanban-column.component';
import { InvestmentHoldingComponent } from './components/investment-holding/investment-holding.component';
import { RequestFormComponent } from './components/request-form/request-form.component';
import { StartupListComponent } from './components/startup-list/startup-list.component';
import { RequestManagementComponent } from './components/request-management/request-management.component';

@NgModule({
  declarations: [
    CriteriaFormComponent,
    CriteriaListComponent,
    KanbanBoardComponent,
    KanbanColumnComponent,
    InvestmentHoldingComponent,
    DealCardComponent,
    RequestFormComponent,
    StartupListComponent,
    RequestManagementComponent,
  ],
  imports: [
    CommonModule,
    InvestmentRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    DragDropModule,
    MatProgressSpinnerModule
  ]
})
export class InvestmentModule { }
