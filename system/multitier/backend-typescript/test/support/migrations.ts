import { Client } from 'pg';
import type { StartedPostgreSqlContainer } from '@testcontainers/postgresql';
import * as fs from 'fs';
import * as path from 'path';

// Canonical schema — the same migrations Flyway applies for the Java backend
// (system/db/migrations). Applied to a throwaway container so tests exercise the
// real DDL, not an entity-synchronised schema.
const MIGRATIONS_DIR = path.resolve(__dirname, '../../../../db/migrations');

export async function applyMigrations(
  container: StartedPostgreSqlContainer,
): Promise<void> {
  const client = new Client({
    host: container.getHost(),
    port: container.getPort(),
    user: container.getUsername(),
    password: container.getPassword(),
    database: container.getDatabase(),
  });
  await client.connect();
  try {
    const files = fs
      .readdirSync(MIGRATIONS_DIR)
      .filter((f) => f.endsWith('.sql'))
      .sort();
    for (const file of files) {
      const sql = fs.readFileSync(path.join(MIGRATIONS_DIR, file), 'utf8');
      await client.query(sql);
    }
  } finally {
    await client.end();
  }
}
