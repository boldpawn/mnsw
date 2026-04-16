import { TruncateUuidPipe } from './truncate-uuid.pipe';

describe('TruncateUuidPipe', () => {
  let pipe: TruncateUuidPipe;

  beforeEach(() => {
    pipe = new TruncateUuidPipe();
  });

  it('truncates a UUID longer than 8 chars and appends ellipsis', () => {
    const uuid = '550e8400-e29b-41d4-a716-446655440000';
    expect(pipe.transform(uuid)).toBe('550e8400…');
  });

  it('returns the value unchanged when it is exactly 8 characters', () => {
    expect(pipe.transform('12345678')).toBe('12345678');
  });

  it('returns the value unchanged when it is shorter than 8 characters', () => {
    expect(pipe.transform('abc')).toBe('abc');
  });

  it('handles empty string', () => {
    expect(pipe.transform('')).toBe('');
  });
});
