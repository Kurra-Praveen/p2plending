/**
 * Borrowers List Page
 */

import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Button,
  TextField,
  InputAdornment,
  Chip,
  IconButton,
  Alert,
  Menu,
  MenuItem,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import VisibilityIcon from '@mui/icons-material/Visibility';
import BlockIcon from '@mui/icons-material/Block';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { borrowerApi } from '../../api/borrower.api';
import type { Borrower, BorrowerStatus } from '../../types';
import { logger } from '../../utils/logger';

import DataTable from '../../components/tables/DataTable';

const MODULE = 'BorrowersPage';

const BorrowersPage: React.FC = () => {
  const navigate = useNavigate();
  const [borrowers, setBorrowers] = useState<Borrower[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedBorrower, setSelectedBorrower] = useState<Borrower | null>(null);

  logger.debug(MODULE, 'Rendering BorrowersPage');

  const fetchBorrowers = useCallback(async () => {
    logger.info(MODULE, 'Fetching borrowers', { page, rowsPerPage });
    setLoading(true);
    setError(null);

    try {
      const response = await borrowerApi.getAll({ page, size: rowsPerPage });
      logger.info(MODULE, `Fetched ${response.content.length} borrowers`);
      setBorrowers(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      logger.error(MODULE, 'Failed to fetch borrowers', err);
      setError('Failed to load borrowers');
    } finally {
      setLoading(false);
    }
  }, [page, rowsPerPage]);

  useEffect(() => {
    fetchBorrowers();
  }, [fetchBorrowers]);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, borrower: Borrower) => {
    setAnchorEl(event.currentTarget);
    setSelectedBorrower(borrower);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setSelectedBorrower(null);
  };

  const handleBlockToggle = async () => {
    if (!selectedBorrower) return;

    logger.info(MODULE, `Toggling block status for borrower: ${selectedBorrower.id}`);
    handleMenuClose();

    try {
      if (selectedBorrower.status === 'BLOCKED') {
        await borrowerApi.unblock(selectedBorrower.id);
      } else {
        await borrowerApi.block(selectedBorrower.id);
      }
      fetchBorrowers();
    } catch (err) {
      logger.error(MODULE, 'Failed to toggle block status', err);
      setError('Failed to update borrower status');
    }
  };

  const getStatusColor = (status: BorrowerStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'BLOCKED':
        return 'error';
      default:
        return 'default';
    }
  };

  const filteredBorrowers = borrowers.filter(
    (b) =>
      b.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      b.phone.includes(searchQuery) ||
      b.email?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const columns = [
    {
      id: 'fullName',
      label: 'Name',
      format: (value: string) => <Typography fontWeight="medium">{value}</Typography>,
    },
    { id: 'phone', label: 'Phone' },
    { id: 'email', label: 'Email', format: (value: string) => value || '-' },
    {
      id: 'status',
      label: 'Status',
      format: (value: BorrowerStatus) => (
        <Chip label={value} size="small" color={getStatusColor(value)} />
      ),
    },
    {
      id: 'createdAt',
      label: 'Created',
      format: (value: string) => new Date(value).toLocaleDateString(),
    },
    {
      id: 'actions',
      label: 'Actions',
      align: 'right' as const,
      format: (_: any, row: Borrower) => (
        <IconButton
          size="small"
          onClick={(e) => {
            e.stopPropagation();
            handleMenuOpen(e, row);
          }}
        >
          <MoreVertIcon />
        </IconButton>
      ),
    },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Borrowers
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage your borrower database
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/borrowers/new')}
        >
          Add Borrower
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Card>
        <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
          <TextField
            placeholder="Search borrowers..."
            size="small"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon color="action" />
                </InputAdornment>
              ),
            }}
            sx={{ width: 300 }}
          />
        </Box>

        <DataTable
          columns={columns}
          data={filteredBorrowers}
          loading={loading}
          totalElements={totalElements}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onRowClick={(row) => navigate(`/borrowers/${row.id}`)}
          emptyMessage="No borrowers found"
        />
      </Card>

      {/* Actions Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        <MenuItem
          onClick={() => {
            if (selectedBorrower) navigate(`/borrowers/${selectedBorrower.id}`);
            handleMenuClose();
          }}
        >
          <VisibilityIcon sx={{ mr: 1, fontSize: 20 }} />
          View Details
        </MenuItem>
        <MenuItem onClick={handleBlockToggle}>
          {selectedBorrower?.status === 'BLOCKED' ? (
            <>
              <CheckCircleIcon sx={{ mr: 1, fontSize: 20 }} />
              Unblock
            </>
          ) : (
            <>
              <BlockIcon sx={{ mr: 1, fontSize: 20 }} />
              Block
            </>
          )}
        </MenuItem>
      </Menu>
    </Box>
  );
};

export default BorrowersPage;
