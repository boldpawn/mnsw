import { Component, inject, input, OnInit, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FormalityService, SubmitFormalityRequest } from '../../../../core/api/formality.service';
import { VidPayload } from '../../../../core/models/formality.model';
import { VisitFormSectionComponent, createVisitFormGroup } from '../../../../shared/components/visit-form-section/visit-form-section.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../../shared/components/error-display/error-display.component';

export interface VidFormGroup {
  visit: FormGroup;
  callSign: FormControl<string | null>;
  mmsi: FormControl<string | null>;
  certificateNationality: FormControl<string | null>;
  grossTonnage: FormControl<number | null>;
  netTonnage: FormControl<number | null>;
  deadweight: FormControl<number | null>;
  lengthOverall: FormControl<number | null>;
  shipType: FormControl<string | null>;
}

export function createVidFormGroup(): FormGroup<VidFormGroup> {
  return new FormGroup<VidFormGroup>({
    visit: createVisitFormGroup(),
    callSign: new FormControl<string | null>(null),
    mmsi: new FormControl<string | null>(null, [Validators.pattern(/^\d{9}$/)]),
    certificateNationality: new FormControl<string | null>(null, [Validators.pattern(/^[A-Z]{2}$/)]),
    grossTonnage: new FormControl<number | null>(null, [Validators.min(0)]),
    netTonnage: new FormControl<number | null>(null, [Validators.min(0)]),
    deadweight: new FormControl<number | null>(null, [Validators.min(0)]),
    lengthOverall: new FormControl<number | null>(null, [Validators.min(0), Validators.max(500)]),
    shipType: new FormControl<string | null>(null),
  });
}

@Component({
  selector: 'app-vid-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    VisitFormSectionComponent,
    PageHeaderComponent,
    ErrorDisplayComponent,
  ],
  templateUrl: './vid-form.component.html',
  styleUrl: './vid-form.component.scss',
})
export class VidFormComponent implements OnInit {
  /** When provided, the component operates in prefill/correction mode */
  prefillData = input<VidPayload | null>(null);
  /** When provided, submit goes to the correction endpoint */
  correctionId = input<string | null>(null);

  private formalityService = inject(FormalityService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  form = createVidFormGroup();
  loading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    if (this.correctionId()) {
      // In correction mode the visit section is not shown and not submitted
      this.form.get('visit')?.disable();
    }

    const data = this.prefillData();
    if (data) {
      this.form.patchValue({
        callSign: data.callSign ?? null,
        mmsi: data.mmsi ?? null,
        certificateNationality: data.certificateNationality ?? null,
        grossTonnage: data.grossTonnage ?? null,
        netTonnage: data.netTonnage ?? null,
        deadweight: data.deadweight ?? null,
        lengthOverall: data.lengthOverall ?? null,
        shipType: data.shipType ?? null,
      });
    }
  }

  get visit(): FormGroup {
    return this.form.get('visit') as FormGroup;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const payload: VidPayload = {
      callSign: this.form.value.callSign ?? undefined,
      mmsi: this.form.value.mmsi ?? undefined,
      certificateNationality: this.form.value.certificateNationality ?? undefined,
      grossTonnage: this.form.value.grossTonnage ?? undefined,
      netTonnage: this.form.value.netTonnage ?? undefined,
      deadweight: this.form.value.deadweight ?? undefined,
      lengthOverall: this.form.value.lengthOverall ?? undefined,
      shipType: this.form.value.shipType ?? undefined,
    };

    const corrId = this.correctionId();
    if (corrId) {
      this.formalityService.correct(corrId, { payload }).subscribe({
        next: result => {
          this.loading.set(false);
          this.snackBar.open('Correctie ingediend', 'Sluiten', { duration: 4000 });
          this.router.navigate(['/formalities', corrId]);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Fout bij indienen van de correctie. Probeer het opnieuw.');
        },
      });
    } else {
      const request: SubmitFormalityRequest = {
        visitId: '',
        type: 'VID',
        payload,
      };
      this.formalityService.submit(request).subscribe({
        next: result => {
          this.loading.set(false);
          this.snackBar.open('VID ingediend', 'Sluiten', { duration: 4000 });
          this.router.navigate(['/formalities', result.formalityId]);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Fout bij indienen van de VID. Probeer het opnieuw.');
        },
      });
    }
  }
}
