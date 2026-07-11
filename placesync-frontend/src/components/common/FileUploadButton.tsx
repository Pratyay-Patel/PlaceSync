import { useRef } from 'react';
import { Button, Typography } from '@mui/material';
import { UploadFileRounded } from '@mui/icons-material';
import type { SvgIconComponent } from '@mui/icons-material';

interface FileUploadButtonProps {
  accept: string;
  maxSizeMb: number;
  label: string;
  selectedFileName?: string;
  loading?: boolean;
  disabled?: boolean;
  Icon?: SvgIconComponent;
  onFile: (file: File) => void;
  onError: (message: string) => void;
}

export default function FileUploadButton({
  accept,
  maxSizeMb,
  label,
  selectedFileName,
  loading = false,
  disabled = false,
  Icon = UploadFileRounded,
  onFile,
  onError,
}: FileUploadButtonProps) {
  const inputRef = useRef<HTMLInputElement>(null);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;

    const extensions = accept.split(',').map((ext) => ext.trim().toLowerCase());
    const fileName = file.name.toLowerCase();
    const validExtension = extensions.some((ext) => fileName.endsWith(ext));
    if (!validExtension) {
      onError(`Only ${extensions.join(', ')} files are accepted.`);
      e.target.value = '';
      return;
    }

    if (file.size > maxSizeMb * 1024 * 1024) {
      onError(`File size exceeds the ${maxSizeMb} MB limit.`);
      e.target.value = '';
      return;
    }

    onFile(file);
  }

  return (
    <>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        style={{ display: 'none' }}
        onChange={handleChange}
      />
      <Button
        variant="outlined"
        startIcon={<Icon />}
        disabled={disabled || loading}
        onClick={() => inputRef.current?.click()}
        fullWidth
      >
        {selectedFileName ? (
          <Typography noWrap sx={{ maxWidth: 240, fontSize: 'inherit' }}>
            {selectedFileName}
          </Typography>
        ) : label}
      </Button>
    </>
  );
}
