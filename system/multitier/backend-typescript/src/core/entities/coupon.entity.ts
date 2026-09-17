import Decimal from 'decimal.js';
import { Column, Entity, PrimaryGeneratedColumn } from 'typeorm';
import { numericTransformer } from './numeric.transformer';

@Entity('coupons')
export class Coupon {
  @PrimaryGeneratedColumn('identity')
  id!: number;

  @Column({
    name: 'code',
    type: 'varchar',
    length: 255,
    unique: true,
    nullable: false,
  })
  code!: string;

  @Column({
    name: 'discount_rate',
    type: 'numeric',
    precision: 5,
    scale: 4,
    nullable: false,
    transformer: numericTransformer(4),
  })
  discountRate!: Decimal;

  @Column({ name: 'valid_from', type: 'timestamptz', nullable: true })
  validFrom!: Date | null;

  @Column({ name: 'valid_to', type: 'timestamptz', nullable: true })
  validTo!: Date | null;

  @Column({ name: 'usage_limit', type: 'int', nullable: true })
  usageLimit!: number | null;

  @Column({ name: 'used_count', nullable: false, default: 0 })
  usedCount!: number;
}
