/**
 * Loan Detail Page
 */

import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Chip,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  CircularProgress,
  Alert,
  Paper,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Stack,
  Breadcrumbs,
  Link,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import PaymentIcon from '@mui/icons-material/Payment';
import CancelIcon from '@mui/icons-material/Cancel';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import { loanApi } from '../../api/loan.api';
import type { Loan, RepaymentSchedule, PaymentMode } from '../../types';
import { logger } from '../../utils/logger';
import { formatCurrency } from '../../utils/currency';

const MODULE = 'LoanDetailPage';

const LoanDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loan, setLoan] = useState<Loan | null>(null);
  const [schedule, setSchedule] = useState<RepaymentSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Disbursement Dialog State
  const [openDisburseDialog, setOpenDisburseDialog] = useState(false);
  const [disburseMode, setDisburseMode] = useState<PaymentMode>('BANK');
  const [disburseReference, setDisburseReference] = useState('');
  const [disbursing, setDisbursing] = useState(false);

  // Close Loan Dialog State
  const [openCloseDialog, setOpenCloseDialog] = useState(false);
  const [closing, setClosing] = useState(false);

  const fetchData = useCallback(async () => {
    if (!id) return;

    logger.info(MODULE, `Fetching details for loan: ${id}`);
    setLoading(true);
    setError(null);

    try {
      const [loanData, scheduleData] = await Promise.all([
        loanApi.getById(id),
        loanApi.getSchedule(id),
      ]);

      setLoan(loanData);
      setSchedule(scheduleData);
      logger.info(MODULE, 'Fetched loan details and schedule successfully');
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch loan details', err);
      setError('Failed to load loan details');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleDisburse = async () => {
    if (!loan || !id) return;

    setDisbursing(true);
    try {
      await loanApi.disburse(id, {
        amount: loan.principal,
        mode: disburseMode,
        reference: disburseReference,
      });
      logger.info(MODULE, 'Loan disbursed successfully');
      setOpenDisburseDialog(false);
      fetchData(); // Refresh data
    } catch (err) {
      logger.error(MODULE, 'Failed to disburse loan', err);
      // Ideally show a toast here
    } finally {
      setDisbursing(false);
    }
  };

  const handleCloseLoan = async () => {
    if (!loan || !id) return;

    setClosing(true);
    try {
      await loanApi.close(id);
      logger.info(MODULE, 'Loan closed successfully');
      setOpenCloseDialog(false);
      fetchData(); // Refresh data
    } catch (err) {
      logger.error(MODULE, 'Failed to close loan', err);
    } finally {
      setClosing(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'CLOSED': return 'info';
      case 'DEFAULTED': return 'error';
      case 'WRITTEN_OFF': return 'default';
      case 'CREATED': return 'warning';
      case 'PAID': return 'success';
      case 'PARTIAL': return 'warning';
      case 'OVERDUE': return 'error';
      case 'PENDING': return 'default';
      default: return 'default';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !loan) {
    return (
      <Box>
        <Alert severity="error">{error || 'Loan not found'}</Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/loans')} sx={{ mt: 2 }}>
          Back to List
        </Button>
      </Box>
    );
  }

  return (
    <Box>
       {/* Breadcrumbs */}
       <Breadcrumbs
        separator={<NavigateNextIcon fontSize="small" />}
        aria-label="breadcrumb"
        sx={{ mb: 3 }}
      >
        <Link underline="hover" color="inherit" onClick={() => navigate('/dashboard')} sx={{ cursor: 'pointer' }}>
          Dashboard
        </Link>
        <Link underline="hover" color="inherit" onClick={() => navigate('/loans')} sx={{ cursor: 'pointer' }}>
          Loans
        </Link>
        <Typography color="text.primary">Loan Details</Typography>
      </Breadcrumbs>

      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/loans')}
            sx={{ mr: 2 }}
          >
            Back
          </Button>
          <Box>
            <Typography variant="h4" fontWeight="bold">
              Loan #{loan.loanId.substring(0, 8)}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Borrower: <Link component="button" onClick={() => navigate(`/borrowers/${loan.borrowerId}`)}>{loan.borrowerName}</Link>
            </Typography>
          </Box>
          <Chip
            label={loan.status}
            color={getStatusColor(loan.status) as any}
            sx={{ ml: 2 }}
          />
        </Box>
        <Stack direction="row" spacing={2}>
          {loan.status === 'CREATED' && (
            <Button
              variant="contained"
              color="success"
              startIcon={<AccountBalanceWalletIcon />}
              onClick={() => setOpenDisburseDialog(true)}
            >
              Disburse Loan
            </Button>
          )}
          {loan.status === 'ACTIVE' && (
            <>
              <Button
                variant="contained"
                startIcon={<PaymentIcon />}
                onClick={() => navigate(`/loans/${loan.loanId}/payments/new`)}
              >
                Record Payment
              </Button>
              <Button
                variant="outlined"
                color="error"
                startIcon={<CancelIcon />}
                onClick={() => setOpenCloseDialog(true)}
                disabled={loan.totalOutstanding > 0}
                title={loan.totalOutstanding > 0 ? "Cannot close loan with outstanding balance" : "Close this loan"}
              >
                Close Loan
              </Button>
            </>
          )}
        </Stack>
      </Box>

      <Grid container spacing={3}>
        {/* Loan Overview Card */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Loan Overview
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Stack spacing={2}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Principal Amount</Typography>
                  <Typography fontWeight="medium">{formatCurrency(loan.principal)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Interest Rate</Typography>
                  <Typography fontWeight="medium">{loan.interestRate}% ({loan.interestType})</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Tenure</Typography>
                  <Typography fontWeight="medium">{loan.tenureMonths} Months</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">EMI Amount</Typography>
                  <Typography fontWeight="medium">{formatCurrency(loan.emiAmount)}</Typography>
                </Box>
                 <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Total Interest</Typography>
                  <Typography fontWeight="medium">{formatCurrency(loan.totalInterest)}</Typography>
                </Box>
                <Divider />
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary">Total Outstanding</Typography>
                  <Typography fontWeight="bold" color="error.main">
                    {formatCurrency(loan.totalOutstanding)}
                  </Typography>
                </Box>
                 <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary" variant="caption">Principal Outstanding</Typography>
                  <Typography variant="caption">{formatCurrency(loan.outstandingPrincipal)}</Typography>
                </Box>
                 <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary" variant="caption">Interest Outstanding</Typography>
                  <Typography variant="caption">{formatCurrency(loan.outstandingInterest)}</Typography>
                </Box>
                 <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography color="text.secondary" variant="caption">Penalty Outstanding</Typography>
                  <Typography variant="caption" color="error">{formatCurrency(loan.outstandingPenalty)}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        {/* Repayment Schedule Card */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Repayment Schedule
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <TableContainer component={Paper} elevation={0} variant="outlined" sx={{ maxHeight: 400 }}>
                <Table stickyHeader size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>#</TableCell>
                      <TableCell>Due Date</TableCell>
                      <TableCell align="right">Amount Due</TableCell>
                      <TableCell align="right">Paid</TableCell>
                      <TableCell align="center">Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {schedule.map((emi) => (
                      <TableRow key={emi.id} hover>
                        <TableCell>{emi.emiNo}</TableCell>
                        <TableCell>{new Date(emi.dueDate).toLocaleDateString()}</TableCell>
                        <TableCell align="right">{formatCurrency(emi.totalDue)}</TableCell>
                         <TableCell align="right">
                            {formatCurrency(emi.principalPaid + emi.interestPaid + emi.penaltyPaid)}
                         </TableCell>
                        <TableCell align="center">
                          <Chip
                            label={emi.status}
                            size="small"
                            color={getStatusColor(emi.status) as any}
                            variant="outlined"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Disburse Dialog */}
      <Dialog open={openDisburseDialog} onClose={() => setOpenDisburseDialog(false)}>
        <DialogTitle>Disburse Loan</DialogTitle>
        <DialogContent>
          <Typography sx={{ mb: 2 }}>
            Confirm disbursement of {formatCurrency(loan.principal)} to {loan.borrowerName}?
          </Typography>
          <TextField
            select
            label="Payment Mode"
            fullWidth
            value={disburseMode}
            onChange={(e) => setDisburseMode(e.target.value as PaymentMode)}
            sx={{ mb: 2 }}
          >
            <MenuItem value="BANK">Bank Transfer</MenuItem>
            <MenuItem value="CASH">Cash</MenuItem>
            <MenuItem value="CHEQUE">Cheque</MenuItem>
            <MenuItem value="UPI">UPI</MenuItem>
          </TextField>
          <TextField
            label="Reference ID / Notes"
            fullWidth
            value={disburseReference}
            onChange={(e) => setDisburseReference(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDisburseDialog(false)}>Cancel</Button>
          <Button
            onClick={handleDisburse}
            variant="contained"
            color="success"
            disabled={disbursing}
          >
            {disbursing ? 'Processing...' : 'Confirm Disbursement'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Close Loan Dialog */}
      <Dialog open={openCloseDialog} onClose={() => setOpenCloseDialog(false)}>
        <DialogTitle>Close Loan</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to close this loan? This action cannot be undone.
          </Typography>
          {loan.totalOutstanding > 0 && (
             <Alert severity="warning" sx={{ mt: 2 }}>
               There is an outstanding balance of {formatCurrency(loan.totalOutstanding)}. You cannot close this loan until it is fully paid.
             </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCloseDialog(false)}>Cancel</Button>
          <Button
            onClick={handleCloseLoan}
            variant="contained"
            color="error"
            disabled={closing || loan.totalOutstanding > 0}
          >
            {closing ? 'Closing...' : 'Close Loan'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default LoanDetailPage;
