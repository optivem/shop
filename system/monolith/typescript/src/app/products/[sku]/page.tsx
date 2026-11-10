import { notFound } from "next/navigation";
import { loadProduct } from "@/lib/storefront/catalog-server";
import { ProductDetail } from "./ProductDetailView";

function decodeSku(raw: string): string {
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
}

/** `/products/{sku}` (storefront-ux §4.2, UC-007): public; an unknown SKU is a 404 page. */
export default async function ProductPage({ params }: Readonly<{ params: Promise<{ sku: string }> }>) {
  const { sku } = await params;
  const load = await loadProduct(decodeSku(sku));
  if (load.kind === "not-found") {
    notFound();
  }
  return <ProductDetail product={load.kind === "found" ? load.product : null} />;
}
