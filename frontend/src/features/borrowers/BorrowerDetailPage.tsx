/**
 * Borrower Detail Page
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
  IconButton,
  Breadcrumbs,
  Link,
  Paper,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon from '@mui/icons-material/Edit';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import PhoneIcon from '@mui/icons-material/Phone';
import EmailIcon from '@mui/icons-material/Email';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import VisibilityIcon from '@mui/icons-material/Visibility';
import AddIcon from '@mui/icons-material/Add';
import { borrowerApi } from '../../api/borrower.api';
import { loanApi } from '../../api/loan.api';
import type { Borrower, Loan } from '../../types';
import { logger } from '../../utils/logger';

const MODULE = 'BorrowerDetailPage';

const BorrowerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [borrower, setBorrower] = useState<Borrower | null>(null);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    if (!id) return;

    logger.info(MODULE, `Fetching details for borrower: ${id}`);
    setLoading(true);
    setError(null);

    try {
      // Fetch borrower details and loans in parallel
      const [borrowerData, loansData] = await Promise.all([
        borrowerApi.getById(id),
        loanApi.getAll({ borrowerId: id, size: 100, page: 0 }) // Fetch all loans for this borrower
      ]);

      setBorrower(borrowerData);
      setLoans(loansData.content);
      logger.info(MODULE, 'Fetched borrower details and loans successfully');
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch borrower details', err);
      setError('Failed to load borrower details');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'CLOSED': return 'success';
      case 'BLOCKED': return 'error';
      case 'DEFAULTED': return 'error';
      case 'DELETED': return 'default';
      default: return 'default';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
    }).format(amount);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error || !borrower) {
    return (
      <Box>
        <Alert severity="error">{error || 'Borrower not found'}</Alert>
        <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/borrowers')} sx={{ mt: 2 }}>
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
        <Link underline="hover" color="inherit" onClick={() => navigate('/borrowers')} sx={{ cursor: 'pointer' }}>
          Borrowers
        </Link>
        <Typography color="text.primary">{borrower.fullName}</Typography>
      </Breadcrumbs>

      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate('/borrowers')}
            sx={{ mr: 2 }}
          >
            Back
          </Button>
          <Typography variant="h4" fontWeight="bold">
            {borrower.fullName}
          </Typography>
          <Chip
            label={borrower.status}
            color={getStatusColor(borrower.status) as any}
            sx={{ ml: 2 }}
          />
        </Box>
        <Box>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate('/loans/new', { state: { borrowerId: borrower.id } })}
            sx={{ mr: 1 }}
          >
            New Loan
          </Button>
          <Button
            variant="outlined"
            startIcon={<EditIcon />}
            onClick={() => navigate(`/borrowers/${id}/edit`)}
          >
            Edit Profile
          </Button>
        </Box>
      </Box>

      <Grid container spacing={3}>
        {/* Borrower Profile Card */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Contact Information
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <PhoneIcon color="action" sx={{ mr: 2 }} />
                <Box>
                  <Typography variant="body2" color="text.secondary">Phone</Typography>
                  <Typography variant="body1">{borrower.phone}</Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <EmailIcon color="action" sx={{ mr: 2 }} />
                <Box>
                  <Typography variant="body2" color="text.secondary">Email</Typography>
                  <Typography variant="body1">{borrower.email || 'N/A'}</Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'start', mb: 2 }}>
                <LocationOnIcon color="action" sx={{ mr: 2, mt: 0.5 }} />
                <Box>
                  <Typography variant="body2" color="text.secondary">Address</Typography>
                  <Typography variant="body1" sx={{ whiteSpace: 'pre-line' }}>
                    {borrower.address || 'N/A'}
                  </Typography>
                </Box>
              </Box>

              <Box sx={{ mt: 3 }}>
                <Typography variant="body2" color="text.secondary">Member Since</Typography>
                <Typography variant="body1">
                  {new Date(borrower.createdAt).toLocaleDateString()}
                </Typography>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Loan History Card */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">
                  Loan History
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Total Loans: {loans.length}
                </Typography>
              </Box>
              <Divider sx={{ mb: 2 }} />

              {loans.length === 0 ? (
                <Alert severity="info">No loans found for this borrower.</Alert>
              ) : (
                <TableContainer component={Paper} elevation={0} variant="outlined">
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Loan ID</TableCell>
                        <TableCell>Principal</TableCell>
                        <TableCell>Outstanding</TableCell>
                        <TableCell>Status</TableCell>
                        <TableCell>Date</TableCell>
                        <TableCell align="right">Actions</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {loans.map((loan) => (
                        <TableRow key={loan.id} hover>
                          <TableCell sx={{ fontFamily: 'monospace' }}>
                            {loan.id.substring(0, 8)}...
                          </TableCell>
                          <TableCell>{formatCurrency(loan.principalAmount)}</TableCell>
                          <TableCell>
                            <Typography
                              color={loan.totalOutstanding > 0 ? 'error.main' : 'success.main'}
                              fontWeight="medium"
                              variant="body2"
                            >
                              {formatCurrency(loan.totalOutstanding)}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={loan.status}
                              size="small"
                              color={getStatusColor(loan.status) as any}
                              variant="outlined"
                            />
                          </TableCell>
                          <TableCell>
                            {new Date(loan.createdAt).toLocaleDateString()}
                          </TableCell>
                          <TableCell align="right">
                            <IconButton
                              size="small"
                              onClick={() => navigate(`/loans/${loan.id}`)}
                              title="View Loan Details"
                            >
                              <VisibilityIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default BorrowerDetailPage;
