import { Column, Entity, PrimaryGeneratedColumn } from 'typeorm';

/**
 * One customer's name and default shipping address, keyed by the token subject (owner). The email is
 * not stored: it always comes from the sign-in account. Table and constraint come from the shared
 * migration V20261109120000__add_customer_profiles_and_order_shipping_address.sql.
 */
@Entity('customer_profiles')
export class CustomerProfile {
  /** BIGSERIAL, so the driver returns it as a string. */
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id!: string;

  @Column({ name: 'owner', type: 'varchar', length: 255, nullable: false })
  owner!: string;

  @Column({ name: 'first_name', type: 'varchar', length: 255, nullable: false })
  firstName!: string;

  @Column({ name: 'last_name', type: 'varchar', length: 255, nullable: false })
  lastName!: string;

  @Column({ name: 'street', type: 'varchar', length: 255, nullable: false })
  street!: string;

  @Column({
    name: 'postal_code',
    type: 'varchar',
    length: 255,
    nullable: false,
  })
  postalCode!: string;

  @Column({ name: 'city', type: 'varchar', length: 255, nullable: false })
  city!: string;

  @Column({ name: 'country', type: 'varchar', length: 255, nullable: false })
  country!: string;

  @Column({ name: 'updated_at', type: 'timestamptz', nullable: false })
  updatedAt!: Date;
}
