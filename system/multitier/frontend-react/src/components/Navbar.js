import React from 'react';
import { Link } from 'react-router-dom';

function Navbar({ title }) {
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
      <div className="container">
        <Link className="navbar-brand" to="/">
          eShop Starter
        </Link>
        {title && <span className="navbar-text text-white">{title}</span>}
      </div>
    </nav>
  );
}

export default Navbar;
