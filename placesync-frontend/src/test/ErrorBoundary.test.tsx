import { render, screen, fireEvent } from '@testing-library/react'
import ErrorBoundary from '../components/common/ErrorBoundary'

function Bomb({ shouldThrow }: { shouldThrow: boolean }) {
  if (shouldThrow) throw new Error('Test explosion')
  return <div>Safe content</div>
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow={false} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('Safe content')).toBeInTheDocument()
  })

  it('shows error UI and message when child throws', () => {
    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    )
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('Test explosion')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /go to home/i })).toBeInTheDocument()
  })

  it('navigates to / when Go to Home is clicked', () => {
    const hrefSetter = vi.fn()
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { ...window.location, set href(v: string) { hrefSetter(v) } },
    })

    render(
      <ErrorBoundary>
        <Bomb shouldThrow />
      </ErrorBoundary>,
    )
    fireEvent.click(screen.getByRole('button', { name: /go to home/i }))
    expect(hrefSetter).toHaveBeenCalledWith('/')
  })
})
