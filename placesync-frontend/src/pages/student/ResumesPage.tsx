import { useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Box, Typography, Card, CardContent, Chip, CircularProgress,
  Button, IconButton, Tooltip, Dialog, DialogTitle, DialogContent,
  DialogActions, TextField, Alert, Divider,
} from '@mui/material';
import {
  UploadFileRounded, DeleteRounded, StarRounded, StarBorderRounded,
  DownloadRounded, DescriptionRounded,
} from '@mui/icons-material';
import { resumeApi } from '../../api/resumeApi';
import ConfirmDialog from '../../components/common/ConfirmDialog';

function fmtSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function fmt(dateStr: string) {
  return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export default function ResumesPage() {
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [label, setLabel] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState('');
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const { data: resumes = [], isLoading } = useQuery({
    queryKey: ['resumes'],
    queryFn: resumeApi.list,
  });

  const uploadMutation = useMutation({
    mutationFn: () => resumeApi.upload(selectedFile!, label, resumes.length === 0),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
      setUploadOpen(false);
      setLabel('');
      setSelectedFile(null);
    },
    onError: () => setUploadError('Upload failed. Please try again.'),
  });

  const defaultMutation = useMutation({
    mutationFn: (id: string) => resumeApi.setDefault(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['resumes'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => resumeApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
      setDeleteId(null);
    },
  });

  const downloadMutation = useMutation({
    mutationFn: (id: string) => resumeApi.getDownloadUrl(id),
    onSuccess: (data) => window.open(data.downloadUrl, '_blank'),
  });

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setSelectedFile(file);
    if (!label) setLabel(file.name.replace(/\.[^.]+$/, ''));
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>My Resumes</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
            {resumes.length} resume{resumes.length !== 1 ? 's' : ''} uploaded
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<UploadFileRounded />}
          onClick={() => { setUploadError(''); setUploadOpen(true); }}
        >
          Upload Resume
        </Button>
      </Box>

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : resumes.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <DescriptionRounded sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
            <Typography variant="body1" sx={{ fontWeight: 500 }}>No resumes yet</Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary', mt: 0.5 }}>
              Upload a PDF to start applying for jobs.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
          {resumes.map((resume) => (
            <Card key={resume.id} sx={{ position: 'relative' }}>
              {resume.isDefault && (
                <Chip
                  label="Default"
                  size="small"
                  color="primary"
                  sx={{ position: 'absolute', top: 12, right: 12, fontWeight: 600 }}
                />
              )}
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 1.5 }}>
                  <DescriptionRounded sx={{ color: 'primary.main', fontSize: 32, flexShrink: 0 }} />
                  <Box sx={{ flexGrow: 1, minWidth: 0, pr: resume.isDefault ? 6 : 0 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 600 }} noWrap>
                      {resume.label}
                    </Typography>
                    <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }} noWrap>
                      {resume.originalFilename}
                    </Typography>
                  </Box>
                </Box>

                <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                  {fmtSize(resume.fileSizeBytes)} · Uploaded {fmt(resume.uploadedAt)}
                </Typography>

                <Divider sx={{ my: 1.5 }} />

                <Box sx={{ display: 'flex', gap: 0.5 }}>
                  <Tooltip title="Download">
                    <IconButton
                      size="small"
                      onClick={() => downloadMutation.mutate(resume.id)}
                    >
                      <DownloadRounded fontSize="small" />
                    </IconButton>
                  </Tooltip>

                  {!resume.isDefault && (
                    <Tooltip title="Set as default">
                      <IconButton
                        size="small"
                        onClick={() => defaultMutation.mutate(resume.id)}
                      >
                        <StarBorderRounded fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )}

                  {resume.isDefault && (
                    <Tooltip title="Default resume">
                      <span>
                        <IconButton size="small" disabled>
                          <StarRounded fontSize="small" sx={{ color: 'warning.main' }} />
                        </IconButton>
                      </span>
                    </Tooltip>
                  )}

                  <Box sx={{ flexGrow: 1 }} />

                  <Tooltip title="Delete">
                    <IconButton
                      size="small"
                      onClick={() => setDeleteId(resume.id)}
                      sx={{ color: 'error.main' }}
                    >
                      <DeleteRounded fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Box>
              </CardContent>
            </Card>
          ))}
        </Box>
      )}

      {/* Upload dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Resume</DialogTitle>
        <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {uploadError && <Alert severity="error">{uploadError}</Alert>}

          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />

          <Button
            variant="outlined"
            startIcon={<UploadFileRounded />}
            onClick={() => fileInputRef.current?.click()}
          >
            {selectedFile ? selectedFile.name : 'Choose PDF file'}
          </Button>

          <TextField
            label="Label"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            size="small"
            fullWidth
            placeholder="e.g. Software Engineer Resume"
          />
        </DialogContent>
        <Divider />
        <DialogActions sx={{ px: 3, py: 1.5 }}>
          <Button onClick={() => setUploadOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!selectedFile || !label.trim() || uploadMutation.isPending}
            onClick={() => uploadMutation.mutate()}
          >
            {uploadMutation.isPending ? 'Uploading…' : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Resume?"
        message="This resume will be permanently deleted and removed from any pending applications."
        confirmLabel="Delete"
        confirmColor="error"
        loading={deleteMutation.isPending}
        onConfirm={() => deleteId && deleteMutation.mutate(deleteId)}
        onClose={() => setDeleteId(null)}
      />
    </Box>
  );
}
