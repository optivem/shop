/** Builds a URL query string from the parameters that are set; null and undefined values are left out. */
export function queryString(parameters: Record<string, string | null | undefined>): string {
  const query = Object.entries(parameters)
    .filter((entry): entry is [string, string] => entry[1] !== null && entry[1] !== undefined)
    .map(([name, value]) => `${name}=${encodeURIComponent(value)}`)
    .join('&');
  return query === '' ? '' : `?${query}`;
}
