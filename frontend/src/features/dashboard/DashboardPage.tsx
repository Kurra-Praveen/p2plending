/**
 * Dashboard Page
 * Displays portfolio KPIs and charts
 */

import React, { useEffect, useState } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  CircularProgress,
  Alert,
} from '@mui/material';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import WarningIcon from '@mui/icons-material/Warning';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';
import { reportApi } from '../../api/report.api';
import type { PortfolioSummary } from '../../types';
import { logger } from '../../utils/logger';

const MODULE = 'DashboardPage';

// Format currency in INR
const formatCurrency = (value: number): string => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value / 100); // Convert paise to rupees
};

interface KpiCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: string;
  subtitle?: string;
}

const KpiCard: React.FC<KpiCardProps> = ({ title, value, icon, color, subtitle }) => (
  <Card sx={{ height: '100%' }}>
    <CardContent>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
        <Box>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            {title}
          </Typography>
          <Typography variant="h5" fontWeight="bold">
            {value}
          </Typography>
          {subtitle && (
            <Typography variant="caption" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        <Box
          sx={{
            p: 1,
            borderRadius: 1,
            bgcolor: `${color}.100`,
            color: `${color}.main`,
          }}
        >
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042'];

const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  logger.debug(MODULE, 'Rendering DashboardPage');

  useEffect(() => {
    const fetchData = async () => {
      logger.info(MODULE, 'Fetching portfolio summary');
      setLoading(true);
      setError(null);

      try {
        const data = await reportApi.getPortfolioSummary();
        logger.info(MODULE, 'Portfolio summary loaded', data);
        setSummary(data);
      } catch (err) {
        logger.error(MODULE, 'Failed to fetch portfolio summary', err);
        setError('Failed to load dashboard data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return <Alert severity="error">{error}</Alert>;
  }

  if (!summary) {
    return <Alert severity="info">No data available</Alert>;
  }

  // Prepare chart data
  const loanStatusData = [
    { name: 'Active', value: summary.activeLoans },
    { name: 'Closed', value: summary.closedLoans },
    { name: 'Defaulted', value: summary.defaultedLoans },
  ];

  // Backend does not currently provide breakdown of outstanding amounts
  // Placeholder or removal of breakdown chart would be appropriate here.
  // For now, we will hide the breakdown chart if data is missing, or mocked as 0.
  /*
  const outstandingData = [
    { name: 'Principal', amount: summary.principalOutstanding / 100 },
    { name: 'Interest', amount: summary.interestOutstanding / 100 },
    { name: 'Penalty', amount: summary.penaltyOutstanding / 100 },
  ];
  */

  return (
    <Box>
      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Dashboard
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Portfolio overview and key metrics
      </Typography>

      {/* KPI Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard
            title="Total Loans"
            value={summary.totalLoans || summary.activeLoans + summary.closedLoans + summary.defaultedLoans} // Fallback if totalLoans missing
            icon={<AccountBalanceIcon />}
            color="primary"
            subtitle={`${summary.activeLoans} active`}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard
            title="Total Disbursed"
            value={formatCurrency(summary.totalDisbursed)}
            icon={<TrendingUpIcon />}
            color="success"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard
            title="Total Outstanding"
            value={formatCurrency(summary.outstanding)}
            icon={<AccountBalanceIcon />}
            color="warning"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard
            title="Total Collected"
            value={formatCurrency(summary.totalCollected)}
            icon={<CheckCircleIcon />}
            color="success"
          />
        </Grid>
      </Grid>

      {/* Additional KPIs */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* Overdue Amount not in API currently
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <KpiCard
            title="Overdue Amount"
            value={formatCurrency(summary.overdueAmount)}
            icon={<WarningIcon />}
            color="error"
          />
        </Grid>
        */}
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <KpiCard
            title="Active Loans"
            value={summary.activeLoans}
            icon={<AccountBalanceIcon />}
            color="primary"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <KpiCard
            title="Closed Loans"
            value={summary.closedLoans}
            icon={<CheckCircleIcon />}
            color="success"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <KpiCard
            title="Defaulted Loans"
            value={summary.defaultedLoans}
            icon={<WarningIcon />}
            color="error"
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Loan Status Distribution
              </Typography>
              <Box sx={{ height: 300 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={loanStatusData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      outerRadius={100}
                      label
                    >
                      {loanStatusData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Removed Outstanding Breakdown Chart due to missing API data */}
      </Grid>
    </Box>
  );
};

export default DashboardPage;
