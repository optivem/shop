/**
 * Dev-only switch to see the cold-start wake-up dialog locally, where the catalog loads too fast to notice it:
 * open the shop with `?slowStart=5` and the first catalog load waits 5 seconds. Ignored in production builds.
 */
export function slowStartDelayMs(search: string = window.location.search, isDev: boolean = import.meta.env.DEV): number {
  if (!isDev) return 0;
  const seconds = Number(new URLSearchParams(search).get('slowStart'));
  return Number.isFinite(seconds) && seconds > 0 ? seconds * 1000 : 0;
}

export function delay(ms: number): Promise<void> {
  return ms > 0 ? new Promise((resolve) => setTimeout(resolve, ms)) : Promise.resolve();
}
