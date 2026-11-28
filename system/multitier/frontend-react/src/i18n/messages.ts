/**
 * The shape every translation must match: same keys as the English source, every leaf a string.
 * A missing or extra key in sr/de/fr is a compile error.
 */
export type Messages<T> = { [K in keyof T]: T[K] extends string ? string : Messages<T[K]> };
