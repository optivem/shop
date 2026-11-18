import { NextResponse } from 'next/server';
import { findDistinctOwnerNames } from '@/lib/db';
import { internalErrorResponse } from '@/lib/errors';

/** The usernames that have placed an order, distinct, A to Z (UC-011 customer filter). Admin only, via the policy. */
export async function GET() {
  try {
    return NextResponse.json(await findDistinctOwnerNames());
  } catch (error) {
    return internalErrorResponse(error);
  }
}
