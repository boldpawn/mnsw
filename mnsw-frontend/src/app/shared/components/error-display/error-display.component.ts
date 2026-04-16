import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-error-display',
  standalone: true,
  template: `
    @if (message()) {
      <div class="error-display" role="alert">
        <mat-icon>error_outline</mat-icon>
        <span>{{ message() }}</span>
      </div>
    }
  `,
  styles: [`.error-display { display: flex; align-items: center; gap: 8px; padding: 12px 16px; background: #FDE8E6; border: 1px solid var(--color-error); color: var(--color-error); margin-bottom: 16px; }`],
  imports: [MatIconModule, NgIf]
})
export class ErrorDisplayComponent {
  message = input<string | null>(null);
}
