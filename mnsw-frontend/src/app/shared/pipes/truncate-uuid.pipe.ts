import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'truncateUuid', standalone: true })
export class TruncateUuidPipe implements PipeTransform {
  transform(value: string): string { return value?.length > 8 ? value.substring(0, 8) + '…' : value; }
}
