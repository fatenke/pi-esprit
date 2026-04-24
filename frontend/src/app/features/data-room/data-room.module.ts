import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataRoomRoutingModule } from './data-room-routing.module';
import { DataRoomComponent } from './components/data-room/data-room.component';
import { NdaComponent } from './components/nda/nda.component';
import { FolderListComponent } from './components/folder-list/folder-list.component';
import { DocumentListComponent } from './components/document-list/document-list.component';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';

@NgModule({
  declarations: [
    DataRoomComponent,
    NdaComponent,
    FolderListComponent,
    DocumentListComponent,
  ],
  imports: [
    CommonModule,
    DataRoomRoutingModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatListModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
})
export class DataRoomModule {}
