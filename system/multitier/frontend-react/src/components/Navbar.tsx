import { Link } from 'react-router-dom';
import { UserMenu } from './UserMenu';

interface NavbarProps {
  title?: string;
}

/**
 * Navigation bar component displayed at the top of all pages
 * @param title - Optional page title to display next to the brand name
 */
export function Navbar({ title }: Readonly<NavbarProps>) {
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary">
      <div className="container">
        <Link className="navbar-brand" to="/">
          MyShop
        </Link>
        {title && <span className="navbar-text text-white">{title}</span>}
        <UserMenu />
      </div>
    </nav>
  );
}
