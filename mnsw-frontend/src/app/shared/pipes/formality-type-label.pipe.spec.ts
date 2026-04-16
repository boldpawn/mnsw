import { FormalityTypeLabelPipe } from './formality-type-label.pipe';
import { FormalityType } from '../../core/models/formality.model';

describe('FormalityTypeLabelPipe', () => {
  let pipe: FormalityTypeLabelPipe;

  beforeEach(() => {
    pipe = new FormalityTypeLabelPipe();
  });

  const cases: Array<[FormalityType, string]> = [
    ['NOA', 'Aankomstmelding'],
    ['NOS', 'Zeilvaardigheidsverklaring'],
    ['NOD', 'Vertrekmelding'],
    ['VID', 'Scheepsidentificatie'],
    ['SID', 'Veiligheidsverklaring'],
  ];

  cases.forEach(([type, expectedLabel]) => {
    it(`transforms ${type} to "${expectedLabel}"`, () => {
      expect(pipe.transform(type)).toBe(expectedLabel);
    });
  });
});
