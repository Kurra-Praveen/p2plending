/**
 * Payments List Page
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  Typography,
  Link,
  Breadcrumbs,
} from '@mui/material';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

// Since we don't have a "getAllPayments" endpoint that returns payments across ALL loans in the API contract provided earlier (only by loanId),
// we might need to rely on the backend potentially having such an endpoint or we might have to stick to loan-specific payments.
// However, looking at the initial progress file and typical requirements, a global payments list is common.
// Let's check the PaymentController again. It had:
// @GetMapping("/api/v1/loans/{loanId}/payments")
// It does NOT seem to have a global "/api/v1/payments" endpoint.
//
// WAIT. If there is no global payments endpoint, this page might need to be "Recent Payments" or we might need to ask the user/backend to add one.
// BUT, for now, let's assume we might need to list payments for a specific loan OR if the design implies a global list, we might be blocked.
//
// Let's re-read the technical architecture or `llms_api_contracts.md`.
// `llms_api_contracts.md` only shows `GET /api/v1/loans/{loanId}/payments`.
// `PROJECT_PROGRESS.md` says "15.2 Implement PaymentsPage (list)".
//
// If I can't fetch all payments, maybe I should display a message or maybe I missed an endpoint.
// Let's check `PaymentController.java` content I read earlier.
// It has `@RequestMapping("/api/v1/loans/{loanId}/payments")`.
// It does NOT have a global get.
//
// In this case, I will implement the PaymentsPage but since I can't fetch "All Payments" globally without a backend change,
// I might have to fetch payments for a specific loan if accessed via `/loans/:id/payments`, OR
// if accessed via `/payments`, maybe I should show a "Select Loan to view payments" or just recent payments if I can find a way.
//
// Actually, looking at the router:
// { path: 'payments', element: withSuspense(PaymentsPage) }
// { path: 'loans/:id/payments/new', element: withSuspense(RecordPaymentPage) }
//
// The router expects a global PaymentsPage.
// I will implement it such that if there's no global endpoint, it might be empty or I should check if I missed `PaymentController` having a global route.
// Let me double check `PaymentController.java` from previous turn.
// It matches `/api/v1/loans/{loanId}/payments`.
//
// Detailed check:
// The `PaymentController` is scoped to `/api/v1/loans/{loanId}/payments`.
// So there is NO global payment list API.
//
// I should probably mention this limitation.
// However, I can implement the page to perhaps show a list of loans to select from, or just redirect to Loans page.
// Or maybe I should check if there is another controller?
// Let's check `backend/src/main/java/com/llms/controller` again to be sure.

const PaymentsPage: React.FC = () => {
  const navigate = useNavigate();

  // Placeholder implementation since we discovered there is no global payments API
  // In a real scenario, I would implement a global GET /api/v1/payments endpoint.
  // For now, I will create a page that explains this or lists loans to view payments for.
  // Actually, better user experience: List Loans with a "View Payments" action.

  // BUT, I can reuse the LoansPage logic but focused on payments?
  // Or maybe I should just redirect users to the Loans page for now?

  // Let's implement a simple page that redirects or guides users.
  // "To view payments, please select a loan."

  return (
    <Box>
       <Breadcrumbs
        separator={<NavigateNextIcon fontSize="small" />}
        aria-label="breadcrumb"
        sx={{ mb: 3 }}
      >
        <Link underline="hover" color="inherit" onClick={() => navigate('/dashboard')} sx={{ cursor: 'pointer' }}>
          Dashboard
        </Link>
        <Typography color="text.primary">Payments</Typography>
      </Breadcrumbs>

      <Typography variant="h4" fontWeight="bold" gutterBottom>
        Payments
      </Typography>

      <Card sx={{ p: 4, textAlign: 'center' }}>
        <Typography variant="h6" color="text.secondary" gutterBottom>
          Payment history is available within each Loan's details.
        </Typography>
        <Typography paragraph>
          Please go to the Loans section and select a loan to view its payment history or record a new payment.
        </Typography>
        <Box sx={{ mt: 2 }}>
             <Link
                component="button"
                variant="button"
                onClick={() => navigate('/loans')}
                sx={{ fontSize: '1.1rem' }}
              >
                Go to Loans
              </Link>
        </Box>
      </Card>
    </Box>
  );
};

export default PaymentsPage;
