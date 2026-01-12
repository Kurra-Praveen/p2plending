/**
 * Header Component
 * Contains user info and logout button
 */

import React from 'react';
import { Box, IconButton, Typography, Menu, MenuItem, Avatar, Chip } from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import PersonIcon from '@mui/icons-material/Person';
import { useNavigate } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from '../../app/hooks';
import { logout } from '../../features/auth/authSlice';
import { logger } from '../../utils/logger';

const MODULE = 'Header';

const Header: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { user } = useAppSelector((state) => state.auth);
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  logger.debug(MODULE, 'Rendering Header', { user: user?.email });

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logger.info(MODULE, 'User initiated logout');
    handleMenuClose();
    dispatch(logout());
    navigate('/login');
  };

  const getRoleColor = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return 'error';
      case 'LENDER':
        return 'primary';
      case 'AUDITOR':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', width: '100%', justifyContent: 'space-between' }}>
      <Typography variant="h6" component="div" sx={{ fontWeight: 500 }}>
        Lender Loan Management System
      </Typography>

      {user && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Chip
            label={user.role}
            size="small"
            color={getRoleColor(user.role)}
            variant="outlined"
          />

          <IconButton onClick={handleMenuOpen} size="small">
            <Avatar sx={{ width: 36, height: 36, bgcolor: 'primary.main' }}>
              <PersonIcon />
            </Avatar>
          </IconButton>

          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
          >
            <MenuItem disabled>
              <Box>
                <Typography variant="body2" fontWeight="medium">
                  {user.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {user.role}
                </Typography>
              </Box>
            </MenuItem>
            <MenuItem onClick={handleLogout}>
              <LogoutIcon sx={{ mr: 1, fontSize: 20 }} />
              Logout
            </MenuItem>
          </Menu>
        </Box>
      )}
    </Box>
  );
};

export default Header;
