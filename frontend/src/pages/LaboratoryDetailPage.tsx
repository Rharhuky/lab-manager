import { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { laboratoryApi } from '../api/laboratoryApi';
import { reservationApi } from '../api/reservationApi';
import type { LaboratoryDetail } from '../types/laboratory';
import type { ReservationRequest, ReservationResponse } from '../types/reservation';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { LaboratoryForm } from '../components/LaboratoryForm';
import { ReservationForm } from '../components/ReservationForm';
import { useAuthStore } from '../store/authStore';

type ReservationModal =
  | { mode: 'create' }
  | { mode: 'edit'; reservation: ReservationResponse }
  | null;

/**
 * Shows full laboratory details: lab data, inline edit/delete, and reservation management.
 * Requirements: 10.4, 11.2, 11.3, 12.1–12.4
 */
export function LaboratoryDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [lab, setLab] = useState<LaboratoryDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Lab-level UI state
  const [editingLab, setEditingLab] = useState(false);
  const [deletingLab, setDeletingLab] = useState(false);
  const [deleteLabError, setDeleteLabError] = useState('');
  const [deleteLabLoading, setDeleteLabLoading] = useState(false);

  // Reservation modal state
  const [reservationModal, setReservationModal] = useState<ReservationModal>(null);

  const getUserRole = useAuthStore((s) => s.getUserRole);
  const isProfessor = getUserRole() === 'PROFESSOR';

  const loadLab = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const data = await laboratoryApi.getById(id);
      setLab(data);
    } catch {
      setError('Não foi possível carregar o laboratório.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { loadLab(); }, [loadLab]);

  // --- Lab delete ---
  const handleDeleteLab = async () => {
    if (!lab) return;
    setDeleteLabLoading(true);
    setDeleteLabError('');
    try {
      await laboratoryApi.remove(lab.id);
      navigate('/laboratories', { replace: true });
    } catch (err: unknown) {
      const resp = (err as { response?: { status?: number; data?: { code?: string } } })?.response;
      if (resp?.status === 409 && resp.data?.code === 'LABORATORY_HAS_FUTURE_RESERVATIONS') {
        setDeleteLabError(
          'Este laboratório possui reservas futuras e não pode ser excluído.'
        );
      } else {
        setDeleteLabError('Não foi possível excluir o laboratório. Tente novamente.');
      }
    } finally {
      setDeleteLabLoading(false);
      setDeletingLab(false);
    }
  };

  // --- Reservation submit (create or update) ---
  const handleReservationSubmit = async (
    request: ReservationRequest
  ): Promise<ReservationResponse> => {
    if (!lab) throw new Error('Lab not loaded');

    if (reservationModal?.mode === 'edit') {
      const updated = await reservationApi.update(reservationModal.reservation.id, request);
      await loadLab(); // refresh list
      return updated;
    } else {
      const created = await reservationApi.create(lab.id, request);
      await loadLab();
      return created;
    }
  };

  // --- Reservation delete ---
  const handleDeleteReservation = async (reservationId: string) => {
    if (!window.confirm('Confirmar exclusão desta reserva?')) return;
    try {
      await reservationApi.remove(reservationId);
      await loadLab();
    } catch {
      alert('Não foi possível excluir a reserva. Tente novamente.');
    }
  };

  // --- Render ---
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <LoadingSpinner label="Carregando laboratório..." />
      </div>
    );
  }

  if (error || !lab) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <ErrorMessage message={error ?? 'Laboratório não encontrado.'} onRetry={loadLab} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-4xl px-4 py-8">
        {/* Breadcrumb */}
        <nav className="mb-4 text-sm text-gray-500" aria-label="Breadcrumb">
          <Link to="/laboratories" className="hover:text-blue-600 hover:underline">
            Laboratórios
          </Link>
          <span className="mx-2">/</span>
          <span className="text-gray-700">{lab.name}</span>
        </nav>

        {/* Lab edit form (inline) */}
        {editingLab ? (
          <div className="mb-6">
            <LaboratoryForm
              initial={lab}
              onSuccess={(updated) => {
                setLab({ ...lab, ...updated });
                setEditingLab(false);
              }}
              onCancel={() => setEditingLab(false)}
            />
          </div>
        ) : (
          <div className="mb-6 rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h1 className="text-2xl font-bold text-gray-800">{lab.name}</h1>
                <p className="mt-1 text-gray-500">Bloco: {lab.block}</p>
                <p className="mt-2">
                  {lab.reserved ? (
                    <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700">
                      Reservado
                    </span>
                  ) : (
                    <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700">
                      Disponível
                    </span>
                  )}
                </p>
              </div>

              {isProfessor && (
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setEditingLab(true)}
                    className="min-h-[44px] rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    onClick={() => { setDeletingLab(true); setDeleteLabError(''); }}
                    className="min-h-[44px] rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
                  >
                    Excluir
                  </button>
                </div>
              )}
            </div>

            {/* Delete confirmation inline */}
            {deletingLab && (
              <div className="mt-4 rounded-md border border-red-200 bg-red-50 p-4">
                <p className="text-sm font-medium text-red-800">
                  Tem certeza que deseja excluir o laboratório <strong>{lab.name}</strong>?
                </p>
                {deleteLabError && (
                  <p role="alert" className="mt-2 text-sm text-red-700">{deleteLabError}</p>
                )}
                <div className="mt-3 flex gap-2">
                  <button
                    type="button"
                    onClick={handleDeleteLab}
                    disabled={deleteLabLoading}
                    className="min-h-[44px] rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-60"
                  >
                    {deleteLabLoading ? 'Excluindo...' : 'Confirmar exclusão'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeletingLab(false)}
                    disabled={deleteLabLoading}
                    className="min-h-[44px] rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-60"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Reservations section */}
        <div className="rounded-lg border border-gray-200 bg-white shadow-sm">
          <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h2 className="text-lg font-semibold text-gray-700">Reservas</h2>
            {isProfessor && reservationModal === null && (
              <button
                type="button"
                onClick={() => setReservationModal({ mode: 'create' })}
                className="min-h-[44px] rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Nova Reserva
              </button>
            )}
          </div>

          {/* Inline reservation form */}
          {reservationModal !== null && (
            <div className="border-b border-gray-200 p-6">
              <ReservationForm
                initial={reservationModal.mode === 'edit' ? reservationModal.reservation : undefined}
                onSubmit={handleReservationSubmit}
                onCancel={() => setReservationModal(null)}
              />
            </div>
          )}

          {lab.reservations.length === 0 && reservationModal === null ? (
            <div className="p-8 text-center text-gray-500">
              <p>Nenhuma reserva cadastrada para este laboratório.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[400px] text-left text-sm">
                <thead className="bg-gray-50 text-xs font-semibold uppercase text-gray-500">
                  <tr>
                    <th className="px-6 py-3">Início</th>
                    <th className="px-6 py-3">Fim</th>
                    {isProfessor && <th className="px-6 py-3 text-right">Ações</th>}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {lab.reservations.map((r) => (
                    <tr key={r.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 text-gray-700">
                        {new Date(r.startAt).toLocaleString('pt-BR')}
                      </td>
                      <td className="px-6 py-4 text-gray-700">
                        {new Date(r.endAt).toLocaleString('pt-BR')}
                      </td>
                      {isProfessor && (
                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => setReservationModal({ mode: 'edit', reservation: r })}
                            className="mr-3 inline-flex min-h-[44px] items-center rounded px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDeleteReservation(r.id)}
                            className="inline-flex min-h-[44px] items-center rounded px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                          >
                            Excluir
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default LaboratoryDetailPage;
