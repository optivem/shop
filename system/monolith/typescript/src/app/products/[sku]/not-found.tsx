import Link from "next/link";
import { StoreLayout } from "@/components/StoreLayout";

/** The 404 page of an unknown SKU (UC-007 2a). */
export default function ProductNotFound() {
  return (
    <StoreLayout title="Product not found">
      <div className="market-empty text-center py-5 px-3">
        <h1 className="h3">Product not found</h1>
        <p className="text-muted">We couldn&apos;t find a product with that SKU.</p>
        <Link href="/" className="btn btn-primary" aria-label="Back to Catalog">
          Back to the catalog
        </Link>
      </div>
    </StoreLayout>
  );
}
