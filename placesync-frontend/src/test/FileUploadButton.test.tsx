import { render, screen, fireEvent } from '@testing-library/react'
import FileUploadButton from '../components/common/FileUploadButton'

function makeFile(name: string, sizeMb: number) {
  const content = new Array(sizeMb * 1024 * 1024).fill('a').join('')
  return new File([content], name, { type: 'application/pdf' })
}

describe('FileUploadButton', () => {
  it('renders label text when no file selected', () => {
    render(
      <FileUploadButton accept=".pdf" maxSizeMb={5} label="Upload CV" onFile={vi.fn()} onError={vi.fn()} />,
    )
    expect(screen.getByRole('button', { name: /upload cv/i })).toBeInTheDocument()
  })

  it('shows selectedFileName when provided', () => {
    render(
      <FileUploadButton
        accept=".pdf"
        maxSizeMb={5}
        label="Upload"
        selectedFileName="my-cv.pdf"
        onFile={vi.fn()}
        onError={vi.fn()}
      />,
    )
    expect(screen.getByText('my-cv.pdf')).toBeInTheDocument()
  })

  it('calls onError for wrong file extension', () => {
    const onError = vi.fn()
    const onFile = vi.fn()
    render(
      <FileUploadButton accept=".pdf" maxSizeMb={5} label="Upload" onFile={onFile} onError={onError} />,
    )
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['content'], 'doc.docx', { type: 'application/msword' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(onError).toHaveBeenCalledWith('Only .pdf files are accepted.')
    expect(onFile).not.toHaveBeenCalled()
  })

  it('calls onError when file exceeds maxSizeMb', () => {
    const onError = vi.fn()
    const onFile = vi.fn()
    render(
      <FileUploadButton accept=".pdf" maxSizeMb={1} label="Upload" onFile={onFile} onError={onError} />,
    )
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const bigFile = makeFile('big.pdf', 2)
    fireEvent.change(input, { target: { files: [bigFile] } })
    expect(onError).toHaveBeenCalledWith('File size exceeds the 1 MB limit.')
    expect(onFile).not.toHaveBeenCalled()
  })

  it('calls onFile with valid file', () => {
    const onFile = vi.fn()
    render(
      <FileUploadButton accept=".pdf" maxSizeMb={5} label="Upload" onFile={onFile} onError={vi.fn()} />,
    )
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['pdf content'], 'cv.pdf', { type: 'application/pdf' })
    fireEvent.change(input, { target: { files: [file] } })
    expect(onFile).toHaveBeenCalledWith(file)
  })

  it('does nothing when no file is selected', () => {
    const onFile = vi.fn()
    const onError = vi.fn()
    render(
      <FileUploadButton accept=".pdf" maxSizeMb={5} label="Upload" onFile={onFile} onError={onError} />,
    )
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, { target: { files: [] } })
    expect(onFile).not.toHaveBeenCalled()
    expect(onError).not.toHaveBeenCalled()
  })
})
