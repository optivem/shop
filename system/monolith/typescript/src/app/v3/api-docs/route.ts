import { NextResponse } from 'next/server';
import { buildOpenApiDocument } from '@/lib/openapi';

// springdoc's URL, as on the Java backends. Not listed as public in the auth policy, so only a
// signed-in caller gets the document.
export function GET() {
  return NextResponse.json(buildOpenApiDocument());
}
