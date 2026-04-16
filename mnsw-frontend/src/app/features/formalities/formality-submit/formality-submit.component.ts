import { Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { FormalityType } from '../../../core/models/formality.model';

interface FormalityTypeCard {
  type: FormalityType;
  title: string;
  description: string;
  icon: string;
  enabled: boolean;
}

@Component({
  selector: 'app-formality-submit',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatButtonModule, PageHeaderComponent],
  templateUrl: './formality-submit.component.html',
  styleUrl: './formality-submit.component.scss'
})
export class FormalitySubmitComponent {
  private router = inject(Router);
  private auth = inject(AuthService);

  canSubmit = computed(() => this.auth.hasAnyRole('SCHEEPSAGENT', 'LADINGAGENT', 'ADMIN'));

  readonly typeCards: FormalityTypeCard[] = [
    {
      type: 'NOA',
      title: 'Aankomstmelding',
      description: 'Meld de verwachte aankomst van een schip in de haven.',
      icon: 'anchor',
      enabled: true
    },
    {
      type: 'NOS',
      title: 'Zeilvaardigheidsverklaring',
      description: 'Dien een melding in van het daadwerkelijke vertrek van het schip.',
      icon: 'sailing',
      enabled: true
    },
    {
      type: 'NOD',
      title: 'Vertrekmelding',
      description: 'Meld het verwachte vertrek van een schip uit de haven.',
      icon: 'directions_boat',
      enabled: true
    },
    {
      type: 'VID',
      title: 'Scheepsidentificatie',
      description: 'Dien scheepsidentificatiegegevens in (VID-formality).',
      icon: 'badge',
      enabled: true
    },
    {
      type: 'SID',
      title: 'Veiligheidsverklaring',
      description: 'Dien een scheepsveiligheidsverklaring in conform ISPS.',
      icon: 'security',
      enabled: true
    }
  ];

  navigateTo(type: FormalityType, enabled: boolean): void {
    if (!enabled || !this.canSubmit()) return;
    this.router.navigate(['/formalities/new', type.toLowerCase()]);
  }
}
