import { Link } from 'react-router-dom';

/**
 * 404 Not Found page.
 * Requirements: 13.1
 */
export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 px-4 text-center">
      <h1 className="mb-2 text-6xl font-bold text-gray-300">404</h1>
      <h2 className="mb-4 text-2xl font-semibold text-gray-700">Página não encontrada</h2>
      <p className="mb-6 text-gray-500">
        A página que você está procurando não existe ou foi removida.
      </p>
      <Link
        to="/laboratories"
        className="min-h-[44px] rounded-md bg-blue-600 px-6 py-3 font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
      >
        Voltar para Laboratórios
      </Link>
    </div>
  );
}

export default NotFoundPage;
