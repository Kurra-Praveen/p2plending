/**
 * Application Router
 * Defines all routes and their protection
 */

import React from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { logger } from '../utils/logger';

// Layout
import MainLayout from '../components/layout/MainLayout';

// Auth
import LoginPage from '../features/auth/LoginPage';
import SignupPage from '../features/auth/SignupPage';
import AuthGuard from './authGuard';

// Pages (lazy loaded for better performance)
const DashboardPage = React.lazy(() => import('../features/dashboard/DashboardPage'));
const BorrowersPage = React.lazy(() => import('../features/borrowers/BorrowersPage'));
const BorrowerDetailPage = React.lazy(() => import('../features/borrowers/BorrowerDetailPage'));
const CreateBorrowerPage = React.lazy(() => import('../features/borrowers/CreateBorrowerPage'));
const EditBorrowerPage = React.lazy(() => import('../features/borrowers/EditBorrowerPage'));
const LoansPage = React.lazy(() => import('../features/loans/LoansPage'));
const LoanDetailPage = React.lazy(() => import('../features/loans/LoanDetailPage'));
const CreateLoanPage = React.lazy(() => import('../features/loans/CreateLoanPage'));
const PaymentsPage = React.lazy(() => import('../features/payments/PaymentsPage'));
const RecordPaymentPage = React.lazy(() => import('../features/payments/RecordPaymentPage'));
const ReportsPage = React.lazy(() => import('../features/reports/ReportsPage'));
const OverduePage = React.lazy(() => import('../features/reports/OverduePage'));
const ForbiddenPage = React.lazy(() => import('../components/layout/ForbiddenPage'));
const NotFoundPage = React.lazy(() => import('../components/layout/NotFoundPage'));

import PageLoader from '../components/common/PageLoader';

const MODULE = 'Router';

logger.info(MODULE, 'Initializing application router');

// Wrap lazy components with Suspense
const withSuspense = (Component: React.LazyExoticComponent<React.FC>) => (
  <React.Suspense fallback={<PageLoader />}>
    <Component />
  </React.Suspense>
);

export const router = createBrowserRouter([
  // Public routes
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/signup',
    element: <SignupPage />,
  },

  // Protected routes
  {
    path: '/',
    element: (
      <AuthGuard>
        <MainLayout />
      </AuthGuard>
    ),
    children: [
      {
        index: true,
        element: <Navigate to="/dashboard" replace />,
      },
      {
        path: 'dashboard',
        element: withSuspense(DashboardPage),
      },
      // Borrowers
      {
        path: 'borrowers',
        element: withSuspense(BorrowersPage),
      },
      {
        path: 'borrowers/new',
        element: withSuspense(CreateBorrowerPage),
      },
      {
        path: 'borrowers/:id/edit',
        element: withSuspense(EditBorrowerPage),
      },
      {
        path: 'borrowers/:id',
        element: withSuspense(BorrowerDetailPage),
      },
      // Loans
      {
        path: 'loans',
        element: withSuspense(LoansPage),
      },
      {
        path: 'loans/new',
        element: withSuspense(CreateLoanPage),
      },
      {
        path: 'loans/:id',
        element: withSuspense(LoanDetailPage),
      },
      // Payments
      {
        path: 'payments',
        element: withSuspense(PaymentsPage),
      },
      {
        path: 'loans/:id/payments/new',
        element: withSuspense(RecordPaymentPage),
      },
      // Reports
      {
        path: 'reports',
        element: withSuspense(ReportsPage),
      },
      {
        path: 'overdue',
        element: withSuspense(OverduePage),
      },
    ],
  },

  // Error pages
  {
    path: '/forbidden',
    element: withSuspense(ForbiddenPage),
  },
  {
    path: '*',
    element: withSuspense(NotFoundPage),
  },
]);

export default router;
