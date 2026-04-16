import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth/auth.guard';
import { AppLayoutComponent } from './layout/app-layout/app-layout.component';

export const routes: Routes = [
  { path: '', redirectTo: '/formalities', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'formalities',
        loadChildren: () =>
          import('./features/formalities/formalities.routes').then(m => m.formalitiesRoutes),
      },
      {
        path: 'visits',
        loadChildren: () =>
          import('./features/visits/visits.routes').then(m => m.visitsRoutes),
      },
      {
        path: 'users',
        canActivate: [roleGuard('ADMIN')],
        loadChildren: () =>
          import('./features/users/users.routes').then(m => m.usersRoutes),
      },
    ],
  },
  { path: '**', redirectTo: '/formalities' },
];
