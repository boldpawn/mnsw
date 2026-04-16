import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

import { FormalityCorrectComponent } from './formality-correct.component';
import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Formality, VidPayload, SidPayload } from '../../../core/models/formality.model';
import { environment } from '../../../../environments/environment';

const vidPayload: VidPayload = {
  callSign: 'PBAB',
  mmsi: '244123456',
  grossTonnage: 15000,
  netTonnage: 8000,
};

const sidPayload: SidPayload = {
  ispsLevel: 2,
  ssasActivated: true,
  shipToShipActivities: false,
  last10Ports: [{ locode: 'BEANR', arrival: '2026-04-01T10:00' }],
};

function buildVidFormality(submitterId = 'user-1'): Formality {
  return {
    id: 'formality-vid-1',
    visitId: 'visit-1',
    type: 'VID',
    version: 1,
    status: 'SUBMITTED',
    submitterId,
    messageIdentifier: 'MSG-VID-001',
    submittedAt: '2026-04-15T10:00:00Z',
    channel: 'WEB',
    payload: vidPayload,
  };
}

function buildSidFormality(submitterId = 'user-1'): Formality {
  return {
    id: 'formality-sid-1',
    visitId: 'visit-1',
    type: 'SID',
    version: 1,
    status: 'SUBMITTED',
    submitterId,
    messageIdentifier: 'MSG-SID-001',
    submittedAt: '2026-04-15T10:00:00Z',
    channel: 'WEB',
    payload: sidPayload,
  };
}

function buildNoaFormality(submitterId = 'user-1'): Formality {
  return {
    id: 'formality-noa-1',
    visitId: 'visit-1',
    type: 'NOA',
    version: 1,
    status: 'SUBMITTED',
    submitterId,
    messageIdentifier: 'MSG-NOA-001',
    submittedAt: '2026-04-15T10:00:00Z',
    channel: 'WEB',
    payload: { expectedArrival: '2026-04-20T08:00:00Z' },
  };
}

function buildActivatedRoute(id = 'formality-vid-1') {
  return {
    snapshot: {
      paramMap: convertToParamMap({ id }),
    },
  };
}

function setupTestBed(options: { userId?: string; formalityId?: string } = {}) {
  const { userId = 'user-1', formalityId = 'formality-vid-1' } = options;

  const authServiceMock = {
    currentUser: () => ({ id: userId, role: 'SCHEEPSAGENT' }),
  };

  const routerMock = { navigate: jest.fn() };
  const snackBarMock = { open: jest.fn() };

  TestBed.configureTestingModule({
    imports: [FormalityCorrectComponent, HttpClientTestingModule],
    providers: [
      FormalityService,
      { provide: AuthService, useValue: authServiceMock },
      { provide: ActivatedRoute, useValue: buildActivatedRoute(formalityId) },
      { provide: Router, useValue: routerMock },
      { provide: MatSnackBar, useValue: snackBarMock },
      provideNoopAnimations(),
    ],
  });

  return { routerMock, snackBarMock };
}

