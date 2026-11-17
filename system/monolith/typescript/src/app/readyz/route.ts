import { NextResponse } from 'next/server';
import { isDatabaseReachable } from '@/lib/db';

export const dynamic = 'force-dynamic';

/** Readiness probe: UP only while the database is reachable; no component details either way. */
export async function GET() {
  return (await isDatabaseReachable())
    ? NextResponse.json({ status: 'UP' })
    : NextResponse.json({ status: 'DOWN' }, { status: 503 });
}
