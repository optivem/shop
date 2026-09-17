import { IsISO8601 } from 'class-validator';

export class ClockGetTimeResponse {
  @IsISO8601({ strict: true })
  time!: string;
}