describe('FormalityCorrectComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock?.verify();
  });

  describe('laden bij init', () => {
    it('laadt de formality op basis van de route-parameter', () => {
      setupTestBed({ formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`);
      expect(req.request.method).toBe('GET');
      req.flush(buildVidFormality());

      expect(component.formality()).not.toBeNull();
      expect(component.formality()?.type).toBe('VID');
      expect(component.loading()).toBe(false);
    });

    it('toont foutmelding wanneer laden mislukt', () => {
      setupTestBed({ formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      expect(component.error()).toBe('Formality kon niet worden geladen. Controleer of het ID correct is.');
      expect(component.loading()).toBe(false);
    });

    it('zet loading op true tijdens het laden en false erna', () => {
      setupTestBed({ formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();

      expect(component.loading()).toBe(true);

      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality());

      expect(component.loading()).toBe(false);
    });
  });

  describe('submitter guard (toegangscontrole)', () => {
    it('laadt de formality wanneer de huidige gebruiker de indiener is', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality('user-1'));

      expect(component.formality()).not.toBeNull();
    });

    it('navigeert naar de detailpagina wanneer de huidige gebruiker NIET de indiener is', () => {
      const { routerMock } = setupTestBed({ userId: 'other-user', formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality('user-1'));

      expect(routerMock.navigate).toHaveBeenCalledWith(['/formalities', 'formality-vid-1']);
      expect(component.formality()).toBeNull();
    });

    it('stelt formality niet in wanneer toegang geweigerd', () => {
      setupTestBed({ userId: 'hacker-42', formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality('user-1'));

      expect(component.formality()).toBeNull();
    });
  });

  describe('vidPayload getter', () => {
    it('retourneert de VID-payload voor een VID-formality', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality());

      expect(component.vidPayload).toEqual(vidPayload);
    });

    it('retourneert null voor een niet-VID-formality', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-sid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-sid-1`).flush(buildSidFormality());

      expect(component.vidPayload).toBeNull();
    });
  });

  describe('sidPayload getter', () => {
    it('retourneert de SID-payload voor een SID-formality', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-sid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-sid-1`).flush(buildSidFormality());

      expect(component.sidPayload).toEqual(sidPayload);
    });

    it('retourneert null voor een niet-SID-formality', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-vid-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-vid-1`).flush(buildVidFormality());

      expect(component.sidPayload).toBeNull();
    });
  });

  describe('NOA correctie indienen', () => {
    it('verstuurt een POST naar corrections endpoint voor een NOA-correctie', () => {
      const { snackBarMock, routerMock } = setupTestBed({
        userId: 'user-1',
        formalityId: 'formality-noa-1',
      });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-noa-1`).flush(buildNoaFormality());

      component.noaForm.patchValue({ expectedArrival: '2026-05-01T10:00' });
      component.submitNoa();

      const correctionReq = httpMock.expectOne(
        `${environment.apiUrl}/formalities/formality-noa-1/corrections`
      );
      expect(correctionReq.request.method).toBe('POST');
      correctionReq.flush({
        formalityId: 'formality-noa-2',
        messageIdentifier: 'MSG-2',
        status: 'SUBMITTED',
        statusUrl: '/formalities/formality-noa-2',
      });

      expect(snackBarMock.open).toHaveBeenCalledWith('Correctie ingediend', 'Sluiten', { duration: 4000 });
      expect(routerMock.navigate).toHaveBeenCalledWith(['/formalities', 'formality-noa-1']);
    });

    it('toont foutmelding wanneer NOA-correctie mislukt', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-noa-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-noa-1`).flush(buildNoaFormality());

      component.noaForm.patchValue({ expectedArrival: '2026-05-01T10:00' });
      component.submitNoa();

      const correctionReq = httpMock.expectOne(
        `${environment.apiUrl}/formalities/formality-noa-1/corrections`
      );
      correctionReq.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      expect(component.submitError()).toBe('Fout bij indienen van de correctie. Probeer het opnieuw.');
      expect(component.submitting()).toBe(false);
    });

    it('markeert het NOA-formulier als aangeraakt wanneer het ongeldig is', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-noa-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-noa-1`).flush(buildNoaFormality());

      // expectedArrival is verplicht — laat het leeg
      component.noaForm.get('expectedArrival')?.setValue('');
      component.submitNoa();

      expect(component.noaForm.get('expectedArrival')?.touched).toBe(true);
    });
  });

  describe('prefill van NOA-formulier', () => {
    it('vult het NOA-formulier in met de bestaande payload', () => {
      setupTestBed({ userId: 'user-1', formalityId: 'formality-noa-1' });
      const fixture = TestBed.createComponent(FormalityCorrectComponent);
      httpMock = TestBed.inject(HttpTestingController);
      const component = fixture.componentInstance;

      const noaFormality = buildNoaFormality();
      noaFormality.payload = {
        expectedArrival: '2026-04-20T08:00:00Z',
        lastPortLocode: 'BEANR',
        nextPortLocode: 'DEHAM',
        purposeOfCall: 'Lossen',
        personsOnBoard: 25,
        dangerousGoods: true,
        wasteDelivery: false,
      };

      fixture.detectChanges();
      httpMock.expectOne(`${environment.apiUrl}/formalities/formality-noa-1`).flush(noaFormality);

      expect(component.noaForm.get('expectedArrival')?.value).toBe('2026-04-20T08:00:00Z');
      expect(component.noaForm.get('lastPortLocode')?.value).toBe('BEANR');
      expect(component.noaForm.get('personsOnBoard')?.value).toBe(25);
      expect(component.noaForm.get('dangerousGoods')?.value).toBe(true);
    });
  });
});
