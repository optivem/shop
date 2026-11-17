import { NextResponse } from 'next/server';
import { notFoundResponse } from '@/lib/errors';
import { buildOpenApiDocument } from '@/lib/openapi';
import { isProdProfile } from '@/lib/production-config-guard';

// springdoc's URL, as on the Java backends. Not listed as public in the auth policy, so only a
// signed-in caller gets the document.
export function GET() {
  // No OpenAPI document in production (B-PRG 4.4); local and pipeline stacks keep it.
  if (isProdProfile()) {
    return notFoundResponse('Not found');
  }
  return NextResponse.json(buildOpenApiDocument());
}
