import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FormalityListComponent } from './formality-list.component';
import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Formality, Page } from '../../../core/models/formality.model';
import { environment } from '../../../../environments/environment';

const mockFormality: Formality = {
  id: 'formality-1',
  visitId: 'visit-1',
  type: 'NOA',
  version: 1,
  status: 'SUBMITTED',
  submitterId: 'user-1',
  submitterName: 'Jan Jansen',
  messageIdentifier: 'MSG-001',
  submittedAt: '2026-04-15T10:00:00Z',
  channel: 'WEB',
  vessel: { imoNumber: '9234567', vesselName: 'MV Rotterdam', portLocode: 'NLRTM' },
};

const mockPage: Page<Formality> = {
  content: [mockFormality],
  totalElements: 1,
  totalPages: 1,
  page: 0,
  size: 20,
};

const emptyPage: Page<Formality> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 20,
};

function setupTestBed(userRole: string = 'SCHEEPSAGENT') {
  const authServiceMock = {
    hasAnyRole: (...roles: string[]) => roles.includes(userRole),
    currentUser: () => ({ id: 'user-1', role: userRole }),
  };

  TestBed.configureTestingModule({
    imports: [FormalityListComponent, HttpClientTestingModule],
    providers: [
      FormalityService,
      { provide: AuthService, useValue: authServiceMock },
      provideRouter([]),
      provideNoopAnimations(),
    ],
  });
}

describe('FormalityListComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock?.verify();
  });

  describe('laden bij init', () => {
    it('laadt formalities bij initialisatie', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`);
      req.flush(mockPage);

      expect(component.formalities()).toEqual([mockFormality]);
      expect(component.totalElements()).toBe(1);
      expect(component.loading()).toBe(false);
    });

    it('toont foutmelding bij mislukt laden', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      expect(component.error()).toBe('Fout bij laden van formalities.');
      expect(component.loading()).toBe(false);
    });
  });

  describe('filteren op type', () => {
    it('stuurt typefilter mee bij selectiewijziging', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      component.typeFilter.set('NOA');
      component.applyFilters();

      const req = httpMock.expectOne(r =>
        r.url === `${environment.apiUrl}/formalities` && r.params.get('type') === 'NOA'
      );
      req.flush(mockPage);

      expect(component.pageIndex()).toBe(0);
    });
  });

  describe('rolgebaseerde weergave', () => {
    it('toont "Nieuwe formality" knop voor SCHEEPSAGENT', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.canSubmit()).toBe(true);
    });

    it('toont "Nieuwe formality" knop voor LADINGAGENT', () => {
      setupTestBed('LADINGAGENT');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.canSubmit()).toBe(true);
    });

    it('verbergt "Nieuwe formality" knop voor HAVENAUTORITEIT', () => {
      setupTestBed('HAVENAUTORITEIT');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.canSubmit()).toBe(false);
    });

    it('bevat geen submitterName kolom voor SCHEEPSAGENT', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.displayedColumns()).not.toContain('submitterName');
    });

    it('bevat submitterName kolom voor HAVENAUTORITEIT', () => {
      setupTestBed('HAVENAUTORITEIT');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.displayedColumns()).toContain('submitterName');
    });

    it('bevat submitterName kolom voor ADMIN', () => {
      setupTestBed('ADMIN');
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      expect(component.displayedColumns()).toContain('submitterName');
    });
  });

  describe('lege staat', () => {
    it('toont lege staat wanneer formalities leeg zijn', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(emptyPage);

      expect(component.formalities()).toHaveLength(0);
      expect(component.totalElements()).toBe(0);
    });
  });

  describe('filters wissen', () => {
    it('wist type- en statusfilter en herlaadt', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalityListComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(mockPage);

      component.typeFilter.set('NOA');
      component.statusFilter.set('SUBMITTED');
      component.resetFilters();

      httpMock.expectOne(r => r.url === `${environment.apiUrl}/formalities`).flush(emptyPage);

      expect(component.typeFilter()).toBe('');
      expect(component.statusFilter()).toBe('');
    });
  });
});
