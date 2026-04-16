import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FormalityService } from '../../../core/api/formality.service';
import { NoaPayload } from '../../../core/models/formality.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../shared/components/error-display/error-display.component';
import { VisitFormSectionComponent, createVisitFormGroup } from '../../../shared/components/visit-form-section/visit-form-section.component';

const LOCODE_PATTERN = /^[A-Z]{2}[A-Z0-9]{3}$/;

@Component({
  selector: 'app-noa-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
    ErrorDisplayComponent,
    VisitFormSectionComponent
  ],
  templateUrl: './noa-form.component.html',
  styleUrl: './noa-form.component.scss'
})
export class NoaFormComponent {
  private formalityService = inject(FormalityService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  error = signal<string | null>(null);

  form = new FormGroup({
    visit: createVisitFormGroup(),
    expectedArrival: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    lastPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)]
    }),
    nextPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)]
    }),
    purposeOfCall: new FormControl('', { nonNullable: true }),
    personsOnBoard: new FormControl<number | null>(null, {
      validators: [Validators.min(0)]
    }),
    maxStaticDraught: new FormControl<number | null>(null, {
      validators: [Validators.min(0), Validators.max(50)]
    }),
    dangerousGoods: new FormControl(false, { nonNullable: true }),
    wasteDelivery: new FormControl(false, { nonNullable: true })
  });

  get visitGroup(): FormGroup {
    return this.form.get('visit') as FormGroup;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const visit = this.form.value.visit!;
    const payload: NoaPayload = {
      expectedArrival: new Date(this.form.value.expectedArrival!).toISOString(),
      ...(this.form.value.lastPortLocode ? { lastPortLocode: this.form.value.lastPortLocode } : {}),
      ...(this.form.value.nextPortLocode ? { nextPortLocode: this.form.value.nextPortLocode } : {}),
      ...(this.form.value.purposeOfCall ? { purposeOfCall: this.form.value.purposeOfCall } : {}),
      ...(this.form.value.personsOnBoard != null ? { personsOnBoard: this.form.value.personsOnBoard } : {}),
      ...(this.form.value.maxStaticDraught != null ? { maxStaticDraught: this.form.value.maxStaticDraught } : {}),
      dangerousGoods: this.form.value.dangerousGoods ?? false,
      wasteDelivery: this.form.value.wasteDelivery ?? false
    };

    this.loading.set(true);
    this.error.set(null);

    this.formalityService.submit({
      visitId: '',
      type: 'NOA',
      payload
    }).subscribe({
      next: result => {
        this.loading.set(false);
        this.snackBar.open('Formality ingediend', 'Sluiten', { duration: 4000 });
        this.router.navigate(['/formalities', result.formalityId]);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Er is een fout opgetreden bij het indienen van de formality. Probeer het opnieuw.');
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/formalities/new']);
  }
}
