import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ReservationForm } from '../ReservationForm';
import type { ReservationResponse } from '../../types/reservation';

const mockReservation: ReservationResponse = {
  id: 'res-1',
  laboratoryId: 'lab-1',
  userId: 'user-1',
  startAt: '2025-06-01T14:00:00Z',
  endAt: '2025-06-01T16:00:00Z',
  createdAt: '2025-01-01T00:00:00Z',
};

describe('ReservationForm', () => {
  const onCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders create form with empty date fields', () => {
    render(<ReservationForm onSubmit={vi.fn()} onCancel={onCancel} />);
    expect(screen.getByLabelText(/início/i)).toHaveValue('');
    expect(screen.getByLabelText(/término/i)).toHaveValue('');
    expect(screen.getByRole('button', { name: /criar/i })).toBeInTheDocument();
  });

  it('renders edit form with pre-filled values', () => {
    render(<ReservationForm initial={mockReservation} onSubmit={vi.fn()} onCancel={onCancel} />);
    expect(screen.getByRole('button', { name: /atualizar/i })).toBeInTheDocument();
  });

  it('shows validation error when startAt is empty', async () => {
    render(<ReservationForm onSubmit={vi.fn()} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/início são obrigatórias/i)).toBeInTheDocument();
  });

  it('shows validation error when endAt is empty', async () => {
    render(<ReservationForm onSubmit={vi.fn()} onCancel={onCancel} />);
    fireEvent.change(screen.getByLabelText(/início/i), { target: { value: '2025-06-01T14:00' } });
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/término são obrigatórias/i)).toBeInTheDocument();
  });

  it('shows validation error when startAt >= endAt', async () => {
    render(<ReservationForm onSubmit={vi.fn()} onCancel={onCancel} />);
    fireEvent.change(screen.getByLabelText(/início/i), { target: { value: '2025-06-01T16:00' } });
    fireEvent.change(screen.getByLabelText(/término/i), { target: { value: '2025-06-01T14:00' } });
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    expect(await screen.findByText(/posterior ao horário de início/i)).toBeInTheDocument();
  });

  it('does NOT call onSubmit when validation fails', async () => {
    const onSubmit = vi.fn();
    render(<ReservationForm onSubmit={onSubmit} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    await waitFor(() => {
      expect(onSubmit).not.toHaveBeenCalled();
    });
  });

  it('calls onSubmit with correct ISO payload when valid', async () => {
    const onSubmit = vi.fn().mockResolvedValue(mockReservation);
    render(<ReservationForm onSubmit={onSubmit} onCancel={onCancel} />);

    fireEvent.change(screen.getByLabelText(/início/i), { target: { value: '2025-06-01T14:00' } });
    fireEvent.change(screen.getByLabelText(/término/i), { target: { value: '2025-06-01T16:00' } });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    });

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledOnce();
      const call = onSubmit.mock.calls[0][0];
      expect(call).toHaveProperty('startAt');
      expect(call).toHaveProperty('endAt');
      expect(new Date(call.startAt).toISOString()).toBeTruthy();
    });
  });

  it('shows conflict error message for HTTP 409', async () => {
    const onSubmit = vi.fn().mockRejectedValue({
      response: { status: 409, data: { message: 'Conflito' } },
    });
    render(<ReservationForm onSubmit={onSubmit} onCancel={onCancel} />);

    fireEvent.change(screen.getByLabelText(/início/i), { target: { value: '2025-06-01T14:00' } });
    fireEvent.change(screen.getByLabelText(/término/i), { target: { value: '2025-06-01T16:00' } });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    });

    expect(await screen.findByText(/conflito de horário/i)).toBeInTheDocument();
  });

  it('shows success feedback after successful submit', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const onSubmit = vi.fn().mockResolvedValue(mockReservation);
    render(<ReservationForm onSubmit={onSubmit} onCancel={onCancel} />);

    fireEvent.change(screen.getByLabelText(/início/i), { target: { value: '2025-06-01T14:00' } });
    fireEvent.change(screen.getByLabelText(/término/i), { target: { value: '2025-06-01T16:00' } });

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /criar/i }));
    });

    expect(await screen.findByText(/criada com sucesso/i)).toBeInTheDocument();
    vi.useRealTimers();
  });

  it('calls onCancel when cancel button is clicked', () => {
    render(<ReservationForm onSubmit={vi.fn()} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /cancelar/i }));
    expect(onCancel).toHaveBeenCalledOnce();
  });
});
