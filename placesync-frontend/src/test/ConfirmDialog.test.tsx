import { render, screen, fireEvent } from '@testing-library/react'
import ConfirmDialog from '../components/common/ConfirmDialog'

const noop = () => {}

describe('ConfirmDialog', () => {
  it('renders title and message when open', () => {
    render(
      <ConfirmDialog open title="Delete item" message="Are you sure?" onConfirm={noop} onClose={noop} />,
    )
    expect(screen.getByText('Delete item')).toBeInTheDocument()
    expect(screen.getByText('Are you sure?')).toBeInTheDocument()
  })

  it('renders error alert when error prop is provided', () => {
    render(
      <ConfirmDialog open title="T" message="M" error="Something failed" onConfirm={noop} onClose={noop} />,
    )
    expect(screen.getByText('Something failed')).toBeInTheDocument()
  })

  it('calls onConfirm when confirm button is clicked', () => {
    const onConfirm = vi.fn()
    render(
      <ConfirmDialog open title="T" message="M" onConfirm={onConfirm} onClose={noop} />,
    )
    fireEvent.click(screen.getByRole('button', { name: /confirm/i }))
    expect(onConfirm).toHaveBeenCalledOnce()
  })

  it('shows loading text and disables buttons when loading=true', () => {
    render(
      <ConfirmDialog open loading title="T" message="M" onConfirm={noop} onClose={noop} />,
    )
    expect(screen.getByText('Please wait…')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled()
  })
})
