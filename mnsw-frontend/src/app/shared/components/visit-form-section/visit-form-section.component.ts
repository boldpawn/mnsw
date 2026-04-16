import { Component, input, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface VisitFormGroup {
  imoNumber: FormControl<string>;
  vesselName: FormControl<string>;
  portLocode: FormControl<string>;
  eta: FormControl<string>;
  etd: FormControl<string | null>;
}

export function createVisitFormGroup(): FormGroup<VisitFormGroup> {
  return new FormGroup<VisitFormGroup>({
    imoNumber: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^IMO\d{7}$/)]
    }),
    vesselName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    portLocode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Z]{2}[A-Z0-9]{3}$/)]
    }),
    eta: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    etd: new FormControl<string | null>(null)
  });
}

@Component({
  selector: 'app-visit-form-section',
  standalone: true,
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule],
  templateUrl: './visit-form-section.component.html',
  styleUrl: './visit-form-section.component.scss'
})
export class VisitFormSectionComponent implements OnInit {
  formGroup = input.required<FormGroup>();

  get visit(): FormGroup {
    return this.formGroup().get('visit') as FormGroup;
  }

  ngOnInit(): void {
    // Ensure the visit sub-group exists in the parent form
    if (!this.formGroup().get('visit')) {
      this.formGroup().addControl('visit', createVisitFormGroup());
    }
  }
}
