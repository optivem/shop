import { NextResponse } from 'next/server';

/** Liveness probe: answers while the process runs and checks no dependency. The Docker HEALTHCHECK probes it. */
export function GET() {
  return NextResponse.json({ status: 'UP' });
}
