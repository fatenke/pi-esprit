import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DataRoomComponent } from './components/data-room/data-room.component';

const routes: Routes = [{ path: ':roomId', component: DataRoomComponent }];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class DataRoomRoutingModule {}
