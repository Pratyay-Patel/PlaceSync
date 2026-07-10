import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Box, Typography, Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Select, MenuItem, FormControl, InputLabel, Chip, Pagination, CircularProgress,
  Collapse, IconButton, Tooltip,
} from '@mui/material';
import { ExpandMoreRounded, ExpandLessRounded } from '@mui/icons-material';
import { adminApi } from '../../api/adminApi';
import type { AuditLog, AuditAction } from '../../types/admin';

const ACTION_COLOR: Record<AuditAction, 'default' | 'success' | 'error' | 'warning' | 'info' | 'primary'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'error',
  SOFT_DELETE: 'warning',
  LOGIN_SUCCESS: 'success',
  LOGIN_FAILURE: 'error',
  LOGOUT: 'default',
  PASSWORD_CHANGE: 'info',
  PASSWORD_RESET: 'warning',
  EMAIL_VERIFIED: 'success',
  ACCOUNT_LOCKED: 'error',
  ACCOUNT_UNLOCKED: 'success',
};

const AUDIT_ACTIONS: AuditAction[] = [
  'CREATE', 'UPDATE', 'DELETE', 'SOFT_DELETE',
  'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT',
  'PASSWORD_CHANGE', 'PASSWORD_RESET',
  'EMAIL_VERIFIED', 'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED',
];

function fmtDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function AuditRow({ log }: { log: AuditLog }) {
  const [expanded, setExpanded] = useState(false);
  const hasDiff = log.oldValues != null || log.newValues != null;

  return (
    <>
      <TableRow hover>
        <TableCell>
          <Typography variant="caption" sx={{ fontFamily: 'monospace' }}>
            {log.entityType ?? '—'}
          </Typography>
        </TableCell>
        <TableCell>
          <Chip
            label={log.action.replace('_', ' ')}
            size="small"
            color={ACTION_COLOR[log.action] ?? 'default'}
          />
        </TableCell>
        <TableCell>
          <Typography variant="caption">{log.actorEmail ?? '—'}</Typography>
          {log.actorRole && (
            <Typography variant="caption" sx={{ display: 'block', color: 'text.secondary' }}>
              {log.actorRole}
            </Typography>
          )}
        </TableCell>
        <TableCell>
          <Typography variant="caption">{fmtDate(log.createdAt)}</Typography>
        </TableCell>
        <TableCell align="right">
          {hasDiff && (
            <Tooltip title={expanded ? 'Hide diff' : 'Show diff'}>
              <IconButton size="small" onClick={() => setExpanded((e) => !e)}>
                {expanded ? (
                  <ExpandLessRounded fontSize="small" />
                ) : (
                  <ExpandMoreRounded fontSize="small" />
                )}
              </IconButton>
            </Tooltip>
          )}
        </TableCell>
      </TableRow>

      {hasDiff && (
        <TableRow>
          <TableCell colSpan={5} sx={{ py: 0 }}>
            <Collapse in={expanded} unmountOnExit>
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: log.oldValues && log.newValues ? '1fr 1fr' : '1fr',
                  gap: 2,
                  py: 2,
                  px: 1,
                }}
              >
                {log.oldValues && (
                  <Box>
                    <Typography
                      variant="caption"
                      sx={{ fontWeight: 600, color: 'error.main', display: 'block', mb: 0.5 }}
                    >
                      Before
                    </Typography>
                    <Box
                      component="pre"
                      sx={{
                        fontSize: '0.7rem', bgcolor: 'grey.50', p: 1, borderRadius: 1,
                        overflow: 'auto', maxHeight: 200, m: 0, fontFamily: 'monospace',
                        border: '1px solid', borderColor: 'error.light',
                      }}
                    >
                      {JSON.stringify(log.oldValues, null, 2)}
                    </Box>
                  </Box>
                )}
                {log.newValues && (
                  <Box>
                    <Typography
                      variant="caption"
                      sx={{ fontWeight: 600, color: 'success.main', display: 'block', mb: 0.5 }}
                    >
                      After
                    </Typography>
                    <Box
                      component="pre"
                      sx={{
                        fontSize: '0.7rem', bgcolor: 'grey.50', p: 1, borderRadius: 1,
                        overflow: 'auto', maxHeight: 200, m: 0, fontFamily: 'monospace',
                        border: '1px solid', borderColor: 'success.light',
                      }}
                    >
                      {JSON.stringify(log.newValues, null, 2)}
                    </Box>
                  </Box>
                )}
              </Box>
            </Collapse>
          </TableCell>
        </TableRow>
      )}
    </>
  );
}

export default function AuditLogPage() {
  const [entityType, setEntityType] = useState('');
  const [action, setAction] = useState<AuditAction | ''>('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;

  const params = {
    entityType: entityType || undefined,
    action: (action || undefined) as AuditAction | undefined,
    from: from ? new Date(from).toISOString() : undefined,
    to: to ? new Date(to).toISOString() : undefined,
    page,
    size: PAGE_SIZE,
  };

  const { data: auditPage, isLoading } = useQuery({
    queryKey: ['admin-audit-log', params],
    queryFn: () => adminApi.getAuditLog(params),
  });

  const logs = auditPage?.content ?? [];
  const totalPages = auditPage?.totalPages ?? 1;

  return (
    <Box>
      <Typography variant="h5" sx={{ fontWeight: 700, mb: 3 }}>
        Audit Log
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <TextField
          label="Entity type"
          value={entityType}
          onChange={(e) => { setEntityType(e.target.value); setPage(0); }}
          size="small"
          placeholder="e.g. Job, User"
          sx={{ minWidth: 160 }}
        />
        <FormControl size="small" sx={{ minWidth: 180 }}>
          <InputLabel>Action</InputLabel>
          <Select
            label="Action"
            value={action}
            onChange={(e) => { setAction(e.target.value as AuditAction | ''); setPage(0); }}
          >
            <MenuItem value="">All actions</MenuItem>
            {AUDIT_ACTIONS.map((a) => (
              <MenuItem key={a} value={a}>
                {a.replace('_', ' ')}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <Box>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
            From
          </Typography>
          <TextField
            type="datetime-local"
            value={from}
            onChange={(e) => { setFrom(e.target.value); setPage(0); }}
            size="small"
          />
        </Box>
        <Box>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block', mb: 0.5 }}>
            To
          </Typography>
          <TextField
            type="datetime-local"
            value={to}
            onChange={(e) => { setTo(e.target.value); setPage(0); }}
            size="small"
          />
        </Box>
      </Box>

      <Card>
        {isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : logs.length === 0 ? (
          <Box sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">No audit log entries found.</Typography>
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600 }}>Entity</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Action</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Actor</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Timestamp</TableCell>
                  <TableCell sx={{ fontWeight: 600 }} align="right">Diff</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {logs.map((log) => (
                  <AuditRow key={log.id} log={log} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Card>

      {totalPages > 1 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
          <Pagination
            count={totalPages}
            page={page + 1}
            onChange={(_, v) => setPage(v - 1)}
            color="primary"
            size="small"
          />
        </Box>
      )}
    </Box>
  );
}
