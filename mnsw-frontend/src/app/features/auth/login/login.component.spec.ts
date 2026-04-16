import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/auth/auth.service';
import { provideAnimations } from '@angular/platform-browser/animations';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authService: jest.Mocked<Partial<AuthService>>;
  let router: Router;

  beforeEach(async () => {
    authService = {
      login: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        provideAnimations(),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form validation', () => {
    it('form is invalid when empty', () => {
      expect(component.form.valid).toBe(false);
    });

    it('email field is invalid when empty', () => {
      const emailCtrl = component.form.get('email')!;
      expect(emailCtrl.hasError('required')).toBe(true);
    });

    it('email field is invalid when not a valid email', () => {
      const emailCtrl = component.form.get('email')!;
      emailCtrl.setValue('not-an-email');
      expect(emailCtrl.hasError('email')).toBe(true);
    });

    it('password field is invalid when empty', () => {
      const pwCtrl = component.form.get('password')!;
      expect(pwCtrl.hasError('required')).toBe(true);
    });

    it('form is valid with correct email and password', () => {
      component.form.setValue({ email: 'user@example.com', password: 'geheim' });
      expect(component.form.valid).toBe(true);
    });
  });

  describe('submit()', () => {
    it('does not call authService.login when form is invalid', () => {
      component.submit();
      expect(authService.login).not.toHaveBeenCalled();
    });

    it('calls authService.login with email and password on valid form', () => {
      (authService.login as jest.Mock).mockReturnValue(of({ token: 'tok', expiresAt: '', user: {} as any }));
      component.form.setValue({ email: 'user@example.com', password: 'geheim' });
      component.submit();
      expect(authService.login).toHaveBeenCalledWith('user@example.com', 'geheim');
    });

    it('navigates to /formalities on successful login', async () => {
      (authService.login as jest.Mock).mockReturnValue(of({ token: 'tok', expiresAt: '', user: {} as any }));
      const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
      component.form.setValue({ email: 'user@example.com', password: 'geheim' });
      component.submit();
      expect(navigateSpy).toHaveBeenCalledWith(['/formalities']);
    });

    it('sets error message and clears loading on failed login', () => {
      (authService.login as jest.Mock).mockReturnValue(throwError(() => new Error('401')));
      component.form.setValue({ email: 'user@example.com', password: 'fout' });
      component.submit();
      expect(component.error()).toBe('Ongeldige gebruikersnaam of wachtwoord.');
      expect(component.loading()).toBe(false);
    });
  });
});
