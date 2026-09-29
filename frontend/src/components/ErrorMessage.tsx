interface ErrorMessageProps {
  message?: string;
  onRetry?: () => void;
}

/**
 * Displays an error message with an optional retry button.
 *
 * Requirements: 10.7
 */
export function ErrorMessage({
  message = 'Ocorreu um erro inesperado.',
  onRetry,
}: ErrorMessageProps) {
  return (
    <div
      role="alert"
      className="rounded-md border border-red-200 bg-red-50 p-4 text-red-800"
    >
      <p className="text-sm font-medium">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-2 min-h-[44px] min-w-[44px] rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1"
        >
          Tentar novamente
        </button>
      )}
    </div>
  );
}

export default ErrorMessage;
