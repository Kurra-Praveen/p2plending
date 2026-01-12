/**
 * Loans List Page
 */

import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Button,
  Chip,
  IconButton,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { loanApi } from '../../api/loan.api';
import type { Loan, LoanStatus } from '../../types';
import { logger } from '../../utils/logger';
import { formatCurrency } from '../../utils/currency';

import DataTable from '../../components/tables/DataTable';

const MODULE = 'LoansPage';

const LoansPage: React.FC = () => {
  const navigate = useNavigate();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [statusFilter, setStatusFilter] = useState<LoanStatus | ''>('');

  const fetchLoans = useCallback(async () => {
    logger.info(MODULE, 'Fetching loans', { page, rowsPerPage, statusFilter });
    setLoading(true);
    setError(null);

    try {
      const response = await loanApi.getAll({
        page,
        size: rowsPerPage,
        status: statusFilter || undefined,
      });
      logger.info(MODULE, `Fetched ${response.content.length} loans`);
      setLoans(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch loans', err);
      setError('Failed to load loans');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage, statusFilter]);

  useEffect(() => {
    fetchLoans();
  }, [fetchLoans]);

  const getStatusColor = (status: LoanStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'CLOSED':
        return 'info';
      case 'DEFAULTED':
        return 'error';
      case 'CREATED':
        return 'warning';
      default:
        return 'default';
    }
  };

  const columns = [
    {
      id: 'loanId',
      label: 'Loan ID',
      format: (value: string) => (
        <span style={{ fontFamily: 'monospace' }}>{value.substring(0, 8)}...</span>
      ),
    },
    {
      id: 'borrowerName',
      label: 'Borrower',
      format: (value: string, _row: Loan) => (
        <Box>
          <Typography fontWeight="medium">{value}</Typography>
        </Box>
      ),
    },
    {
      id: 'principal',
      label: 'Principal',
      format: (value: number) => formatCurrency(value),
    },
    {
      id: 'totalOutstanding',
      label: 'Outstanding',
      format: (value: number) => (
        <Typography
          color={value > 0 ? 'error.main' : 'text.primary'}
          fontWeight={value > 0 ? 'medium' : 'regular'}
          variant="body2"
        >
          {formatCurrency(value)}
        </Typography>
      ),
    },
    {
      id: 'tenureMonths',
      label: 'Tenure',
      format: (value: number) => `${value} months`,
    },
    {
      id: 'status',
      label: 'Status',
      format: (value: LoanStatus) => (
        <Chip label={value} size="small" color={getStatusColor(value) as any} />
      ),
    },
    {
      id: 'createdAt',
      label: 'Date',
      format: (value: string) => new Date(value).toLocaleDateString(),
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right' as const,
      format: (_: any, row: Loan) => (
        <IconButton
          size="small"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/loans/${row.loanId}`);
          }}
        >
          <VisibilityIcon />
        </IconButton>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Loans
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage and track all loans
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/loans/new')}
        >
          New Loan
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Card>
        {/* Filters */}
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <Stack direction="row" spacing={2}>
            <FormControl size="small" sx={{ minWidth: 200 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => {
                  setStatusFilter(e.target.value as LoanStatus | '');
                  setPage(0);
                }}
              >
                <MenuItem value="">All Statuses</MenuItem>
                <MenuItem value="CREATED">Created</MenuItem>
                <MenuItem value="ACTIVE">Active</MenuItem>
                <MenuItem value="CLOSED">Closed</MenuItem>
                <MenuItem value="DEFAULTED">Defaulted</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </Box>

        <DataTable
          columns={columns}
          data={loans.map(l => ({ ...l, id: l.loanId }))}
          loading={loading}
          totalElements={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onRowClick={(row) => navigate(`/loans/${row.loanId}`)}
          emptyMessage="No loans found"
        />
      </Card>
    </Box>
  );
};

export default LoansPage;
