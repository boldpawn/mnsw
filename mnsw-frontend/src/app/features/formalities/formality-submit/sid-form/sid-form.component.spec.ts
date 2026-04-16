import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SidFormComponent } from './sid-form.component';
import { FormalityService } from '../../../../core/api/formality.service';
import { SidPayload } from '../../../../core/models/formality.model';
import { environment } from '../../../../../environments/environment';

function setupTestBed() {
  const snackBarMock = { open: jest.fn() };
  const routerMock = { navigate: jest.fn() };

  TestBed.configureTestingModule({
    imports: [SidFormComponent, HttpClientTestingModule],
    providers: [
      FormalityService,
      { provide: MatSnackBar, useValue: snackBarMock },
      { provide: Router, useValue: routerMock },
      provideRouter([]),
      provideNoopAnimations(),
    ],
  });

  return { snackBarMock, routerMock };
}

describe('SidFormComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock?.verify();
  });

  describe('FormArray: last10Ports', () => {
    it('begint met een lege FormArray', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.portCalls.length).toBe(0);
    });

    it('voegt een haven-rij toe via addPortCall()', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      component.addPortCall();

      expect(component.portCalls.length).toBe(1);
    });

    it('voegt meerdere haven-rijen toe', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      component.addPortCall();
      component.addPortCall();
      component.addPortCall();

      expect(component.portCalls.length).toBe(3);
    });

    it('verwijdert een haven-rij via removePortCall()', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      component.addPortCall();
      component.addPortCall();
      component.removePortCall(0);

      expect(component.portCalls.length).toBe(1);
    });

    it('staat maximaal 10 haven-rijen toe', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      for (let i = 0; i < 10; i++) {
        component.addPortCall();
      }

      expect(component.portCalls.length).toBe(10);
      expect(component.canAddPortCall).toBe(false);
    });

    it('voegt geen haven-rij toe wanneer het maximum van 10 bereikt is', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      for (let i = 0; i < 10; i++) {
        component.addPortCall();
      }

      // Probeer een 11e toe te voegen — dit zou geen effect mogen hebben
      component.addPortCall();

      expect(component.portCalls.length).toBe(10);
    });

    it('rapporteert canAddPortCall als true wanneer er minder dan 10 rijen zijn', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      component.addPortCall();

      expect(component.canAddPortCall).toBe(true);
    });

    it('rapporteert canAddPortCall als false bij exact 10 rijen', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      for (let i = 0; i < 10; i++) {
        component.addPortCall();
      }

      expect(component.canAddPortCall).toBe(false);
    });

    it('staat opnieuw toevoegen toe nadat een rij is verwijderd vanuit het maximum', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      for (let i = 0; i < 10; i++) {
        component.addPortCall();
      }
      component.removePortCall(0);

      expect(component.portCalls.length).toBe(9);
      expect(component.canAddPortCall).toBe(true);

      component.addPortCall();
      expect(component.portCalls.length).toBe(10);
    });
  });

  describe('formulier initialisatie', () => {
    it('initialiseert het formulier met ispsLevel 1 als standaard', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.form.get('ispsLevel')?.value).toBe(1);
    });

    it('initialiseert ssasActivated als false', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.form.get('ssasActivated')?.value).toBe(false);
    });

    it('initialiseert shipToShipActivities als false', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      expect(component.form.get('shipToShipActivities')?.value).toBe(false);
    });
  });

  describe('formulier validatie', () => {
    it('is geldig wanneer ispsLevel is opgegeven', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      component.form.patchValue({ ispsLevel: 2 });

      expect(component.form.get('ispsLevel')?.valid).toBe(true);
    });

    it('markeert het formulier als aangeraakt bij onSubmit wanneer ongeldig', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      fixture.detectChanges();

      // Maak visit-velden ongeldig door required velden leeg te laten
      const visitGroup = component.form.get('visit');
      visitGroup?.get('imoNumber')?.setValue('');
      visitGroup?.get('vesselName')?.setValue('');

      component.onSubmit();

      expect(component.form.touched).toBe(true);
    });
  });

  describe('prefill vanuit prefillData input', () => {
    it('vult het formulier in met prefillData wanneer opgegeven', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;

      const prefill: SidPayload = {
        ispsLevel: 2,
        ssasActivated: true,
        shipToShipActivities: true,
        securityDeclaration: 'Testverklaring',
        designatedAuthority: 'Rijkswaterstaat',
        last10Ports: [
          { locode: 'BEANR', arrival: '2026-04-01T10:00', departure: '2026-04-02T10:00' },
        ],
      };

      fixture.componentRef.setInput('prefillData', prefill);
      fixture.detectChanges();

      expect(component.form.get('ispsLevel')?.value).toBe(2);
      expect(component.form.get('ssasActivated')?.value).toBe(true);
      expect(component.form.get('securityDeclaration')?.value).toBe('Testverklaring');
      expect(component.portCalls.length).toBe(1);
      expect(component.portCalls.at(0).get('locode')?.value).toBe('BEANR');
    });
  });

  describe('indienen', () => {
    it('verstuurt een POST-verzoek bij geldig formulier (nieuwe SID)', () => {
      const { snackBarMock, routerMock } = setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      httpMock = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      component.form.patchValue({ ispsLevel: 1 });
      const visitGroup = component.form.get('visit')!;
      visitGroup.patchValue({
        imoNumber: 'IMO1234567',
        vesselName: 'MV Test',
        portLocode: 'NLRTM',
        eta: '2026-05-01T08:00',
      });

      component.onSubmit();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities`);
      expect(req.request.method).toBe('POST');
      req.flush({ formalityId: 'sid-1', messageIdentifier: 'MSG-SID', status: 'SUBMITTED', statusUrl: '/formalities/sid-1' });

      expect(snackBarMock.open).toHaveBeenCalledWith('SID ingediend', 'Sluiten', { duration: 4000 });
      expect(routerMock.navigate).toHaveBeenCalledWith(['/formalities', 'sid-1']);
    });

    it('toont foutmelding wanneer het indienen mislukt', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(SidFormComponent);
      const component = fixture.componentInstance;
      httpMock = TestBed.inject(HttpTestingController);
      fixture.detectChanges();

      component.form.patchValue({ ispsLevel: 1 });
      const visitGroup = component.form.get('visit')!;
      visitGroup.patchValue({
        imoNumber: 'IMO1234567',
        vesselName: 'MV Test',
        portLocode: 'NLRTM',
        eta: '2026-05-01T08:00',
      });

      component.onSubmit();

      const req = httpMock.expectOne(`${environment.apiUrl}/formalities`);
      req.flush('Server error', { status: 500, statusText: 'Internal Server Error' });

      expect(component.error()).toBe('Fout bij indienen van de SID. Probeer het opnieuw.');
      expect(component.loading()).toBe(false);
    });
  });
});
