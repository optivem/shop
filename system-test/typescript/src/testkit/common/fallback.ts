// Fallback helpers for values where an empty string means "not provided" (env vars, scraped UI text,
// optional JSON fields). Unlike `??`, an empty string also falls back; the named helper makes that
// intent explicit instead of relying on `||` truthiness.

export function nonEmptyOr<T extends string, F>(value: T | null | undefined, fallback: F): T | F {
  if (value) return value;
  return fallback;
}

export function envOrDefault(name: string, defaultValue: string): string {
  return nonEmptyOr(process.env[name], defaultValue);
}
