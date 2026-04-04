import { Column, Entity, PrimaryGeneratedColumn } from 'typeorm';

@Entity('coupons')
export class Coupon {
  @PrimaryGeneratedColumn('identity')
  id: number;

  @Column({ name: 'code', unique: true, nullable: false })
  code: string;

  @Column({
    name: 'discount_rate',
    type: 'numeric',
    precision: 5,
    scale: 4,
    nullable: false,
  })
  discountRate: number;

  @Column({ name: 'valid_from', type: 'timestamptz', nullable: true })
  validFrom: Date | null;

  @Column({ name: 'valid_to', type: 'timestamptz', nullable: true })
  validTo: Date | null;

  @Column({ name: 'usage_limit', type: 'int', nullable: true })
  usageLimit: number | null;

  @Column({ name: 'used_count', nullable: false, default: 0 })
  usedCount: number;
}
