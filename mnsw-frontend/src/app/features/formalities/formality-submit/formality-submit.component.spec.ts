import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FormalitySubmitComponent } from './formality-submit.component';
import { AuthService } from '../../../core/auth/auth.service';

function setupTestBed(userRole: string = 'SCHEEPSAGENT') {
  const authServiceMock = {
    hasAnyRole: (...roles: string[]) => roles.includes(userRole),
    currentUser: () => ({ id: 'user-1', role: userRole }),
  };

  TestBed.configureTestingModule({
    imports: [FormalitySubmitComponent],
    providers: [
      { provide: AuthService, useValue: authServiceMock },
      provideRouter([]),
      provideNoopAnimations(),
    ],
  });
}

describe('FormalitySubmitComponent', () => {
  describe('weergave typekaarten', () => {
    it('toont alle 5 formality typekaarten', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      expect(component.typeCards).toHaveLength(5);
      const types = component.typeCards.map(c => c.type);
      expect(types).toContain('NOA');
      expect(types).toContain('NOS');
      expect(types).toContain('NOD');
      expect(types).toContain('VID');
      expect(types).toContain('SID');
    });

    it('geeft NOA, NOS en NOD correct Nederlandse titels', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      const noa = component.typeCards.find(c => c.type === 'NOA');
      const nos = component.typeCards.find(c => c.type === 'NOS');
      const nod = component.typeCards.find(c => c.type === 'NOD');

      expect(noa?.title).toBe('Aankomstmelding');
      expect(nos?.title).toBe('Zeilvaardigheidsverklaring');
      expect(nod?.title).toBe('Vertrekmelding');
    });

    it('bevat een Nederlandse omschrijving per kaart', () => {
      setupTestBed();
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      component.typeCards.forEach(card => {
        expect(card.description).toBeTruthy();
        expect(card.description.length).toBeGreaterThan(10);
      });
    });
  });

  describe('navigatie bij klikken', () => {
    it('navigeert naar /formalities/new/noa bij klikken op NOA kaart', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.navigateTo('NOA', true);

      expect(navigateSpy).toHaveBeenCalledWith(['/formalities/new', 'noa']);
    });

    it('navigeert naar /formalities/new/nos bij klikken op NOS kaart', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.navigateTo('NOS', true);

      expect(navigateSpy).toHaveBeenCalledWith(['/formalities/new', 'nos']);
    });

    it('navigeert naar /formalities/new/nod bij klikken op NOD kaart', () => {
      setupTestBed('LADINGAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.navigateTo('NOD', true);

      expect(navigateSpy).toHaveBeenCalledWith(['/formalities/new', 'nod']);
    });

    it('navigeert niet als kaart uitgeschakeld is', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.navigateTo('NOA', false);

      expect(navigateSpy).not.toHaveBeenCalled();
    });

    it('navigeert niet als gebruiker geen indienrechten heeft', () => {
      setupTestBed('HAVENAUTORITEIT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;
      const router = TestBed.inject(Router);
      const navigateSpy = jest.spyOn(router, 'navigate');

      component.navigateTo('NOA', true);

      expect(navigateSpy).not.toHaveBeenCalled();
    });
  });

  describe('rolgebaseerde toegang', () => {
    it('canSubmit is true voor SCHEEPSAGENT', () => {
      setupTestBed('SCHEEPSAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      expect(component.canSubmit()).toBe(true);
    });

    it('canSubmit is true voor LADINGAGENT', () => {
      setupTestBed('LADINGAGENT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      expect(component.canSubmit()).toBe(true);
    });

    it('canSubmit is true voor ADMIN', () => {
      setupTestBed('ADMIN');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      expect(component.canSubmit()).toBe(true);
    });

    it('canSubmit is false voor HAVENAUTORITEIT', () => {
      setupTestBed('HAVENAUTORITEIT');
      const fixture = TestBed.createComponent(FormalitySubmitComponent);
      const component = fixture.componentInstance;

      expect(component.canSubmit()).toBe(false);
    });
  });
});
