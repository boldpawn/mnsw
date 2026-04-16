import { Pipe, PipeTransform } from '@angular/core';
import { FormalityType } from '../../core/models/formality.model';

@Pipe({ name: 'formalityTypeLabel', standalone: true })
export class FormalityTypeLabelPipe implements PipeTransform {
  private labels: Record<FormalityType, string> = {
    NOA: 'Aankomstmelding',
    NOS: 'Zeilvaardigheidsverklaring',
    NOD: 'Vertrekmelding',
    VID: 'Scheepsidentificatie',
    SID: 'Veiligheidsverklaring'
  };

  transform(value: FormalityType): string { return this.labels[value] ?? value; }
}
