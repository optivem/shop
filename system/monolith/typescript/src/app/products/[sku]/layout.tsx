import type { ReactNode } from "react";

// Gives the product segment its own not-found boundary, so an unknown SKU renders ./not-found.tsx.
export default function ProductLayout({ children }: Readonly<{ children: ReactNode }>) {
  return children;
}
