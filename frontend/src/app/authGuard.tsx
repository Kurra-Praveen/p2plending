/**
 * AuthGuard component
 * Protects routes that require authentication
 * Redirects to login if not authenticated
 */

import React, { useEffect } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAppSelector, useAppDispatch } from './hooks';
import { logout } from '../features/auth/authSlice';
import { logger } from '../utils/logger';
import type { UserRole } from '../types';

const MODULE = 'AuthGuard';

interface AuthGuardProps {
  children: React.ReactNode;
  allowedRoles?: UserRole[];
}

export const AuthGuard: React.FC<AuthGuardProps> = ({ children, allowedRoles }) => {
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { isAuthenticated, user } = useAppSelector((state) => state.auth);

  logger.debug(MODULE, 'AuthGuard check', {
    path: location.pathname,
    isAuthenticated,
    userRole: user?.role,
    allowedRoles,
  });

  // Listen for global logout events (from axios interceptor)
  useEffect(() => {
    const handleLogout = () => {
      logger.warn(MODULE, 'Global logout event received');
      dispatch(logout());
    };

    const handleForbidden = () => {
      logger.warn(MODULE, 'Global forbidden event received');
      // Could navigate to a forbidden page
    };

    window.addEventListener('auth:logout', handleLogout);
    window.addEventListener('auth:forbidden', handleForbidden);

    return () => {
      window.removeEventListener('auth:logout', handleLogout);
      window.removeEventListener('auth:forbidden', handleForbidden);
    };
  }, [dispatch]);

  // Not authenticated - redirect to login
  if (!isAuthenticated) {
    logger.info(MODULE, 'User not authenticated, redirecting to login');
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check role-based access
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    logger.warn(MODULE, `User role ${user.role} not allowed. Required: ${allowedRoles.join(', ')}`);
    return <Navigate to="/forbidden" replace />;
  }

  logger.debug(MODULE, 'AuthGuard passed, rendering children');
  return <>{children}</>;
};

export default AuthGuard;
