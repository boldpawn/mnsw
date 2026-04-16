import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';

import { NoaFormComponent } from './noa-form.component';
import { FormalityService, SubmitFormalityResult } from '../../../core/api/formality.service';
import { environment } from '../../../../environments/environment';

const mockSubmitResult: SubmitFormalityResult = {
  formalityId: 'formality-123',
  messageIdentifier: 'MSG-001',
  status: 'SUBMITTED',
  statusUrl: '/formalities/formality-123',
  version: 1,
};

function setupTestBed() {
  TestBed.configureTestingModule({
    imports: [NoaFormComponent, HttpClientTestingModule],
    providers: [
      FormalityService,
      provideRouter([]),
      provideNoopAnimations(),
    ],
  });
}

describe('NoaFormComponent', () => {
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock?.verify();
  });

  describe('formuliervalidatie', () => {
    it('formulier is initieel ongeldig (verplichte velden ontbreken)', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;

      expect(component.form.valid).toBe(false);
    });

    it('imoNumber veld is verplicht', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const imoControl = component.form.get('visit.imoNumber')!;

      imoControl.setValue('');
      imoControl.markAsTouched();

      expect(imoControl.hasError('required')).toBe(true);
    });

    it('imoNumber moet IMO-patroon volgen', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const imoControl = component.form.get('visit.imoNumber')!;

      imoControl.setValue('123456');

      expect(imoControl.hasError('pattern')).toBe(true);
    });

    it('imoNumber is geldig met correct IMO-formaat', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const imoControl = component.form.get('visit.imoNumber')!;

      imoControl.setValue('IMO1234567');

      expect(imoControl.valid).toBe(true);
    });

    it('scheepsnaam is verplicht', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const vesselControl = component.form.get('visit.vesselName')!;

      vesselControl.setValue('');
      vesselControl.markAsTouched();

      expect(vesselControl.hasError('required')).toBe(true);
    });

    it('portLocode is verplicht en volgt LOCODE-patroon', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const portControl = component.form.get('visit.portLocode')!;

      portControl.setValue('RTM');
      portControl.markAsTouched();

      expect(portControl.hasError('pattern')).toBe(true);
    });

    it('portLocode is geldig voor NLRTM', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const portControl = component.form.get('visit.portLocode')!;

      portControl.setValue('NLRTM');

      expect(portControl.valid).toBe(true);
    });

    it('eta is verplicht', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const etaControl = component.form.get('visit.eta')!;

      etaControl.setValue('');
      etaControl.markAsTouched();

      expect(etaControl.hasError('required')).toBe(true);
    });

    it('expectedArrival is verplicht', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const arrivalControl = component.form.get('expectedArrival')!;

      arrivalControl.setValue('');
      arrivalControl.markAsTouched();

      expect(arrivalControl.hasError('required')).toBe(true);
    });

    it('personsOnBoard mag niet negatief zijn', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const personsControl = component.form.get('personsOnBoard')!;

      personsControl.setValue(-1);
      personsControl.markAsTouched();

      expect(personsControl.hasError('min')).toBe(true);
    });

    it('maxStaticDraught mag niet meer dan 50 zijn', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const draughtControl = component.form.get('maxStaticDraught')!;

      draughtControl.setValue(51);
      draughtControl.markAsTouched();

      expect(draughtControl.hasError('max')).toBe(true);
    });

    it('markeert alle velden als touched bij submittedpoging met ongeldig formulier', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;

      component.submit();

      expect(component.form.touched).toBe(true);
    });
  });

  describe('indienen', () => {
    it('roept formalityService.submit aan bij geldig formulier', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      const submitSpy = jest.spyOn(formalityService, 'submit').mockReturnValue(of(mockSubmitResult));

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
        dangerousGoods: false,
        wasteDelivery: false,
      });

      component.submit();

      expect(submitSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: 'NOA',
          payload: expect.objectContaining({
            dangerousGoods: false,
            wasteDelivery: false,
          }),
        })
      );
    });

    it('navigeert naar formality-detail na succesvolle indiening', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      const router = TestBed.inject(Router);
      jest.spyOn(formalityService, 'submit').mockReturnValue(of(mockSubmitResult));
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
      });

      component.submit();

      expect(navigateSpy).toHaveBeenCalledWith(['/formalities', 'formality-123']);
    });

    it('toont snackbar bericht na succesvolle indiening', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      const snackBar = TestBed.inject(MatSnackBar);
      jest.spyOn(formalityService, 'submit').mockReturnValue(of(mockSubmitResult));
      const snackBarSpy = jest.spyOn(snackBar, 'open');

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
      });

      component.submit();

      expect(snackBarSpy).toHaveBeenCalledWith('Formality ingediend', 'Sluiten', expect.any(Object));
    });

    it('toont foutmelding bij mislukte indiening', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      jest.spyOn(formalityService, 'submit').mockReturnValue(throwError(() => new Error('Server error')));

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
      });

      component.submit();

      expect(component.error()).toBeTruthy();
      expect(component.loading()).toBe(false);
    });

    it('stelt loading in op true tijdens indiening en false daarna', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      jest.spyOn(formalityService, 'submit').mockReturnValue(of(mockSubmitResult));

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
      });

      component.submit();

      expect(component.loading()).toBe(false);
    });
  });

  describe('annuleren', () => {
    it('navigeert terug naar /formalities/new bij annuleren', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.cancel();

      expect(navigateSpy).toHaveBeenCalledWith(['/formalities/new']);
    });
  });

  describe('payload opbouw', () => {
    it('neemt optionele velden niet op als leeg', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      let capturedPayload: any;
      jest.spyOn(formalityService, 'submit').mockImplementation(cmd => {
        capturedPayload = cmd.payload;
        return of(mockSubmitResult);
      });

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
        lastPortLocode: '',
        nextPortLocode: '',
        purposeOfCall: '',
        personsOnBoard: null,
        maxStaticDraught: null,
        dangerousGoods: false,
        wasteDelivery: false,
      });

      component.submit();

      expect(capturedPayload).not.toHaveProperty('lastPortLocode');
      expect(capturedPayload).not.toHaveProperty('nextPortLocode');
      expect(capturedPayload).not.toHaveProperty('purposeOfCall');
      expect(capturedPayload).not.toHaveProperty('personsOnBoard');
      expect(capturedPayload).not.toHaveProperty('maxStaticDraught');
    });

    it('neemt optionele velden op als ingevuld', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(NoaFormComponent);
      const component = fixture.componentInstance;
      const formalityService = TestBed.inject(FormalityService);
      let capturedPayload: any;
      jest.spyOn(formalityService, 'submit').mockImplementation(cmd => {
        capturedPayload = cmd.payload;
        return of(mockSubmitResult);
      });

      component.form.patchValue({
        visit: {
          imoNumber: 'IMO1234567',
          vesselName: 'MV Rotterdam',
          portLocode: 'NLRTM',
          eta: '2026-04-20T10:00',
          etd: null,
        },
        expectedArrival: '2026-04-20T10:00',
        lastPortLocode: 'GBSOU',
        purposeOfCall: 'laden',
        personsOnBoard: 25,
        dangerousGoods: true,
        wasteDelivery: false,
      });

      component.submit();

      expect(capturedPayload.lastPortLocode).toBe('GBSOU');
      expect(capturedPayload.purposeOfCall).toBe('laden');
      expect(capturedPayload.personsOnBoard).toBe(25);
      expect(capturedPayload.dangerousGoods).toBe(true);
    });
  });
});
