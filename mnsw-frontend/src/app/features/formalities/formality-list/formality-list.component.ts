import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, NgFor, NgIf } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { FormalityService } from '../../../core/api/formality.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Formality, FormalityStatus, FormalityType } from '../../../core/models/formality.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { ErrorDisplayComponent } from '../../../shared/components/error-display/error-display.component';
import { FormalityTypeLabelPipe } from '../../../shared/pipes/formality-type-label.pipe';
import { TruncateUuidPipe } from '../../../shared/pipes/truncate-uuid.pipe';

@Component({
  selector: 'app-formality-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatSortModule, MatSelectModule,
    MatDatepickerModule, MatInputModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, RouterLink, NgIf, NgFor, AsyncPipe,
    StatusBadgeComponent, PageHeaderComponent, ErrorDisplayComponent,
    FormalityTypeLabelPipe, TruncateUuidPipe
  ],
  templateUrl: './formality-list.component.html',
  styleUrl: './formality-list.component.scss'
})
export class FormalityListComponent implements OnInit {
  private formalityService = inject(FormalityService);
  protected auth = inject(AuthService);

  formalities = signal<Formality[]>([]);
  totalElements = signal(0);
  loading = signal(false);
  error = signal<string | null>(null);

  typeFilter = signal<FormalityType | ''>('');
  statusFilter = signal<FormalityStatus | ''>('');
  pageIndex = signal(0);
  pageSize = signal(20);

  displayedColumns = computed(() => {
    const cols = ['type', 'status', 'vessel', 'submittedAt', 'channel', 'actions'];
    if (this.auth.hasAnyRole('HAVENAUTORITEIT', 'ADMIN')) cols.splice(4, 0, 'submitterName');
    return cols;
  });

  canSubmit = computed(() => this.auth.hasAnyRole('SCHEEPSAGENT', 'LADINGAGENT', 'ADMIN'));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.formalityService.list({
      type: this.typeFilter() || undefined,
      status: this.statusFilter() || undefined,
      page: this.pageIndex(),
      size: this.pageSize()
    }).subscribe({
      next: page => {
        this.formalities.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Fout bij laden van formalities.');
        this.loading.set(false);
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  resetFilters(): void {
    this.typeFilter.set('');
    this.statusFilter.set('');
    this.applyFilters();
  }
}
