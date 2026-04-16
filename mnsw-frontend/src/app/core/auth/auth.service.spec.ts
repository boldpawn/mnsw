import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResult, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerSpy: jest.SpyInstance;

  const mockUser: User = {
    id: 'user-1',
    email: 'agent@rederij.nl',
    fullName: 'Jan Jansen',
    role: 'SCHEEPSAGENT',
  };

  const mockAuthResult: AuthResult = {
    token: 'eyJ.test.token',
    expiresAt: '2026-04-16T12:00:00Z',
    user: mockUser,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        {
          provide: Router,
          useValue: { navigate: jest.fn() },
        },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    routerSpy = jest.spyOn(router, 'navigate');

    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  describe('login', () => {
    it('moet een POST sturen naar /auth/login', () => {
      service.login('agent@rederij.nl', 'geheim').subscribe();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email: 'agent@rederij.nl', password: 'geheim' });
      req.flush(mockAuthResult);
    });

    it('moet het token en de gebruiker opslaan na succesvol inloggen', done => {
      service.login('agent@rederij.nl', 'geheim').subscribe(() => {
        expect(localStorage.getItem('mnsw_token')).toBe(mockAuthResult.token);
        expect(JSON.parse(localStorage.getItem('mnsw_user')!)).toEqual(mockUser);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });

    it('moet de currentUser Signal bijwerken na inloggen', done => {
      expect(service.currentUser()).toBeNull();

      service.login('agent@rederij.nl', 'geheim').subscribe(() => {
        expect(service.currentUser()).toEqual(mockUser);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });
  });

  describe('logout', () => {
    beforeEach(() => {
      localStorage.setItem('mnsw_token', 'test-token');
      localStorage.setItem('mnsw_user', JSON.stringify(mockUser));
    });

    it('moet token en gebruiker verwijderen uit localStorage', () => {
      service.logout();

      expect(localStorage.getItem('mnsw_token')).toBeNull();
      expect(localStorage.getItem('mnsw_user')).toBeNull();
    });

    it('moet naar /login navigeren', () => {
      service.logout();

      expect(routerSpy).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('isAuthenticated', () => {
    it('moet false teruggeven als er geen gebruiker is', () => {
      expect(service.isAuthenticated()).toBe(false);
    });

    it('moet true teruggeven als de gebruiker ingelogd is', done => {
      service.login('agent@rederij.nl', 'geheim').subscribe(() => {
        expect(service.isAuthenticated()).toBe(true);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });
  });

  describe('hasRole', () => {
    it('moet false teruggeven als er geen gebruiker is', () => {
      expect(service.hasRole('SCHEEPSAGENT')).toBe(false);
    });

    it('moet true teruggeven als de gebruiker de juiste rol heeft', done => {
      service.login('agent@rederij.nl', 'geheim').subscribe(() => {
        expect(service.hasRole('SCHEEPSAGENT')).toBe(true);
        expect(service.hasRole('ADMIN')).toBe(false);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });
  });

  describe('hasAnyRole', () => {
    it('moet true teruggeven als de gebruiker een van de opgegeven rollen heeft', done => {
      service.login('agent@rederij.nl', 'geheim').subscribe(() => {
        expect(service.hasAnyRole('SCHEEPSAGENT', 'LADINGAGENT')).toBe(true);
        expect(service.hasAnyRole('HAVENAUTORITEIT', 'ADMIN')).toBe(false);
        done();
      });

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      req.flush(mockAuthResult);
    });
  });

  describe('getToken', () => {
    it('moet null teruggeven als er geen token is', () => {
      expect(service.getToken()).toBeNull();
    });

    it('moet het token teruggeven als het opgeslagen is', () => {
      localStorage.setItem('mnsw_token', 'test-token');
      expect(service.getToken()).toBe('test-token');
    });
  });
});
