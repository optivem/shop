import { type NextRequest, NextResponse } from 'next/server';
import { findCustomerProfile, upsertCustomerProfile } from '@/lib/db';
import { resolveCaller } from '@/lib/auth/caller';
import { parseUpdateProfileRequest } from '@/lib/validation';
import { toProfileResponse } from '@/lib/profile';
import { isRecord } from '@/lib/type-guards';
import {
  badRequestResponse,
  internalErrorResponse,
  unauthorizedResponse,
  validationErrorResponse,
} from '@/lib/errors';

// UC-010 Manage Profile: any signed-in user reads and saves their own profile, keyed by the token subject.

export async function GET(request: NextRequest) {
  try {
    const caller = await resolveCaller(request);
    if (!caller) {
      return unauthorizedResponse('Authentication is required');
    }
    return NextResponse.json(toProfileResponse(await findCustomerProfile(caller.subject), caller));
  } catch (error) {
    return internalErrorResponse(error);
  }
}

export async function PUT(request: NextRequest) {
  try {
    const caller = await resolveCaller(request);
    if (!caller) {
      return unauthorizedResponse('Authentication is required');
    }

    const body: unknown = await request.json().catch(() => undefined);
    if (!isRecord(body)) {
      return badRequestResponse('Invalid request format');
    }

    const parsed = parseUpdateProfileRequest(body);
    if (!parsed.ok) {
      return validationErrorResponse(parsed.errors);
    }

    const saved = await upsertCustomerProfile({ owner: caller.subject, ...parsed.value });
    return NextResponse.json(toProfileResponse(saved, caller));
  } catch (error) {
    return internalErrorResponse(error);
  }
}
