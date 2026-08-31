import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { VirtualKeyboard } from '../VirtualKeyboard'

describe('VirtualKeyboard', () => {
  it('lays out two octaves of real piano keys', () => {
    render(<VirtualKeyboard onSubmit={vi.fn()} disabled={false} />)
    expect(screen.getByLabelText('C3')).toBeInTheDocument()
    expect(screen.getByLabelText('C#3')).toBeInTheDocument()
    expect(screen.getByLabelText('C5')).toBeInTheDocument()
    // No key between E and F, or between B and C.
    expect(screen.queryByLabelText('E#3')).not.toBeInTheDocument()
  })

  it('submits the notes in the order they were struck, which is what a scale needs', async () => {
    const onSubmit = vi.fn()
    render(<VirtualKeyboard onSubmit={onSubmit} disabled={false} />)

    await userEvent.click(screen.getByLabelText('E3'))
    await userEvent.click(screen.getByLabelText('C3'))
    await userEvent.click(screen.getByLabelText('G3'))
    await userEvent.click(screen.getByRole('button', { name: 'Play it' }))

    expect(onSubmit).toHaveBeenCalledWith([52, 48, 55])
  })

  it('clears the selection after sending, ready for the next answer', async () => {
    const onSubmit = vi.fn()
    render(<VirtualKeyboard onSubmit={onSubmit} disabled={false} />)

    await userEvent.click(screen.getByLabelText('D3'))
    await userEvent.click(screen.getByRole('button', { name: 'Play it' }))
    expect(screen.getByText(/click the keys/i)).toBeInTheDocument()
  })

  it('lets a mistaken key be taken back', async () => {
    const onSubmit = vi.fn()
    render(<VirtualKeyboard onSubmit={onSubmit} disabled={false} />)

    await userEvent.click(screen.getByLabelText('C3'))
    await userEvent.click(screen.getByLabelText('F#3'))
    await userEvent.click(screen.getByLabelText('F#3'))
    await userEvent.click(screen.getByRole('button', { name: 'Play it' }))

    expect(onSubmit).toHaveBeenCalledWith([48])
  })

  it('shows what is currently selected', async () => {
    render(<VirtualKeyboard onSubmit={vi.fn()} disabled={false} />)
    await userEvent.click(screen.getByLabelText('G3'))
    await userEvent.click(screen.getByLabelText('B3'))
    expect(screen.getByText('G3 B3')).toBeInTheDocument()
    expect(screen.getByLabelText('G3')).toHaveAttribute('aria-pressed', 'true')
  })

  it('cannot be played while the tutor is busy', () => {
    render(<VirtualKeyboard onSubmit={vi.fn()} disabled />)
    expect(screen.getByLabelText('C3')).toBeDisabled()
  })
})
