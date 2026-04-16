import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';
import { FormalityStatus } from '../../../core/models/formality.model';

describe('StatusBadgeComponent', () => {
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(StatusBadgeComponent);
  });

  const cases: Array<[FormalityStatus, string, string]> = [
    ['SUBMITTED', 'Ingediend', 'badge--submitted'],
    ['ACCEPTED', 'Goedgekeurd', 'badge--accepted'],
    ['REJECTED', 'Afgewezen', 'badge--rejected'],
    ['UNDER_REVIEW', 'In beoordeling', 'badge--under-review'],
    ['SUPERSEDED', 'Vervangen', 'badge--superseded'],
  ];

  cases.forEach(([status, expectedLabel, expectedClass]) => {
    it(`renders label "${expectedLabel}" and class "${expectedClass}" for status ${status}`, () => {
      fixture.componentRef.setInput('status', status);
      fixture.detectChanges();
      const el: HTMLElement = fixture.nativeElement.querySelector('.badge');
      expect(el.textContent?.trim()).toBe(expectedLabel);
      expect(el.classList).toContain(expectedClass);
    });
  });
});
