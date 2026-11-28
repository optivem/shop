// Service layer for the public wake-up status API (no sign-in needed): one probe per part of the system.

/** The parts of the system the backend reports on; `backend` also checks the database. */
export type ProbedSystem = 'backend' | 'erp' | 'tax' | 'clock';

/** `reached`: the backend itself answered. `up`: it reported the probed part as UP. */
export interface ProbeResult {
  reached: boolean;
  up: boolean;
}

/**
 * Asks the backend whether one part of the system answers; asking also wakes that part. Only a
 * `{"status":"UP"|"DOWN"}` body counts as the backend's answer: a proxy error page while the backend is
 * still starting, or no response at all, means the backend was not reached.
 */
export async function probeSystem(system: ProbedSystem, signal?: AbortSignal): Promise<ProbeResult> {
  try {
    const response = await fetch(`/api/status/${system}`, { method: 'GET', signal });
    const body: unknown = await response.json().catch(() => null);
    const status = typeof body === 'object' && body !== null ? (body as { status?: unknown }).status : undefined;
    if (status !== 'UP' && status !== 'DOWN') return { reached: false, up: false };
    return { reached: true, up: response.ok && status === 'UP' };
  } catch {
    return { reached: false, up: false };
  }
}
