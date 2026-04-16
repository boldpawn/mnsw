import { Routes } from '@angular/router';

export const formalitiesRoutes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./formality-list/formality-list.component').then(
        m => m.FormalityListComponent
      ),
  },
];
