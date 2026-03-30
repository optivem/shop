import React from 'react';
import { Link } from 'react-router-dom';
import './Home.css';

function Home() {
  return (
    <div className="container home">
      <h1>Hello World!</h1>
      <p>Welcome to the Starter</p>
      <nav>
        <Link to="/todos">Todo Manager</Link>
      </nav>
    </div>
  );
}

export default Home;