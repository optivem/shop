// Supplies the bearer token for a request, or undefined to send it without an Authorization header.
export type BearerTokenSource = (method: string, path: string) => Promise<string | undefined>;
