/**
 * Payments List Page
 */

import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Chip,
  IconButton,
  Alert,
  Breadcrumbs,
  Link,
  Tooltip,
} from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import VisibilityIcon from '@mui/icons-material/Visibility';
import RefreshIcon from '@mui/icons-material/Refresh';
import paymentApi from '../../api/payment.api';
import type { Payment, PaymentMode } from '../../types';
import { logger } from '../../utils/logger';
import DataTable from '../../components/tables/DataTable';
import { formatCurrency } from '../../utils/currency';

const MODULE = 'PaymentsPage';

const PaymentsPage: React.FC = () => {
  const navigate = useNavigate();
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);

  logger.debug(MODULE, 'Rendering PaymentsPage');

  const fetchPayments = useCallback(async () => {
    logger.info(MODULE, 'Fetching payments', { page, rowsPerPage });
    setLoading(true);
    setError(null);

    try {
      const response = await paymentApi.getAll({ page, size: rowsPerPage });
      logger.info(MODULE, `Fetched ${response.content.length} payments`);
      setPayments(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch payments', err);
      setError('Failed to load payments. Please try again later.');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchPayments();
  }, [fetchPayments]);

  const getModeColor = (mode: PaymentMode) => {
    switch (mode) {
      case 'CASH':
        return 'success';
      case 'UPI':
        return 'info';
      case 'BANK':
        return 'primary';
      case 'CHEQUE':
        return 'warning';
      default:
        return 'default';
    }
  };

  const columns = [
    {
      id: 'paymentDate',
      label: 'Date',
      format: (value: string) => new Date(value).toLocaleDateString(),
    },
    {
      id: 'amountPaid',
      label: 'Amount',
      format: (value: number) => (
        <Typography fontWeight="medium" color="success.main">
          {formatCurrency(value)}
        </Typography>
      ),
    },
    {
      id: 'reference',
      label: 'Reference',
      format: (value: string) => value || '-',
    },
    {
      id: 'mode',
      label: 'Mode',
      format: (value: PaymentMode) => (
        <Chip label={value} size="small" color={getModeColor(value)} variant="outlined" />
      ),
    },
    {
      id: 'loanId',
      label: 'Loan ID',
      format: (value: string) => (
        <Link
          component="button"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/loans/${value}`);
          }}
          sx={{ fontFamily: 'monospace' }}
        >
          {value.substring(0, 8)}...
        </Link>
      ),
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right' as const,
      format: (_: any, row: Payment) => (
        <Tooltip title="View Loan Details">
            <IconButton
            size="small"
            onClick={(e) => {
                e.stopPropagation();
                navigate(`/loans/${row.loanId}`);
            }}
            >
            <VisibilityIcon />
            </IconButton>
        </Tooltip>
      ),
    },
  ];

  return (
    <Box>
      <Breadcrumbs
        separator={<NavigateNextIcon fontSize="small" />}
        aria-label="breadcrumb"
        sx={{ mb: 3 }}
      >
        <Link underline="hover" color="inherit" onClick={() => navigate('/dashboard')} sx={{ cursor: 'pointer' }}>
          Dashboard
        </Link>
        <Typography color="text.primary">Payments</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Payments History
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View all payments across your loans
          </Typography>
        </Box>
        <IconButton onClick={fetchPayments} disabled={loading}>
            <RefreshIcon />
        </IconButton>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Card>
        <DataTable
          columns={columns}
          data={payments}
          loading={loading}
          totalElements={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onRowClick={(row) => navigate(`/loans/${row.loanId}`)}
          emptyMessage="No payments found"
        />
      </Card>
    </Box>
  );
};

export default PaymentsPage;
