import { useState, useEffect } from 'react';
import type { LaboratoryRequest, LaboratoryResponse } from '../types/laboratory';
import { laboratoryApi } from '../api/laboratoryApi';

interface LaboratoryFormProps {
  /** If provided, form operates in edit mode with pre-filled values */
  initial?: LaboratoryResponse;
  onSuccess: (lab: LaboratoryResponse) => void;
  onCancel: () => void;
}

/**
 * Form component for creating or editing a laboratory.
 * - Validates required fields and character limits before submitting
 * - Shows success feedback for at least 3 seconds
 * - Displays API 400 errors on respective fields
 *
 * Requirements: 11.1, 11.2, 11.4
 */
export function LaboratoryForm({ initial, onSuccess, onCancel }: LaboratoryFormProps) {
  const isEdit = !!initial;

  const [name, setName] = useState(initial?.name ?? '');
  const [block, setBlock] = useState(initial?.block ?? '');
  const [nameError, setNameError] = useState('');
  const [blockError, setBlockError] = useState('');
  const [apiError, setApiError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  // Keep form in sync if initial changes (e.g., re-opened for different lab)
  useEffect(() => {
    setName(initial?.name ?? '');
    setBlock(initial?.block ?? '');
    setNameError('');
    setBlockError('');
    setApiError('');
    setSuccess(false);
  }, [initial]);

  const validate = (): boolean => {
    let valid = true;
    if (!name.trim()) {
      setNameError('Nome é obrigatório');
      valid = false;
    } else if (name.length > 100) {
      setNameError('Nome não pode exceder 100 caracteres');
      valid = false;
    } else {
      setNameError('');
    }

    if (!block.trim()) {
      setBlockError('Bloco é obrigatório');
      valid = false;
    } else if (block.length > 20) {
      setBlockError('Bloco não pode exceder 20 caracteres');
      valid = false;
    } else {
      setBlockError('');
    }
    return valid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setApiError('');
    setLoading(true);

    const payload: LaboratoryRequest = { name: name.trim(), block: block.trim() };

    try {
      const result = isEdit
        ? await laboratoryApi.update(initial!.id, payload)
        : await laboratoryApi.create(payload);

      setSuccess(true);
      // Keep success message visible for at least 3 seconds, then notify parent
      setTimeout(() => {
        onSuccess(result);
      }, 3000);
    } catch (err: unknown) {
      const resp = (err as { response?: { status?: number; data?: { message?: string } } })?.response;
      if (resp?.status === 400) {
        setApiError(resp.data?.message ?? 'Dados inválidos. Verifique os campos.');
      } else if (resp?.status === 409) {
        setNameError(resp.data?.message ?? 'Já existe um laboratório com este nome.');
      } else {
        setApiError('Erro ao salvar laboratório. Tente novamente.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-gray-800">
        {isEdit ? 'Editar Laboratório' : 'Criar Laboratório'}
      </h2>

      {success && (
        <div role="status" className="mb-4 rounded bg-green-50 p-3 text-sm font-medium text-green-700">
          {isEdit ? 'Laboratório atualizado com sucesso!' : 'Laboratório criado com sucesso!'}
        </div>
      )}

      {apiError && (
        <div role="alert" className="mb-4 rounded bg-red-50 p-3 text-sm text-red-700">
          {apiError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="mb-4">
          <label htmlFor="lab-name" className="mb-1 block text-sm font-medium text-gray-700">
            Nome <span aria-hidden="true">*</span>
          </label>
          <input
            id="lab-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={100}
            aria-describedby={nameError ? 'lab-name-error' : undefined}
            aria-invalid={!!nameError}
            className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              nameError ? 'border-red-400' : 'border-gray-300'
            }`}
          />
          {nameError && (
            <p id="lab-name-error" role="alert" className="mt-1 text-xs text-red-600">
              {nameError}
            </p>
          )}
          <p className="mt-0.5 text-right text-xs text-gray-400">{name.length}/100</p>
        </div>

        <div className="mb-6">
          <label htmlFor="lab-block" className="mb-1 block text-sm font-medium text-gray-700">
            Bloco <span aria-hidden="true">*</span>
          </label>
          <input
            id="lab-block"
            type="text"
            value={block}
            onChange={(e) => setBlock(e.target.value)}
            maxLength={20}
            aria-describedby={blockError ? 'lab-block-error' : undefined}
            aria-invalid={!!blockError}
            className={`w-full min-h-[44px] rounded-md border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              blockError ? 'border-red-400' : 'border-gray-300'
            }`}
          />
          {blockError && (
            <p id="lab-block-error" role="alert" className="mt-1 text-xs text-red-600">
              {blockError}
            </p>
          )}
          <p className="mt-0.5 text-right text-xs text-gray-400">{block.length}/20</p>
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

export default LaboratoryForm;
