import { useState, useEffect } from 'react';
import type { ReservationRequest, ReservationResponse } from '../types/reservation';

interface ReservationFormProps {
  /** If provided, form operates in edit mode */
  initial?: ReservationResponse;
  onSubmit: (request: ReservationRequest) => Promise<ReservationResponse>;
  onCancel: () => void;
}

/**
 * Form for creating or editing a reservation.
 * - Validates required fields and startAt < endAt before submitting
 * - Shows success feedback for at least 3 seconds
 * - Guards: only rendered when caller has PROFESSOR role
 *
 * Requirements: 12.1, 12.2
 */
export function ReservationForm({ initial, onSubmit, onCancel }: ReservationFormProps) {
  const isEdit = !!initial;

  // datetime-local inputs use local ISO format (YYYY-MM-DDTHH:mm)
  const toLocalInput = (iso?: string) => {
    if (!iso) return '';
    // Truncate to minute precision for datetime-local
    return iso.slice(0, 16);
  };

  const [startAt, setStartAt] = useState(toLocalInput(initial?.startAt));
  const [endAt, setEndAt] = useState(toLocalInput(initial?.endAt));
  const [startError, setStartError] = useState('');
  const [endError, setEndError] = useState('');
  const [apiError, setApiError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setStartAt(toLocalInput(initial?.startAt));
    setEndAt(toLocalInput(initial?.endAt));
    setStartError('');
    setEndError('');
    setApiError('');
    setSuccess(false);
  }, [initial]);

  const validate = (): boolean => {
    let valid = true;

    if (!startAt) {
      setStartError('Data e hora de início são obrigatórias');
      valid = false;
    } else {
      setStartError('');
    }

    if (!endAt) {
      setEndError('Data e hora de término são obrigatórias');
      valid = false;
    } else {
      setEndError('');
    }

    if (startAt && endAt) {
      const start = new Date(startAt);
      const end = new Date(endAt);
      if (isNaN(start.getTime())) {
        setStartError('Formato de data/hora inválido');
        valid = false;
      } else if (isNaN(end.getTime())) {
        setEndError('Formato de data/hora inválido');
        valid = false;
      } else if (start >= end) {
        setEndError('O horário de término deve ser posterior ao horário de início');
        valid = false;
      }
    }

    return valid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setApiError('');
    setLoading(true);

    // Convert datetime-local to ISO 8601 UTC string
    const payload: ReservationRequest = {
      startAt: new Date(startAt).toISOString(),
      endAt: new Date(endAt).toISOString(),
    };

    try {
      await onSubmit(payload);
      setSuccess(true);
      // Notify parent after 3 seconds success display
      setTimeout(() => {
        onCancel();
      }, 3000);
    } catch (err: unknown) {
      const resp = (err as { response?: { status?: number; data?: { message?: string } } })?.response;
      if (resp?.status === 409) {
        setApiError('Existe conflito de horário com outra reserva existente.');
      } else if (resp?.status === 400) {
        setApiError(resp.data?.message ?? 'Dados inválidos. Verifique os campos.');
      } else {
        setApiError('Erro ao salvar reserva. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-gray-800">
        {isEdit ? 'Editar Reserva' : 'Nova Reserva'}
      </h2>

      {success && (
        <div role="status" className="mb-4 rounded bg-green-50 p-3 text-sm font-medium text-green-700">
          {isEdit ? 'Reserva atualizada com sucesso!' : 'Reserva criada com sucesso!'}
        </div>
      )}

      {apiError && (
        <div role="alert" className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
          {apiError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="mb-4">
          <label htmlFor="res-start" className="mb-1 block text-sm font-medium text-gray-700">
            Início <span aria-hidden="true">*</span>
          </label>
          <input
            id="res-start"
            type="datetime-local"
            value={startAt}
            onChange={(e) => setStartAt(e.target.value)}
            aria-describedby={startError ? 'res-start-error' : undefined}
            aria-invalid={!!startError}
            className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              startError ? 'border-red-400' : 'border-gray-300'
            }`}
          />
          {startError && (
            <p id="res-start-error" role="alert" className="mt-1 text-xs text-red-600">
              {startError}
            </p>
          )}
        </div>

        <div className="mb-6">
          <label htmlFor="res-end" className="mb-1 block text-sm font-medium text-gray-700">
            Término <span aria-hidden="true">*</span>
          </label>
          <input
            id="res-end"
            type="datetime-local"
            value={endAt}
            onChange={(e) => setEndAt(e.target.value)}
            aria-describedby={endError ? 'res-end-error' : undefined}
            aria-invalid={!!endError}
            className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              endError ? 'border-red-400' : 'border-gray-300'
            }`}
          />
          {endError && (
            <p id="res-end-error" role="alert" className="mt-1 text-xs text-red-600">
              {endError}
            </p>
          )}
        </div>

        <div className="flex flex-wrap justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="min-h-[44px] rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={loading || success}
            className="min-h-[44px] rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-60"
          >
            {loading ? 'Salvando...' : isEdit ? 'Atualizar' : 'Criar'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default ReservationForm;
