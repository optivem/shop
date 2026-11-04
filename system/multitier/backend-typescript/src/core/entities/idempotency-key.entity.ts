import { Column, Entity, PrimaryGeneratedColumn } from 'typeorm';

/**
 * One answered POST /api/orders per (owner, Idempotency-Key): the request fingerprint and the response
 * that was sent, so a replay can be answered without placing a second order. Table and constraint
 * come from the shared migration V20261103180000__add_idempotency_keys.sql.
 */
@Entity('idempotency_keys')
export class IdempotencyKey {
  @PrimaryGeneratedColumn('identity')
  id!: number;

  @Column({ name: 'owner', type: 'varchar', length: 255, nullable: false })
  owner!: string;

  @Column({
    name: 'idempotency_key',
    type: 'varchar',
    length: 255,
    nullable: false,
  })
  key!: string;

  @Column({
    name: 'request_hash',
    type: 'varchar',
    length: 64,
    nullable: false,
  })
  requestHash!: string;

  @Column({ name: 'response_status', type: 'int', nullable: false })
  responseStatus!: number;

  @Column({ name: 'response_body', type: 'text', nullable: false })
  responseBody!: string;

  @Column({ name: 'created_at', type: 'timestamptz', nullable: false })
  createdAt!: Date;
}
