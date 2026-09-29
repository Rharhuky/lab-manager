import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

/**
 * Login page with email/password form.
 * - Validates fields before calling API
 * - Shows generic error for 401 without revealing which field is wrong
 * - Redirects to /laboratories on success
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5
 */
export function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [apiError, setApiError] = useState('');
  const [loading, setLoading] = useState(false);

  const validateEmail = (value: string): string => {
    if (!value.trim()) return 'E-mail é obrigatório';
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) return 'E-mail deve estar em formato válido';
    return '';
  };

  const validatePassword = (value: string): string => {
    if (!value.trim()) return 'Senha é obrigatória';
    return '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const eErr = validateEmail(email);
    const pErr = validatePassword(password);
    setEmailError(eErr);
    setPasswordError(pErr);

    if (eErr || pErr) return;

    setApiError('');
    setLoading(true);
    try {
      const response = await authApi.login({ email, password });
      setAuth(response);
      navigate('/laboratories', { replace: true });
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 401) {
        setApiError('Credenciais inválidas. Verifique seu e-mail e senha.');
      } else {
        setApiError('Erro ao realizar login. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 px-4 py-8 sm:px-6 lg:px-8">
      <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow sm:p-8">
        <h1 className="mb-6 text-center text-2xl font-bold text-gray-800">CampusLab</h1>

        {apiError && (
          <div role="alert" className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
            {apiError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-4">
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-gray-700">
              E-mail
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              autoComplete="email"
              aria-describedby={emailError ? 'email-error' : undefined}
              aria-invalid={!!emailError}
              className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                emailError ? 'border-red-400' : 'border-gray-300'
              }`}
            />
            {emailError && (
              <p id="email-error" role="alert" className="mt-1 text-xs text-red-600">
                {emailError}
              </p>
            )}
          </div>

          <div className="mb-6">
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-gray-700">
              Senha
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              aria-describedby={passwordError ? 'password-error' : undefined}
              aria-invalid={!!passwordError}
              className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                passwordError ? 'border-red-400' : 'border-gray-300'
              }`}
            />
            {passwordError && (
              <p id="password-error" role="alert" className="mt-1 text-xs text-red-600">
                {passwordError}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full min-h-[44px] rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-60"
          >
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default LoginPage;
