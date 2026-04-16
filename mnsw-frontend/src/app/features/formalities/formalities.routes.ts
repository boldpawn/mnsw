import { Routes } from '@angular/router';
import { FormalityListComponent } from './formality-list/formality-list.component';

export const formalitiesRoutes: Routes = [
  { path: '', component: FormalityListComponent },
  {
    path: 'new',
    loadComponent: () =>
      import('./formality-submit/formality-submit.component').then(m => m.FormalitySubmitComponent)
  },
  {
    path: 'new/noa',
    loadComponent: () =>
      import('./noa-form/noa-form.component').then(m => m.NoaFormComponent)
  },
  {
    path: 'new/nos',
    loadComponent: () =>
      import('./nos-form/nos-form.component').then(m => m.NosFormComponent)
  },
  {
    path: 'new/nod',
    loadComponent: () =>
      import('./nod-form/nod-form.component').then(m => m.NodFormComponent)
  },
  {
    path: 'new/vid',
    loadComponent: () =>
      import('./formality-submit/vid-form/vid-form.component').then(m => m.VidFormComponent)
  },
  {
    path: 'new/sid',
    loadComponent: () =>
      import('./formality-submit/sid-form/sid-form.component').then(m => m.SidFormComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./formality-detail/formality-detail.component').then(m => m.FormalityDetailComponent)
  },
  {
    path: ':id/correct',
    loadComponent: () =>
      import('./formality-correct/formality-correct.component').then(m => m.FormalityCorrectComponent)
  },
];
