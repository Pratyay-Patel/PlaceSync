import { render, screen } from '@testing-library/react'
import LoadingSpinner from '../components/common/LoadingSpinner'

describe('LoadingSpinner', () => {
  it('renders a circular progress element', () => {
    render(<LoadingSpinner />)
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })
})
