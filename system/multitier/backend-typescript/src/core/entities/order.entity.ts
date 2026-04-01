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
    name: 'total_price',
    type: 'numeric',
    precision: 10,
    scale: 2,
    nullable: false,
  })
  totalPrice: number;

  @Column({ name: 'status', nullable: false })
  status: OrderStatus;
}
