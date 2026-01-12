/**
 * Reports Page
 * Displays collections summary and other reports
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Grid,
  CircularProgress,
  Alert,
  Paper,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import GetAppIcon from '@mui/icons-material/GetApp';
import { reportApi } from '../../api/report.api';
import type { CollectionsSummary } from '../../types';
import { logger } from '../../utils/logger';
import { formatCurrency, paiseToRupees } from '../../utils/currency';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const MODULE = 'ReportsPage';

const ReportsPage: React.FC = () => {
  const [startDate, setStartDate] = useState<string>(
    new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0]
  ); // Start of current month
  const [endDate, setEndDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  ); // Today
  const [summary, setSummary] = useState<CollectionsSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchReport = async () => {
    logger.info(MODULE, 'Fetching collections report', { startDate, endDate });
    setLoading(true);
    setError(null);

    try {
      const data = await reportApi.getCollectionsSummary(startDate, endDate);
      setSummary(data);
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch collections report', err);
      setError('Failed to load collections report');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = () => {
    if (!summary) return;

    const csvContent = [
      ['Category', 'Amount (INR)'],
      ['Total Collected', paiseToRupees(summary.totalCollected)],
      ['Principal Collected', paiseToRupees(summary.principalCollected)],
      ['Interest Collected', paiseToRupees(summary.interestCollected)],
      ['Penalty Collected', paiseToRupees(summary.penaltyCollected)],
    ]
      .map((e) => e.join(','))
      .join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `collections_report_${startDate}_${endDate}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const chartData = summary ? [
    {
      name: 'Collected',
      Principal: summary.principalCollected,
      Interest: summary.interestCollected,
      Penalty: summary.penaltyCollected,
    }
  ] : [];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Reports
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Generate collection reports
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<GetAppIcon />}
          onClick={handleExport}
          disabled={!summary}
        >
          Export CSV
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <Grid container spacing={2} alignItems="center">
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="Start Date"
                type="date"
                fullWidth
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                label="End Date"
                type="date"
                fullWidth
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <Button
                variant="contained"
                startIcon={<SearchIcon />}
                onClick={fetchReport}
                fullWidth
                sx={{ height: 56 }}
              >
                Generate Report
              </Button>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {summary && !loading && (
        <Grid container spacing={3}>
          {/* Summary Cards */}
          <Grid size={{ xs: 12, md: 3 }}>
            <Paper sx={{ p: 2, textAlign: 'center', height: '100%', bgcolor: 'success.light', color: 'success.contrastText' }}>
              <Typography variant="body2" sx={{ opacity: 0.9 }}>Total Collected</Typography>
              <Typography variant="h5" fontWeight="bold">{formatCurrency(summary.totalCollected)}</Typography>
            </Paper>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <Paper sx={{ p: 2, textAlign: 'center', height: '100%' }}>
              <Typography variant="body2" color="text.secondary">Principal Collected</Typography>
              <Typography variant="h6">{formatCurrency(summary.principalCollected)}</Typography>
            </Paper>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <Paper sx={{ p: 2, textAlign: 'center', height: '100%' }}>
              <Typography variant="body2" color="text.secondary">Interest Collected</Typography>
              <Typography variant="h6">{formatCurrency(summary.interestCollected)}</Typography>
            </Paper>
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <Paper sx={{ p: 2, textAlign: 'center', height: '100%' }}>
              <Typography variant="body2" color="text.secondary">Penalty Collected</Typography>
              <Typography variant="h6" color="error.main">{formatCurrency(summary.penaltyCollected)}</Typography>
            </Paper>
          </Grid>

          {/* Chart */}
          <Grid size={{ xs: 12 }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>Collection Breakdown</Typography>
                <Box sx={{ height: 400, mt: 2 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={chartData}
                      margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="name" />
                      <YAxis />
                      <Tooltip formatter={(value) => formatCurrency(value as number)} />
                      <Legend />
                      <Bar dataKey="Principal" stackId="a" fill="#1976d2" />
                      <Bar dataKey="Interest" stackId="a" fill="#2e7d32" />
                      <Bar dataKey="Penalty" stackId="a" fill="#d32f2f" />
                    </BarChart>
                  </ResponsiveContainer>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default ReportsPage;
