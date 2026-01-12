/**
 * Overdue Loans Page
 * Displays list of all overdue loans
 */

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Chip,
  Alert,
  IconButton,
  Button,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import WarningIcon from '@mui/icons-material/Warning';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { reportApi } from '../../api/report.api';
import type { OverdueLoan } from '../../types';
import { logger } from '../../utils/logger';

import DataTable from '../../components/tables/DataTable';

const MODULE = 'OverduePage';

const OverduePage: React.FC = () => {
  const navigate = useNavigate();
  const [loans, setLoans] = useState<(OverdueLoan & { id: string })[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  useEffect(() => {
    const fetchOverdueLoans = async () => {
      logger.info(MODULE, 'Fetching overdue loans');
      setLoading(true);
      setError(null);

      try {
        const data = await reportApi.getOverdueLoans();
        // Map loanId to id for DataTable
        const mappedData = data.map(l => ({ ...l, id: l.loanId }));
        setLoans(mappedData);
        logger.info(MODULE, `Fetched ${data.length} overdue loans`);
      } catch (err) {
        logger.error(MODULE, 'Failed to fetch overdue loans', err);
        setError('Failed to load overdue loans');
      } finally {
        setLoading(false);
      }
    };

    fetchOverdueLoans();
  }, []);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
    }).format(amount);
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
      format: (value: string) => <Typography fontWeight="medium">{value}</Typography>,
    },
    { id: 'borrowerPhone', label: 'Phone' },
    {
      id: 'totalOutstanding',
      label: 'Amount Overdue',
      align: 'right' as const,
      format: (value: number) => (
        <Typography color="error" fontWeight="bold">
          {formatCurrency(value)}
        </Typography>
      ),
    },
    {
      id: 'daysOverdue',
      label: 'Days Overdue',
      align: 'center' as const,
      format: (value: number) => (
        <Chip
          icon={<WarningIcon />}
          label={`${value} Days`}
          color="error"
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      id: 'overdueEmiCount',
      label: 'Missed EMIs',
      align: 'center' as const,
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right' as const,
      format: (_: any, row: OverdueLoan & { id: string }) => (
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

  // Client-side pagination
  const paginatedLoans = loans.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/reports')}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Box>
          <Typography variant="h4" fontWeight="bold" color="error">
            Overdue Loans
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Loans with missed payments
          </Typography>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Card>
        <DataTable
          columns={columns}
          data={paginatedLoans}
          loading={loading}
          totalElements={loans.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onRowClick={(row) => navigate(`/loans/${row.loanId}`)}
          emptyMessage="No Overdue Loans! Your portfolio is healthy."
        />
      </Card>
    </Box>
  );
};

export default OverduePage;
