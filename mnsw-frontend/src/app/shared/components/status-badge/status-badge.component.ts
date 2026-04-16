import { Component, computed, input } from '@angular/core';
import { FormalityStatus } from '../../../core/models/formality.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="badge" [class]="badgeClass()">{{ label() }}</span>`,
  styleUrl: './status-badge.component.scss'
})
export class StatusBadgeComponent {
  status = input.required<FormalityStatus>();

  label = computed(() => ({
    SUBMITTED: 'Ingediend',
    ACCEPTED: 'Goedgekeurd',
    REJECTED: 'Afgewezen',
    UNDER_REVIEW: 'In beoordeling',
    SUPERSEDED: 'Vervangen'
  })[this.status()]);

  badgeClass = computed(() => `badge badge--${this.status().toLowerCase().replace('_', '-')}`);
}
