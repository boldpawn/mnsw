import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

@Component({
  selector: 'app-reject-dialog',
  standalone: true,
  imports: [
    MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, ReactiveFormsModule
  ],
  template: `
    <h2 mat-dialog-title>Formality afwijzen</h2>
    <mat-dialog-content>
      <form [formGroup]="form">
        <mat-form-field appearance="outline">
          <mat-label>Redencode</mat-label>
          <mat-select formControlName="reasonCode">
            <mat-option value="INVALID_IMO">Ongeldig IMO-nummer</mat-option>
            <mat-option value="INCOMPLETE_DATA">Onvolledige gegevens</mat-option>
            <mat-option value="INVALID_TIMING">Ongeldige tijdsaanduiding</mat-option>
            <mat-option value="OTHER">Overig</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Toelichting</mat-label>
          <textarea matInput formControlName="reasonDescription" rows="3"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null">Annuleren</button>
      <button mat-raised-button color="warn" [mat-dialog-close]="form.value" [disabled]="form.invalid">Afwijzen</button>
    </mat-dialog-actions>
  `
})
export class RejectDialogComponent {
  form = new FormGroup({
    reasonCode: new FormControl('', Validators.required),
    reasonDescription: new FormControl('', Validators.required)
  });
}
