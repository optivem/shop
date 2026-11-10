import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { CART_COOKIE, parseCart } from "@/lib/storefront/cart-cookie";
import { Checkout } from "./CheckoutView";

/** `/checkout` (customers only, see lib/auth/policy): an empty cart has nothing to check out (UC-009 3a). */
export default async function CheckoutPage() {
  if (parseCart((await cookies()).get(CART_COOKIE)?.value).length === 0) {
    redirect("/cart");
  }
  return <Checkout />;
}
