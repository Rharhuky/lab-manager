import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { laboratoryApi } from '../api/laboratoryApi';
import type { LaboratoryResponse } from '../types/laboratory';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { LaboratoryForm } from '../components/LaboratoryForm';
import { useAuthStore } from '../store/authStore';

type FormMode = { mode: 'create' } | { mode: 'edit'; lab: LaboratoryResponse } | null;

/**
 * Lists all laboratories with reservation status.
 * PROFESSOR sees create/edit/delete buttons; ALUNO does not (hidden from DOM).
 *
 * Requirements: 10.1–10.7, 11.1–11.4
 */
export function LaboratoryListPage() {
  const [laboratories, setLaboratories] = useState<LaboratoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [formMode, setFormMode] = useState<FormMode>(null);
  const [deleteError, setDeleteError] = useState<{ id: string; message: string } | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const getUserRole = useAuthStore((s) => s.getUserRole);
  const isProfessor = getUserRole() === 'PROFESSOR';

  const loadLaboratories = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await laboratoryApi.getAll();
      setLaboratories(data);
    } catch {
      setError('Não foi possível carregar os laboratórios.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadLaboratories(); }, [loadLaboratories]);

  const handleFormSuccess = (saved: LaboratoryResponse) => {
    if (formMode?.mode === 'edit') {
      setLaboratories((prev) => prev.map((l) => (l.id === saved.id ? saved : l)));
    } else {
      setLaboratories((prev) => [...prev, saved]);
    }
    setFormMode(null);
  };

  const handleDelete = async (lab: LaboratoryResponse) => {
    if (!window.confirm(`Excluir o laboratório "${lab.name}"?`)) return;
    setDeletingId(lab.id);
    setDeleteError(null);
    try {
      await laboratoryApi.remove(lab.id);
      setLaboratories((prev) => prev.filter((l) => l.id !== lab.id));
    } catch (err: unknown) {
      const resp = (err as { response?: { status?: number; data?: { code?: string; message?: string } } })?.response;
      if (resp?.status === 409 && resp.data?.code === 'LABORATORY_HAS_FUTURE_RESERVATIONS') {
        setDeleteError({ id: lab.id, message: 'Este laboratório possui reservas futuras e não pode ser excluído.' });
      } else {
        setDeleteError({ id: lab.id, message: 'Não foi possível excluir o laboratório.' });
      }
    } finally {
      setDeletingId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <LoadingSpinner label="Carregando laboratórios..." />
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 p-6">
        <ErrorMessage message={error} onRetry={loadLaboratories} />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-5xl px-4 py-8">
        {/* Header */}
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-800">Laboratórios</h1>
          {isProfessor && formMode === null && (
            <button
              type="button"
              onClick={() => setFormMode({ mode: 'create' })}
              className="min-h-[44px] rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Criar Laboratório
            </button>
          )}
        </div>

        {/* Inline create/edit form */}
        {formMode !== null && (
          <div className="mb-6">
            <LaboratoryForm
              initial={formMode.mode === 'edit' ? formMode.lab : undefined}
              onSuccess={handleFormSuccess}
              onCancel={() => setFormMode(null)}
            />
          </div>
        )}

        {/* Empty state */}
        {laboratories.length === 0 && (
          <div className="rounded-md border border-gray-200 bg-white p-12 text-center text-gray-500">
            <p className="text-lg">Nenhum laboratório encontrado.</p>
            {isProfessor && (
              <p className="mt-2 text-sm">
                Clique em &quot;Criar Laboratório&quot; para adicionar o primeiro.
              </p>
            )}
          </div>
        )}

        {/* List */}
        {laboratories.length > 0 && (
          <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white shadow-sm">
            <table className="w-full min-w-[480px] text-left text-sm">
              <thead className="border-b border-gray-200 bg-gray-50 text-xs font-semibold uppercase text-gray-500">
                <tr>
                  <th className="px-6 py-3">Nome</th>
                  <th className="px-6 py-3">Bloco</th>
                  <th className="px-6 py-3">Status</th>
                  {isProfessor && <th className="px-6 py-3 text-right">Ações</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {laboratories.map((lab) => (
                  <>
                    <tr key={lab.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 font-medium text-gray-800">
                        <Link
                          to={`/laboratories/${lab.id}`}
                          className="hover:text-blue-600 hover:underline"
                        >
                          {lab.name}
                        </Link>
                      </td>
                      <td className="px-6 py-4 text-gray-600">{lab.block}</td>
                      <td className="px-6 py-4">
                        {lab.reserved ? (
                          <span className="inline-flex items-center rounded-full bg-red-100 px-2.5 py-0.5 text-xs font-medium text-red-700">
                            Reservado
                          </span>
                        ) : (
                          <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-700">
                            Disponível
                          </span>
                        )}
                      </td>
                      {isProfessor && (
                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => setFormMode({ mode: 'edit', lab })}
                            className="mr-3 inline-flex min-h-[44px] items-center rounded px-3 py-1.5 text-xs font-medium text-blue-600 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            onClick={() => handleDelete(lab)}
                            disabled={deletingId === lab.id}
                            className="inline-flex min-h-[44px] items-center rounded px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 disabled:opacity-50"
                          >
                            {deletingId === lab.id ? 'Excluindo...' : 'Excluir'}
                          </button>
                        </td>
                      )}
                    </tr>
                    {/* Delete error row */}
                    {deleteError?.id === lab.id && (
                      <tr key={`${lab.id}-error`}>
                        <td colSpan={isProfessor ? 4 : 3} className="px-6 py-2">
                          <p role="alert" className="text-xs text-red-600">{deleteError.message}</p>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default LaboratoryListPage;
