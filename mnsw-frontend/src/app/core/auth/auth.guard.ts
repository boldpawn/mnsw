import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { UserRole } from '../models/user.model';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAuthenticated()
    ? true
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const roleGuard =
  (role: UserRole): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    return auth.hasRole(role) ? true : router.createUrlTree(['/formalities']);
  };
