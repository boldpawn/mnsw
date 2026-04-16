import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  Formality,
  FormalityPayload,
  NoaPayload,
  NodPayload,
  NosPayload,
  VidPayload,
  SidPayload,
} from '../../../core/models/formality.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../shared/components/error-display/error-display.component';
import { VidFormComponent } from '../formality-submit/vid-form/vid-form.component';
import { SidFormComponent } from '../formality-submit/sid-form/sid-form.component';

const LOCODE_PATTERN = /^[A-Z]{2}[A-Z0-9]{3}$/;

@Component({
  selector: 'app-formality-correct',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
    ErrorDisplayComponent,
    VidFormComponent,
    SidFormComponent,
  ],
  templateUrl: './formality-correct.component.html',
  styleUrl: './formality-correct.component.scss',
})
export class FormalityCorrectComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private formalityService = inject(FormalityService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  formality = signal<Formality | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  submitting = signal(false);
  submitError = signal<string | null>(null);

  formalityId = '';

  // NOA inline form
  noaForm = new FormGroup({
    expectedArrival: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    lastPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)],
    }),
    nextPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)],
    }),
    purposeOfCall: new FormControl('', { nonNullable: true }),
    personsOnBoard: new FormControl<number | null>(null, [Validators.min(0)]),
    maxStaticDraught: new FormControl<number | null>(null, [Validators.min(0), Validators.max(50)]),
    dangerousGoods: new FormControl(false, { nonNullable: true }),
    wasteDelivery: new FormControl(false, { nonNullable: true }),
  });

  // NOS inline form
  nosForm = new FormGroup({
    actualSailing: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    nextPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)],
    }),
    destinationCountry: new FormControl('', { nonNullable: true }),
  });

  // NOD inline form
  nodForm = new FormGroup({
    expectedDeparture: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    nextPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)],
    }),
    destinationCountry: new FormControl('', { nonNullable: true }),
    lastCargoOperations: new FormControl('', { nonNullable: true }),
  });

  get vidPayload(): VidPayload | null {
    const f = this.formality();
    return f?.type === 'VID' ? (f.payload as VidPayload) : null;
  }

  get sidPayload(): SidPayload | null {
    const f = this.formality();
    return f?.type === 'SID' ? (f.payload as SidPayload) : null;
  }

  ngOnInit(): void {
    this.formalityId = this.route.snapshot.paramMap.get('id')!;
    this.loading.set(true);
    this.formalityService.get(this.formalityId).subscribe({
      next: f => {
        this.loading.set(false);
        const currentUserId = this.authService.currentUser()?.id;
        if (currentUserId !== f.submitterId) {
          this.router.navigate(['/formalities', this.formalityId]);
          return;
        }
        this.formality.set(f);
        this.prefillForms(f);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Formality kon niet worden geladen. Controleer of het ID correct is.');
      },
    });
  }

  private prefillForms(f: Formality): void {
    if (!f.payload) return;

    if (f.type === 'NOA') {
      const p = f.payload as NoaPayload;
      this.noaForm.patchValue({
        expectedArrival: p.expectedArrival ?? '',
        lastPortLocode: p.lastPortLocode ?? '',
        nextPortLocode: p.nextPortLocode ?? '',
        purposeOfCall: p.purposeOfCall ?? '',
        personsOnBoard: p.personsOnBoard ?? null,
        maxStaticDraught: p.maxStaticDraught ?? null,
        dangerousGoods: p.dangerousGoods ?? false,
        wasteDelivery: p.wasteDelivery ?? false,
      });
    } else if (f.type === 'NOS') {
      const p = f.payload as NosPayload;
      this.nosForm.patchValue({
        actualSailing: p.actualSailing ?? '',
        nextPortLocode: p.nextPortLocode ?? '',
        destinationCountry: p.destinationCountry ?? '',
      });
    } else if (f.type === 'NOD') {
      const p = f.payload as NodPayload;
      this.nodForm.patchValue({
        expectedDeparture: p.expectedDeparture ?? '',
        nextPortLocode: p.nextPortLocode ?? '',
        destinationCountry: p.destinationCountry ?? '',
        lastCargoOperations: p.lastCargoOperations ?? '',
      });
    }
    // VID and SID are handled by child components via prefillData input
  }

  submitNoa(): void {
    if (this.noaForm.invalid) {
      this.noaForm.markAllAsTouched();
      return;
    }
    const v = this.noaForm.value;
    const payload: NoaPayload = {
      expectedArrival: new Date(v.expectedArrival!).toISOString(),
      ...(v.lastPortLocode ? { lastPortLocode: v.lastPortLocode } : {}),
      ...(v.nextPortLocode ? { nextPortLocode: v.nextPortLocode } : {}),
      ...(v.purposeOfCall ? { purposeOfCall: v.purposeOfCall } : {}),
      ...(v.personsOnBoard != null ? { personsOnBoard: v.personsOnBoard } : {}),
      ...(v.maxStaticDraught != null ? { maxStaticDraught: v.maxStaticDraught } : {}),
      dangerousGoods: v.dangerousGoods ?? false,
      wasteDelivery: v.wasteDelivery ?? false,
    };
    this.submitCorrection(payload);
  }

  submitNos(): void {
    if (this.nosForm.invalid) {
      this.nosForm.markAllAsTouched();
      return;
    }
    const v = this.nosForm.value;
    const payload: NosPayload = {
      actualSailing: new Date(v.actualSailing!).toISOString(),
      ...(v.nextPortLocode ? { nextPortLocode: v.nextPortLocode } : {}),
      ...(v.destinationCountry ? { destinationCountry: v.destinationCountry } : {}),
    };
    this.submitCorrection(payload);
  }

  submitNod(): void {
    if (this.nodForm.invalid) {
      this.nodForm.markAllAsTouched();
      return;
    }
    const v = this.nodForm.value;
    const payload: NodPayload = {
      expectedDeparture: new Date(v.expectedDeparture!).toISOString(),
      ...(v.nextPortLocode ? { nextPortLocode: v.nextPortLocode } : {}),
      ...(v.destinationCountry ? { destinationCountry: v.destinationCountry } : {}),
      ...(v.lastCargoOperations ? { lastCargoOperations: v.lastCargoOperations } : {}),
    };
    this.submitCorrection(payload);
  }

  private submitCorrection(payload: FormalityPayload): void {
    this.submitting.set(true);
    this.submitError.set(null);
    this.formalityService.correct(this.formalityId, { payload }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Correctie ingediend', 'Sluiten', { duration: 4000 });
        this.router.navigate(['/formalities', this.formalityId]);
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set('Fout bij indienen van de correctie. Probeer het opnieuw.');
      },
    });
  }
}
