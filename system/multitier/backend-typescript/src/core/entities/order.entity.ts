import { Column, Entity, PrimaryGeneratedColumn } from 'typeorm';
import { OrderStatus } from './order-status.enum';

@Entity('orders')
export class Order {
  @PrimaryGeneratedColumn('identity')
  id: number;

  @Column({ name: 'order_number', unique: true, nullable: false })
  orderNumber: string;

  @Column({ name: 'order_timestamp', type: 'timestamptz', nullable: false })
  orderTimestamp: Date;

  @Column({ name: 'country', nullable: false })
  country: string;

  @Column({ name: 'sku', nullable: false })
  sku: string;

  @Column({ name: 'quantity', nullable: false })
  quantity: number;

  @Column({
    name: 'unit_price',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  unitPrice: number;

  @Column({
    name: 'base_price',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  basePrice: number;

  @Column({
    name: 'discount_rate',
    type: 'numeric',
    precision: 5,
    scale: 4,
    nullable: false,
  })
  discountRate: number;

  @Column({
    name: 'discount_amount',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  discountAmount: number;

  @Column({
    name: 'subtotal_price',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  subtotalPrice: number;

  @Column({
    name: 'tax_rate',
    type: 'numeric',
    precision: 5,
    scale: 4,
    nullable: false,
  })
  taxRate: number;

  @Column({
    name: 'tax_amount',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  taxAmount: number;

  @Column({
    name: 'total_price',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  totalPrice: number;

  @Column({ name: 'status', nullable: false })
  status: OrderStatus;

  @Column({
    name: 'applied_coupon_code',
    type: 'varchar',
    nullable: true,
    default: null,
  })
  appliedCouponCode: string | null;
}
