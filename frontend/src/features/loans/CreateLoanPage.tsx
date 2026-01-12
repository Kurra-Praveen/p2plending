/**
 * Create Loan Page
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
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
  Autocomplete,
  TextField,
  InputAdornment,
} from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { loanApi } from '../../api/loan.api';
import { borrowerApi } from '../../api/borrower.api';
import { logger } from '../../utils/logger';
import { rupeesToPaise } from '../../utils/currency';
import type { Borrower } from '../../types';
import { FormInput, FormSelect } from '../../components/forms';

const MODULE = 'CreateLoanPage';

// Validation Schema
const loanSchema = z.object({
  borrowerId: z.string().min(1, 'Borrower is required'),
  principal: z.number().min(1000, 'Minimum principal is 1,000').max(100000000, 'Maximum principal is 100,000,000'),
  interestRate: z.number().min(1, 'Minimum interest rate is 1%').max(100, 'Maximum interest rate is 100%'),
  interestType: z.enum(['FLAT', 'REDUCING'] as const),
  tenureMonths: z.number().min(1, 'Minimum tenure is 1 month').max(360, 'Maximum tenure is 360 months'),
});

type LoanFormData = z.infer<typeof loanSchema>;

const CreateLoanPage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [serverError, setServerError] = useState<string | null>(null);
  const [borrowers, setBorrowers] = useState<Borrower[]>([]);
  const [loadingBorrowers, setLoadingBorrowers] = useState(false);

  // Get pre-selected borrower from navigation state
  const preSelectedBorrowerId = location.state?.borrowerId;

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<LoanFormData>({
    resolver: zodResolver(loanSchema),
    defaultValues: {
      borrowerId: preSelectedBorrowerId || '',
      principal: 10000,
      interestRate: 12,
      interestType: 'REDUCING',
      tenureMonths: 12,
    },
  });

  // Load borrowers for dropdown
  useEffect(() => {
    const fetchBorrowers = async () => {
      setLoadingBorrowers(true);
      try {
        // Fetching first 100 active borrowers for now
        // In a real app, this should be a search/autocomplete API
        const response = await borrowerApi.getAll({ size: 100, status: 'ACTIVE', page: 0 });
        setBorrowers(response.content);

        // If we have a pre-selected ID, ensure it's set
        if (preSelectedBorrowerId) {
          setValue('borrowerId', preSelectedBorrowerId);
        }
      } catch (err) {
        logger.error(MODULE, 'Failed to fetch borrowers', err);
        setServerError('Failed to load borrowers list');
      } finally {
        setLoadingBorrowers(false);
      }
    };

    fetchBorrowers();
  }, [preSelectedBorrowerId, setValue]);

  const onSubmit = async (data: LoanFormData) => {
    logger.info(MODULE, 'Submitting new loan request', data);
    setServerError(null);

    try {
      const loan = await loanApi.create({
        borrowerId: data.borrowerId,
        principal: rupeesToPaise(data.principal), // Convert rupees to paise for backend
        interestRate: data.interestRate,
        interestType: data.interestType,
        tenureMonths: data.tenureMonths,
      });

      logger.info(MODULE, 'Loan created successfully', loan.loanId);
      navigate(`/loans/${loan.loanId}`);
    } catch (err: any) {
      logger.error(MODULE, 'Failed to create loan', err);
      const message = err.response?.data?.message || 'Failed to create loan. Please check the details and try again.';
      setServerError(message);
    }
  };

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
        <Typography color="text.primary">New Loan</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/loans')}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" fontWeight="bold">
          Create New Loan
        </Typography>
      </Box>

      {serverError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {serverError}
        </Alert>
      )}

      <Card>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)}>
            <Grid container spacing={3}>
              {/* Borrower Selection */}
              <Grid size={{ xs: 12 }}>
                <Controller
                  name="borrowerId"
                  control={control}
                  render={({ field }) => (
                    <Autocomplete
                      options={borrowers}
                      getOptionLabel={(option) => `${option.fullName} (${option.phone})`}
                      loading={loadingBorrowers}
                      value={borrowers.find(b => b.id === field.value) || null}
                      onChange={(_, newValue) => {
                        field.onChange(newValue ? newValue.id : '');
                      }}
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          label="Select Borrower"
                          error={!!errors.borrowerId}
                          helperText={errors.borrowerId?.message}
                          required
                        />
                      )}
                    />
                  )}
                />
              </Grid>

              {/* Principal Amount */}
              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="principal"
                  control={control}
                  label="Principal Amount"
                  type="number"
                  fullWidth
                  required
                  InputProps={{
                    startAdornment: <InputAdornment position="start">₹</InputAdornment>,
                  }}
                />
              </Grid>

              {/* Tenure */}
              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="tenureMonths"
                  control={control}
                  label="Tenure (Months)"
                  type="number"
                  fullWidth
                  required
                />
              </Grid>

              {/* Interest Rate */}
              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="interestRate"
                  control={control}
                  label="Interest Rate (% per annum)"
                  type="number"
                  fullWidth
                  required
                  InputProps={{
                    endAdornment: <InputAdornment position="end">%</InputAdornment>,
                  }}
                />
              </Grid>

              {/* Interest Type */}
              <Grid size={{ xs: 12, md: 6 }}>
                <FormSelect
                  name="interestType"
                  control={control}
                  label="Interest Type"
                  fullWidth
                  required
                  options={[
                    { label: 'Reducing Balance', value: 'REDUCING' },
                    { label: 'Flat Rate', value: 'FLAT' },
                  ]}
                />
              </Grid>

              <Grid size={{ xs: 12 }}>
                <Alert severity="info">
                  Note: The EMI schedule will be generated automatically upon creation.
                </Alert>
              </Grid>

              <Grid size={{ xs: 12 }} sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                <Button
                  variant="outlined"
                  onClick={() => navigate('/loans')}
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
                  {isSubmitting ? 'Creating...' : 'Create Loan'}
                </Button>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
};

export default CreateLoanPage;
