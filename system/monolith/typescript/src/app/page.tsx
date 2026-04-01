import Link from "next/link";

export default function Home() {
  return (
    <main>
      <h1>Welcome to eShop Starter!</h1>
      <p>Your modern e-commerce solution</p>
      <div style={{ display: "flex", gap: "1rem", marginTop: "2rem" }}>
        <div style={{ flex: 1, border: "1px solid #ddd", borderRadius: "8px", padding: "1.5rem", textAlign: "center" }}>
          <h2>New Order</h2>
          <p>Place a new order with our easy-to-use interface</p>
          <Link href="/shop" style={{ display: "inline-block", padding: "0.5rem 1rem", backgroundColor: "#0070f3", color: "#fff", borderRadius: "4px", textDecoration: "none" }}>
            Shop Now
          </Link>
        </div>
        <div style={{ flex: 1, border: "1px solid #ddd", borderRadius: "8px", padding: "1.5rem", textAlign: "center" }}>
          <h2>Order History</h2>
          <p>View and manage your past orders</p>
          <Link href="/order-history" style={{ display: "inline-block", padding: "0.5rem 1rem", backgroundColor: "#0070f3", color: "#fff", borderRadius: "4px", textDecoration: "none" }}>
            View Orders
          </Link>
        </div>
      </div>
    </main>
  );
}
