/**
 * Create Borrower Page
 */

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
} from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { borrowerApi } from '../../api/borrower.api';
import { logger } from '../../utils/logger';
import { FormInput } from '../../components/forms';

const MODULE = 'CreateBorrowerPage';

// Validation Schema
const borrowerSchema = z.object({
  fullName: z.string().min(3, 'Name must be at least 3 characters').max(100),
  phone: z.string().regex(/^\d{10}$/, 'Phone number must be 10 digits'),
  email: z.string().email('Invalid email address').optional().or(z.literal('')),
  address: z.string().optional(),
});

type BorrowerFormData = z.infer<typeof borrowerSchema>;

const CreateBorrowerPage: React.FC = () => {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    control,
    handleSubmit,
    formState: { isSubmitting },
  } = useForm<BorrowerFormData>({
    resolver: zodResolver(borrowerSchema),
    defaultValues: {
      fullName: '',
      phone: '',
      email: '',
      address: '',
    },
  });

  const onSubmit = async (data: BorrowerFormData) => {
    logger.info(MODULE, 'Submitting new borrower', data);
    setServerError(null);

    try {
      await borrowerApi.create({
        fullName: data.fullName,
        phone: data.phone,
        email: data.email || undefined,
        address: data.address || undefined,
      });

      logger.info(MODULE, 'Borrower created successfully');
      navigate('/borrowers');
    } catch (err: any) {
      logger.error(MODULE, 'Failed to create borrower', err);
      // Handle backend validation errors or generic errors
      const message = err.response?.data?.message || 'Failed to create borrower. Please try again.';
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
        <Link underline="hover" color="inherit" onClick={() => navigate('/borrowers')} sx={{ cursor: 'pointer' }}>
          Borrowers
        </Link>
        <Typography color="text.primary">New Borrower</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => navigate('/borrowers')}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h4" fontWeight="bold">
          Add New Borrower
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
              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="fullName"
                  control={control}
                  label="Full Name"
                  fullWidth
                  required
                />
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="phone"
                  control={control}
                  label="Phone Number"
                  fullWidth
                  required
                  inputProps={{ maxLength: 10 }}
                />
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <FormInput
                  name="email"
                  control={control}
                  label="Email Address"
                  fullWidth
                  type="email"
                />
              </Grid>

              <Grid size={{ xs: 12 }}>
                <FormInput
                  name="address"
                  control={control}
                  label="Address"
                  fullWidth
                  multiline
                  rows={3}
                />
              </Grid>

              <Grid size={{ xs: 12 }} sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                <Button
                  variant="outlined"
                  onClick={() => navigate('/borrowers')}
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
                  {isSubmitting ? 'Saving...' : 'Save Borrower'}
                </Button>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>
    </Box>
  );
};

export default CreateBorrowerPage;
