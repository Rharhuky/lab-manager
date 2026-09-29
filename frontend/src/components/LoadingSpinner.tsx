/**
 * Accessible loading spinner component.
 * Uses role="status" and aria-label for screen reader support.
 *
 * Requirements: 10.5
 */
export function LoadingSpinner({ label = 'Carregando...' }: { label?: string }) {
  return (
    <div
      role="status"
      aria-label={label}
      className="flex items-center justify-center p-8"
    >
      <div
        className="h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600"
        aria-hidden="true"
      />
      <span className="sr-only">{label}</span>
    </div>
  );
}

export default LoadingSpinner;
