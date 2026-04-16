import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FormalityService } from '../../../core/api/formality.service';
import { NosPayload } from '../../../core/models/formality.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../shared/components/error-display/error-display.component';
import { VisitFormSectionComponent, createVisitFormGroup } from '../../../shared/components/visit-form-section/visit-form-section.component';

const LOCODE_PATTERN = /^[A-Z]{2}[A-Z0-9]{3}$/;
const COUNTRY_PATTERN = /^[A-Z]{2}$/;

@Component({
  selector: 'app-nos-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    PageHeaderComponent,
    ErrorDisplayComponent,
    VisitFormSectionComponent
  ],
  templateUrl: './nos-form.component.html',
  styleUrl: './nos-form.component.scss'
})
export class NosFormComponent {
  private formalityService = inject(FormalityService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  loading = signal(false);
  error = signal<string | null>(null);

  form = new FormGroup({
    visit: createVisitFormGroup(),
    actualSailing: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    nextPortLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(LOCODE_PATTERN)]
    }),
    destinationCountry: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(COUNTRY_PATTERN)]
    })
  });

  get visitGroup(): FormGroup {
    return this.form.get('visit') as FormGroup;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: NosPayload = {
      actualSailing: new Date(this.form.value.actualSailing!).toISOString(),
      ...(this.form.value.nextPortLocode ? { nextPortLocode: this.form.value.nextPortLocode } : {}),
      ...(this.form.value.destinationCountry ? { destinationCountry: this.form.value.destinationCountry } : {})
    };

    this.loading.set(true);
    this.error.set(null);

    this.formalityService.submit({
      visitId: '',
      type: 'NOS',
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
