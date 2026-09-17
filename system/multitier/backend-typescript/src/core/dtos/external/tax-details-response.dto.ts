import { IsNumber } from 'class-validator';

// Only the fields the backend uses are declared; anything else in the response is ignored.
export class TaxDetailsResponse {
  @IsNumber()
  taxRate!: number;
}
