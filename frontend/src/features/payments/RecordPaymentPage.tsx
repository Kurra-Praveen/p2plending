/**
 * Record Payment Page
 */

import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Grid,
  Alert,
  Breadcrumbs,
  Link,
  CircularProgress,
  InputAdornment,
  Divider,
} from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { loanApi } from '../../api/loan.api';
import { paymentApi } from '../../api/payment.api';
import { logger } from '../../utils/logger';
import { formatCurrency, paiseToRupees, rupeesToPaise } from '../../utils/currency';
import type { Loan } from '../../types';
import { FormInput, FormSelect } from '../../components/forms';

const MODULE = 'RecordPaymentPage';

// Validation Schema
const paymentSchema = z.object({
  amount: z.number().min(1, 'Amount must be greater than 0'),
  paymentDate: z.string().refine((val) => !isNaN(Date.parse(val)), 'Invalid date'),
  mode: z.enum(['CASH', 'UPI', 'BANK', 'CHEQUE'] as const),
  reference: z.string().optional(),
});

type PaymentFormData = z.infer<typeof paymentSchema>;

const RecordPaymentPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loan, setLoan] = useState<Loan | null>(null);
  const [loading, setLoading] = useState(true);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    setValue,
    formState: { isSubmitting },
  } = useForm<PaymentFormData>({
    resolver: zodResolver(paymentSchema),
    defaultValues: {
      amount: 0,
      paymentDate: new Date().toISOString().split('T')[0],
      mode: 'CASH',
      reference: '',
    },
  });

  useEffect(() => {
    const fetchLoan = async () => {
      if (!id) return;
      setLoading(true);
      try {
        const data = await loanApi.getById(id);
        setLoan(data);
        // Pre-fill amount with EMI amount or total outstanding if less (converted to rupees for display)
        const suggestedAmountPaise = Math.min(data.emiAmount, data.totalOutstanding);
        setValue('amount', paiseToRupees(suggestedAmountPaise));
      } catch (err) {
        logger.error(MODULE, 'Failed to fetch loan details', err);
        setServerError('Failed to load loan details');
      } finally {
        setLoading(false);
      }
    };

    fetchLoan();
  }, [id, setValue]);

  const onSubmit = async (data: PaymentFormData) => {
    if (!id) return;

    logger.info(MODULE, 'Submitting payment', data);
    setServerError(null);

    try {
      await paymentApi.record(id, {
        amount: rupeesToPaise(data.amount), // Convert rupees to paise for backend
        paymentDate: data.paymentDate,
        mode: data.mode,
        reference: data.reference,
      });

      logger.info(MODULE, 'Payment recorded successfully');
      navigate(`/loans/${id}`);
    } catch (err: any) {
      logger.error(MODULE, 'Failed to record payment', err);
      const message = err.response?.data?.message || 'Failed to record payment. Please try again.';
      setServerError(message);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!loan) {
    return (
      <Box>
        <Alert severity="error">Loan not found</Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/loans')} sx={{ mt: 2 }}>
          Back to Loans
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
        <Link underline="hover" color="inherit" onClick={() => navigate(`/loans/${id}`)} sx={{ cursor: 'pointer' }}>
          Loan #{id?.substring(0, 8)}
        </Link>
        <Typography color="text.primary">Record Payment</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate(`/loans/${id}`)}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" fontWeight="bold">
          Record Payment
        </Typography>
      </Box>

      {serverError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {serverError}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Loan Info Card */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card sx={{ bgcolor: 'primary.light', color: 'primary.contrastText' }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Loan Summary
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" sx={{ opacity: 0.9 }}>Borrower</Typography>
                <Typography variant="h6">{loan.borrowerName}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" sx={{ opacity: 0.9 }}>Total Outstanding</Typography>
                <Typography variant="h5" fontWeight="bold">{formatCurrency(loan.totalOutstanding)}</Typography>
              </Box>
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" sx={{ opacity: 0.9 }}>EMI Amount</Typography>
                <Typography variant="h6">{formatCurrency(loan.emiAmount)}</Typography>
              </Box>

              <Divider sx={{ my: 2, borderColor: 'rgba(255,255,255,0.2)' }} />

              <Box>
                <Typography variant="body2" sx={{ opacity: 0.9 }}>Breakdown</Typography>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 1 }}>
                  <Typography variant="body2">Principal</Typography>
                  <Typography variant="body2">{formatCurrency(loan.outstandingPrincipal)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">Interest</Typography>
                  <Typography variant="body2">{formatCurrency(loan.outstandingInterest)}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="body2">Penalty</Typography>
                  <Typography variant="body2">{formatCurrency(loan.outstandingPenalty)}</Typography>
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Payment Form */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <form onSubmit={handleSubmit(onSubmit)}>
                <Grid container spacing={3}>
                  <Grid size={{ xs: 12, md: 6 }}>
                    <FormInput
                      name="amount"
                      control={control}
                      label="Payment Amount"
                      type="number"
                      fullWidth
                      required
                      InputProps={{
                        startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                      }}
                    />
                  </Grid>

                  <Grid size={{ xs: 12, md: 6 }}>
                    <FormInput
                      name="paymentDate"
                      control={control}
                      label="Payment Date"
                      type="date"
                      fullWidth
                      required
                      InputLabelProps={{ shrink: true }}
                    />
                  </Grid>

                  <Grid size={{ xs: 12, md: 6 }}>
                    <FormSelect
                      name="mode"
                      control={control}
                      label="Payment Mode"
                      fullWidth
                      required
                      options={[
                        { label: 'Cash', value: 'CASH' },
                        { label: 'UPI', value: 'UPI' },
                        { label: 'Bank Transfer', value: 'BANK' },
                        { label: 'Cheque', value: 'CHEQUE' },
                      ]}
                    />
                  </Grid>

                  <Grid size={{ xs: 12, md: 6 }}>
                    <FormInput
                      name="reference"
                      control={control}
                      label="Reference / Transaction ID"
                      fullWidth
                      helperText="Optional"
                    />
                  </Grid>

                  <Grid size={{ xs: 12 }} sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 2 }}>
                    <Button
                      variant="outlined"
                      onClick={() => navigate(`/loans/${id}`)}
                      disabled={isSubmitting}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="contained"
                      startIcon={isSubmitting ? <CircularProgress size={20} color="inherit" /> : <SaveIcon />}
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? 'Recording...' : 'Record Payment'}
                    </Button>
                  </Grid>
                </Grid>
              </form>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default RecordPaymentPage;
