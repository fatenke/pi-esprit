import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { STARTUP_CATALOG, StartupCatalogEntry } from '../../data/startup-catalog';

type Startup = StartupCatalogEntry;

@Component({
  selector: 'app-startup-list',
  templateUrl: './startup-list.component.html',
  styleUrl: './startup-list.component.css',
})
export class StartupListComponent {
  startups: Startup[] = STARTUP_CATALOG;

  constructor(private router: Router) {}

  requestInvestment(startup: Startup) {
    this.router.navigate(['/investment/request', startup.id], {
      queryParams: { name: startup.name },
    });
  }
}
