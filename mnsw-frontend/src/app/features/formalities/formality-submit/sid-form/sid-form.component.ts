import { Component, inject, input, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { FormalityService, SubmitFormalityRequest } from '../../../../core/api/formality.service';
import { PortCall, SidPayload } from '../../../../core/models/formality.model';
import { VisitFormSectionComponent, createVisitFormGroup } from '../../../../shared/components/visit-form-section/visit-form-section.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../../shared/components/error-display/error-display.component';

const MAX_PORT_CALLS = 10;
const LOCODE_PATTERN = /^[A-Z]{2}[A-Z0-9]{3}$/;

export interface PortCallGroup {
  locode: FormControl<string>;
  arrival: FormControl<string | null>;
  departure: FormControl<string | null>;
}

function createPortCallGroup(data?: Partial<PortCall>): FormGroup<PortCallGroup> {
  return new FormGroup<PortCallGroup>({
    locode: new FormControl(data?.locode ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(LOCODE_PATTERN)],
    }),
    arrival: new FormControl<string | null>(data?.arrival ?? null),
    departure: new FormControl<string | null>(data?.departure ?? null),
  });
}

export interface SidFormGroup {
  visit: FormGroup;
  ispsLevel: FormControl<number>;
  ssasActivated: FormControl<boolean>;
  shipToShipActivities: FormControl<boolean>;
  securityDeclaration: FormControl<string | null>;
  designatedAuthority: FormControl<string | null>;
  last10Ports: FormArray<FormGroup<PortCallGroup>>;
}

export function createSidFormGroup(): FormGroup<SidFormGroup> {
  return new FormGroup<SidFormGroup>({
    visit: createVisitFormGroup(),
    ispsLevel: new FormControl<number>(1, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    ssasActivated: new FormControl<boolean>(false, { nonNullable: true }),
    shipToShipActivities: new FormControl<boolean>(false, { nonNullable: true }),
    securityDeclaration: new FormControl<string | null>(null),
    designatedAuthority: new FormControl<string | null>(null),
    last10Ports: new FormArray<FormGroup<PortCallGroup>>([]),
  });
}

@Component({
  selector: 'app-sid-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSnackBarModule,
    VisitFormSectionComponent,
    PageHeaderComponent,
    ErrorDisplayComponent,
  ],
  templateUrl: './sid-form.component.html',
  styleUrl: './sid-form.component.scss',
})
export class SidFormComponent implements OnInit {
  /** When provided, the component operates in prefill/correction mode */
  prefillData = input<SidPayload | null>(null);
  /** When provided, submit goes to the correction endpoint */
  correctionId = input<string | null>(null);

  private formalityService = inject(FormalityService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  form = createSidFormGroup();
  loading = signal(false);
  error = signal<string | null>(null);

  readonly maxPortCalls = MAX_PORT_CALLS;
  readonly ispsLevels = [1, 2, 3];

  get portCalls(): FormArray<FormGroup<PortCallGroup>> {
    return this.form.get('last10Ports') as FormArray<FormGroup<PortCallGroup>>;
  }

  get canAddPortCall(): boolean {
    return this.portCalls.length < MAX_PORT_CALLS;
  }

  ngOnInit(): void {
    if (this.correctionId()) {
      // In correction mode the visit section is not shown and not submitted
      this.form.get('visit')?.disable();
    }

    const data = this.prefillData();
    if (data) {
      this.form.patchValue({
        ispsLevel: data.ispsLevel,
        ssasActivated: data.ssasActivated ?? false,
        shipToShipActivities: data.shipToShipActivities ?? false,
        securityDeclaration: data.securityDeclaration ?? null,
        designatedAuthority: data.designatedAuthority ?? null,
      });

      if (data.last10Ports?.length) {
        data.last10Ports.forEach(pc => this.portCalls.push(createPortCallGroup(pc)));
      }
    }
  }

  addPortCall(): void {
    if (!this.canAddPortCall) return;
    this.portCalls.push(createPortCallGroup());
  }

  removePortCall(index: number): void {
    this.portCalls.removeAt(index);
  }

  getPortCallGroup(index: number): FormGroup<PortCallGroup> {
    return this.portCalls.at(index) as FormGroup<PortCallGroup>;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const v = this.form.value;
    const payload: SidPayload = {
      ispsLevel: v.ispsLevel as 1 | 2 | 3,
      ssasActivated: v.ssasActivated ?? false,
      shipToShipActivities: v.shipToShipActivities ?? false,
      securityDeclaration: v.securityDeclaration ?? undefined,
      designatedAuthority: v.designatedAuthority ?? undefined,
      last10Ports: (v.last10Ports ?? [])
        .filter((pc): pc is typeof pc & { locode: string } => !!pc.locode)
        .map(pc => ({
          locode: pc.locode!,
          arrival: pc.arrival ?? undefined,
          departure: pc.departure ?? undefined,
        })),
    };

    const corrId = this.correctionId();
    if (corrId) {
      this.formalityService.correct(corrId, { payload }).subscribe({
        next: () => {
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
        type: 'SID',
        payload,
      };
      this.formalityService.submit(request).subscribe({
        next: result => {
          this.loading.set(false);
          this.snackBar.open('SID ingediend', 'Sluiten', { duration: 4000 });
          this.router.navigate(['/formalities', result.formalityId]);
        },
        error: () => {
          this.loading.set(false);
          this.error.set('Fout bij indienen van de SID. Probeer het opnieuw.');
        },
      });
    }
  }
}
