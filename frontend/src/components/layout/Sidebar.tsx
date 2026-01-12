/**
 * Sidebar Navigation Component
 */

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Divider,
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import PaymentIcon from '@mui/icons-material/Payment';
import AssessmentIcon from '@mui/icons-material/Assessment';
import WarningIcon from '@mui/icons-material/Warning';
import { useAppSelector } from '../../app/hooks';
import { logger } from '../../utils/logger';

const MODULE = 'Sidebar';

interface NavItem {
  label: string;
  path: string;
  icon: React.ReactNode;
  roles?: string[];
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
  { label: 'Borrowers', path: '/borrowers', icon: <PeopleIcon /> },
  { label: 'Loans', path: '/loans', icon: <AccountBalanceIcon /> },
  { label: 'Payments', path: '/payments', icon: <PaymentIcon /> },
  { label: 'Reports', path: '/reports', icon: <AssessmentIcon /> },
  { label: 'Overdue', path: '/overdue', icon: <WarningIcon /> },
];

interface SidebarProps {
  onItemClick?: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ onItemClick }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAppSelector((state) => state.auth);

  logger.debug(MODULE, 'Rendering Sidebar', { currentPath: location.pathname });

  const handleNavigation = (path: string) => {
    logger.info(MODULE, `Navigating to: ${path}`);
    navigate(path);
    onItemClick?.();
  };

  const filteredItems = navItems.filter((item) => {
    if (!item.roles) return true;
    return user && item.roles.includes(user.role);
  });

  return (
    <Box sx={{ height: '100%', bgcolor: 'primary.main', color: 'white' }}>
      {/* Logo / Brand */}
      <Box sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 1 }}>
        <AccountBalanceIcon sx={{ fontSize: 32 }} />
        <Typography variant="h6" fontWeight="bold">
          LLMS
        </Typography>
      </Box>

      <Divider sx={{ borderColor: 'rgba(255,255,255,0.2)' }} />

      {/* Navigation Items */}
      <List sx={{ px: 1, py: 2 }}>
        {filteredItems.map((item) => {
          const isActive = location.pathname.startsWith(item.path);

          return (
            <ListItem key={item.path} disablePadding sx={{ mb: 0.5 }}>
              <ListItemButton
                onClick={() => handleNavigation(item.path)}
                sx={{
                  borderRadius: 1,
                  bgcolor: isActive ? 'rgba(255,255,255,0.15)' : 'transparent',
                  '&:hover': {
                    bgcolor: 'rgba(255,255,255,0.1)',
                  },
                }}
              >
                <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  primaryTypographyProps={{
                    fontWeight: isActive ? 600 : 400,
                  }}
                />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>

      {/* User Info at Bottom */}
      {user && (
        <Box
          sx={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            p: 2,
            borderTop: '1px solid rgba(255,255,255,0.2)',
          }}
        >
          <Typography variant="body2" sx={{ opacity: 0.8 }}>
            Logged in as
          </Typography>
          <Typography variant="body1" fontWeight="medium">
            {user.name}
          </Typography>
          <Typography variant="caption" sx={{ opacity: 0.6 }}>
            {user.role}
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default Sidebar;
