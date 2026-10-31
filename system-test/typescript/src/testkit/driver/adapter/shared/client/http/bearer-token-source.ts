// Supplies the bearer token for the next request, or undefined to send it without an Authorization header.
export type BearerTokenSource = () => Promise<string | undefined>;
