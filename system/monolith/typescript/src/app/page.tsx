import { Suspense } from "react";
import { Catalog } from "./CatalogView";

/** `/` (storefront-ux §4.1): the catalog, public. The query string holds the filters. */
export default function Home() {
  return (
    <Suspense>
      <Catalog />
    </Suspense>
  );
}
