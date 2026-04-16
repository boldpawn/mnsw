import { Routes } from '@angular/router';

export const visitsRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./visit-list/visit-list.component').then(m => m.VisitListComponent),
  },
];
