import { Component, input } from '@angular/core';
import { NgIf, NgTemplateOutlet } from '@angular/common';

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [NgIf, NgTemplateOutlet],
  template: `
    <div class="page-header">
      <div>
        <h1 class="page-header__title">{{ title() }}</h1>
        @if (subtitle()) { <p class="page-header__subtitle">{{ subtitle() }}</p> }
      </div>
      <ng-content select="[actions]" />
    </div>
  `,
  styles: [`.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 2px solid var(--color-primary); }
    .page-header__title { margin: 0; font-size: 1.5rem; color: var(--color-primary-darkest); }
    .page-header__subtitle { margin: 4px 0 0; color: var(--color-gray-dark); font-size: 0.875rem; }`]
})
export class PageHeaderComponent {
  title = input.required<string>();
  subtitle = input<string>();
}
