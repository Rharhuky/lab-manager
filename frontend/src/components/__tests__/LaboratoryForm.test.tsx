import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LaboratoryForm } from '../LaboratoryForm';
import * as laboratoryApiModule from '../../api/laboratoryApi';
import type { LaboratoryResponse } from '../../types/laboratory';

vi.mock('../../api/laboratoryApi');

const mockLab: LaboratoryResponse = {
  id: 'lab-1',
  name: 'Lab A',
  block: 'Bloco B',
  reserved: false,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
};

describe('LaboratoryForm', () => {
  const onSuccess = vi.fn();
  const onCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders create form with empty fields', () => {
    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    expect(screen.getByLabelText(/nome/i)).toHaveValue('');
    expect(screen.getByLabelText(/bloco/i)).toHaveValue('');
    expect(screen.getByRole('button', { name: /criar/i })).toBeInTheDocument();
  });

  it('renders edit form pre-filled with initial values', () => {
    render(<LaboratoryForm initial={mockLab} onSuccess={onSuccess} onCancel={onCancel} />);
    expect(screen.getByLabelText(/nome/i)).toHaveValue('Lab A');
    expect(screen.getByLabelText(/bloco/i)).toHaveValue('Bloco B');
    expect(screen.getByRole('button', { name: /atualizar/i })).toBeInTheDocument();
  });

  it('shows validation error when name is empty', async () => {
    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/nome é obrigatório/i)).toBeInTheDocument();
    expect(laboratoryApiModule.laboratoryApi.create).not.toHaveBeenCalled();
  });

  it('shows validation error when block is empty', async () => {
    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    await userEvent.type(screen.getByLabelText(/nome/i), 'Lab Teste');
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/bloco é obrigatório/i)).toBeInTheDocument();
    expect(laboratoryApiModule.laboratoryApi.create).not.toHaveBeenCalled();
  });

  it('shows validation error when name exceeds 100 characters', async () => {
    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    // Bypass maxLength by using fireEvent.change directly
    fireEvent.change(screen.getByLabelText(/nome/i), { target: { value: 'a'.repeat(101) } });
    await userEvent.type(screen.getByLabelText(/bloco/i), 'A');
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/100 caracteres/i)).toBeInTheDocument();
    expect(laboratoryApiModule.laboratoryApi.create).not.toHaveBeenCalled();
  });

  it('shows success message after successful create and calls onSuccess after 3s', async () => {
    vi.mocked(laboratoryApiModule.laboratoryApi.create).mockResolvedValue(mockLab);
    vi.useFakeTimers({ shouldAdvanceTime: true });

    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    await userEvent.type(screen.getByLabelText(/nome/i), 'Lab Novo');
    await userEvent.type(screen.getByLabelText(/bloco/i), 'Bloco C');

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    });

    expect(await screen.findByText(/criado com sucesso/i)).toBeInTheDocument();
    expect(onSuccess).not.toHaveBeenCalled();

    act(() => { vi.advanceTimersByTime(3000); });
    expect(onSuccess).toHaveBeenCalledWith(mockLab);

    vi.useRealTimers();
  });

  it('displays API 400 error message on the form', async () => {
    vi.mocked(laboratoryApiModule.laboratoryApi.create).mockRejectedValue({
      response: { status: 400, data: { message: 'Dados inválidos do servidor.' } },
    });

    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    await userEvent.type(screen.getByLabelText(/nome/i), 'Lab X');
    await userEvent.type(screen.getByLabelText(/bloco/i), 'A');

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    });

    expect(await screen.findByText(/dados inválidos do servidor/i)).toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', () => {
    render(<LaboratoryForm onSuccess={onSuccess} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
