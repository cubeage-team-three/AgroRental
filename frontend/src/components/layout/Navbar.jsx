import { Link } from 'react-router-dom';

function Navbar() {
  return (
    <nav className="bg-green-700 text-white px-6 py-4 flex items-center justify-between">
      <Link to="/" className="text-xl font-bold">
        Agro Rental Platform
      </Link>
      <div className="flex gap-4">
        <Link to="/" className="hover:text-green-200">
          Home
        </Link>
        <Link to="/login" className="hover:text-green-200">
          Login
        </Link>
        <Link to="/register" className="hover:text-green-200">
          Register
        </Link>
      </div>
    </nav>
  );
}

export default Navbar;
