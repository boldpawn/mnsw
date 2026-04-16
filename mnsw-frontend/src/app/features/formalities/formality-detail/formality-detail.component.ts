import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe, NgFor, NgIf, KeyValuePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';

import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Formality } from '../../../core/models/formality.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../shared/components/error-display/error-display.component';
import { FormalityTypeLabelPipe } from '../../../shared/pipes/formality-type-label.pipe';
import { TruncateUuidPipe } from '../../../shared/pipes/truncate-uuid.pipe';
import { RejectDialogComponent } from './reject-dialog.component';

@Component({
  selector: 'app-formality-detail',
  standalone: true,
  imports: [
    MatTabsModule, MatButtonModule, MatIconModule, MatCardModule, MatDividerModule,
    MatDialogModule, MatProgressSpinnerModule, RouterLink, NgIf, NgFor, DatePipe, KeyValuePipe,
    StatusBadgeComponent, PageHeaderComponent, FormalityTypeLabelPipe, TruncateUuidPipe,
    ErrorDisplayComponent
  ],
  templateUrl: './formality-detail.component.html',
  styleUrl: './formality-detail.component.scss'
})
export class FormalityDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private formalityService = inject(FormalityService);
  private dialog = inject(MatDialog);
  private router = inject(Router);
  protected auth = inject(AuthService);

  formality = signal<Formality | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  actionError = signal<string | null>(null);

  isSubmitter = computed(() => this.formality()?.submitterId === this.auth.currentUser()?.id);
  canCorrect = computed(() => this.isSubmitter() && this.formality()?.status !== 'SUPERSEDED');
  canApprove = computed(() => this.auth.hasAnyRole('HAVENAUTORITEIT', 'ADMIN'));

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loading.set(true);
    this.formalityService.get(id).subscribe({
      next: f => {
        this.formality.set(f);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Formality niet gevonden.');
        this.loading.set(false);
      }
    });
  }

  approve(): void {
    const id = this.formality()!.id;
    this.actionError.set(null);
    this.formalityService.approve(id).subscribe({
      next: f => this.formality.set(f),
      error: () => this.actionError.set('Fout bij goedkeuren.')
    });
  }

  openRejectDialog(): void {
    const ref = this.dialog.open(RejectDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.actionError.set(null);
        this.formalityService.reject(this.formality()!.id, result).subscribe({
          next: f => this.formality.set(f),
          error: () => this.actionError.set('Fout bij afwijzen.')
        });
      }
    });
  }
}
