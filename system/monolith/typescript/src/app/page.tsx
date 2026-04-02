import Link from "next/link";

export default function Home() {
  return (
    <div className="jumbotron bg-light p-5 rounded">
      <h1 className="display-4">Welcome to eShop Starter!</h1>
      <p className="lead">Your modern e-commerce solution</p>
      <hr className="my-4" />
      <div className="row mt-4">
        <div className="col-md-4 mb-3">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title">New Order</h5>
              <p className="card-text">
                Place a new order with our easy-to-use interface
              </p>
              <Link className="btn btn-primary" href="/shop">
                Shop Now
              </Link>
            </div>
          </div>
        </div>
        <div className="col-md-4 mb-3">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title">Order History</h5>
              <p className="card-text">View and manage your past orders</p>
              <Link className="btn btn-primary" href="/order-history">
                View Orders
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
