import { render, screen, fireEvent } from '@testing-library/react'
import DataTable, { type Column } from '../components/common/DataTable'

interface Row { id: string; name: string }

const columns: Column<Row>[] = [
  { key: 'name', header: 'Name', render: (r) => r.name },
]
const rows: Row[] = [
  { id: '1', name: 'Alice' },
  { id: '2', name: 'Bob' },
]

describe('DataTable', () => {
  it('renders column headers and row data', () => {
    render(<DataTable columns={columns} rows={rows} rowKey={(r) => r.id} />)
    expect(screen.getByText('Name')).toBeInTheDocument()
    expect(screen.getByText('Alice')).toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
  })

  it('shows loading spinner when loading=true', () => {
    render(<DataTable columns={columns} rows={[]} rowKey={(r) => r.id} loading />)
    expect(screen.getByRole('progressbar')).toBeInTheDocument()
  })

  it('shows empty message when rows is empty', () => {
    render(
      <DataTable columns={columns} rows={[]} rowKey={(r) => r.id} emptyMessage="Nothing here" />,
    )
    expect(screen.getByText('Nothing here')).toBeInTheDocument()
  })

  it('calls onPageChange with 0-indexed page when pagination changes', () => {
    const onPageChange = vi.fn()
    render(
      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r) => r.id}
        page={0}
        totalPages={3}
        onPageChange={onPageChange}
      />,
    )
    fireEvent.click(screen.getByRole('button', { name: /page 2/i }))
    expect(onPageChange).toHaveBeenCalledWith(1)
  })
})
