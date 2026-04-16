import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FormalityDetailComponent } from './formality-detail.component';
import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Formality } from '../../../core/models/formality.model';
import { environment } from '../../../../environments/environment';

const mockFormality: Formality = {
  id: 'formality-1',
  visitId: 'visit-1',
  type: 'NOA',
  version: 2,
  status: 'SUBMITTED',
  submitterId: 'user-1',
  submitterName: 'Jan Jansen',
  messageIdentifier: 'MSG-001',
  submittedAt: '2026-04-15T10:00:00Z',
  channel: 'WEB',
  vessel: { imoNumber: '9234567', vesselName: 'MV Rotterdam', portLocode: 'NLRTM' },
  versionHistory: [
    { id: 'formality-0', version: 1, status: 'SUPERSEDED', submittedAt: '2026-04-14T09:00:00Z' },
    { id: 'formality-1', version: 2, status: 'SUBMITTED', submittedAt: '2026-04-15T10:00:00Z' },
  ],
};

function buildActivatedRoute(id: string = 'formality-1') {
  return {
    snapshot: {
      paramMap: convertToParamMap({ id }),
    },
  };
}

function setupTestBed(options: { userId?: string; userRole?: string } = {}) {
  const { userId = 'user-1', userRole = 'SCHEEPSAGENT' } = options;

  const authServiceMock = {
    hasAnyRole: (...roles: string[]) => roles.includes(userRole),
    currentUser: () => ({ id: userId, role: userRole }),
  };

  const routerMock = { navigate: jest.fn() };

  TestBed.configureTestingModule({
    imports: [FormalityDetailComponent, HttpClientTestingModule],
    providers: [
      FormalityService,
      { provide: AuthService, useValue: authServiceMock },
      { provide: ActivatedRoute, useValue: buildActivatedRoute() },
      { provide: Router, useValue: routerMock },
      provideNoopAnimations(),
    ],
  });
}

describe('FormalityDetailComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock?.verify();
  });

  describe('laden op basis van route param', () => {
    it('laadt formality op basis van de route-parameter', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockFormality);

      expect(component.formality()).toEqual(mockFormality);
      expect(component.loading()).toBe(false);
    });

    it('toont foutmelding als formality niet gevonden wordt', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`);
      req.flush('Niet gevonden', { status: 404, statusText: 'Not Found' });

      expect(component.error()).toBe('Formality niet gevonden.');
      expect(component.loading()).toBe(false);
    });
  });

  describe('correctie-knop zichtbaarheid', () => {
    it('toont correctie-knop voor de indiener bij niet-SUPERSEDED status', () => {
      setupTestBed({ userId: 'user-1' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      expect(component.isSubmitter()).toBe(true);
      expect(component.canCorrect()).toBe(true);
    });

    it('verbergt correctie-knop voor een andere gebruiker', () => {
      setupTestBed({ userId: 'user-2' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      expect(component.isSubmitter()).toBe(false);
      expect(component.canCorrect()).toBe(false);
    });

    it('verbergt correctie-knop voor de indiener als status SUPERSEDED is', () => {
      setupTestBed({ userId: 'user-1' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush({
        ...mockFormality,
        status: 'SUPERSEDED',
      });

      expect(component.canCorrect()).toBe(false);
    });
  });

  describe('goedkeur- en afwijsknoppen voor HAVENAUTORITEIT', () => {
    it('toont goedkeur- en afwijsknoppen voor HAVENAUTORITEIT', () => {
      setupTestBed({ userId: 'auth-user', userRole: 'HAVENAUTORITEIT' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      expect(component.canApprove()).toBe(true);
    });

    it('toont goedkeur- en afwijsknoppen voor ADMIN', () => {
      setupTestBed({ userId: 'admin-user', userRole: 'ADMIN' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      expect(component.canApprove()).toBe(true);
    });

    it('verbergt goedkeur- en afwijsknoppen voor SCHEEPSAGENT', () => {
      setupTestBed({ userId: 'user-1', userRole: 'SCHEEPSAGENT' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      expect(component.canApprove()).toBe(false);
    });
  });

  describe('goedkeuren actie', () => {
    it('roept approve aan en werkt formality bij', () => {
      setupTestBed({ userId: 'auth-user', userRole: 'HAVENAUTORITEIT' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      component.approve();

      const approveReq = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/approve`);
      expect(approveReq.request.method).toBe('PUT');
      const acceptedFormality = { ...mockFormality, status: 'ACCEPTED' as const };
      approveReq.flush(acceptedFormality);

      expect(component.formality()?.status).toBe('ACCEPTED');
      expect(component.actionError()).toBeNull();
    });

    it('toont foutmelding als goedkeuren mislukt', () => {
      setupTestBed({ userId: 'auth-user', userRole: 'HAVENAUTORITEIT' });
      const fixture = TestBed.createComponent(FormalityDetailComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1`).flush(mockFormality);

      component.approve();

      const approveReq = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-1/approve`);
      approveReq.flush('Error', { status: 500, statusText: 'Server Error' });

      expect(component.actionError()).toBe('Fout bij goedkeuren.');
    });
  });
});
