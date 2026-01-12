/**
 * Type definitions for API responses and entities
 * These types mirror the backend DTOs
 */

// ============ User & Auth Types ============
export interface User {
  id: string;
  email: string;
  token: string;
  role: UserRole;
  name: string;
}

export type UserRole = 'LENDER' | 'ADMIN' | 'AUDITOR';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  id: string;
  email: string;
  token: string;
  role: UserRole;
  name: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: UserRole;
}

// ============ Borrower Types ============
export interface Borrower {
  id: string;
  fullName: string;
  phone: string;
  email?: string;
  address?: string;
  riskScore?: number;
  status: BorrowerStatus;
  createdAt: string;
}

export type BorrowerStatus = 'ACTIVE' | 'BLOCKED';

export interface CreateBorrowerRequest {
  fullName: string;
  phone: string;
  email?: string;
  address?: string;
  riskScore?: number;
}

export interface UpdateBorrowerRequest {
  fullName?: string;
  email?: string;
  phone?: string;
  address?: string;
  riskScore?: number;
}

// ============ Loan Types ============
export interface Loan {
  id: string;
  loanId: string;
  borrowerId: string;
  borrowerName: string;
  principal: number;
  principalAmount: number;
  interestRate: number;
  interestType: InterestType;
  tenureMonths: number;
  emiAmount: number;
  totalInterest: number;
  totalPayable: number;
  outstandingPrincipal: number;
  outstandingInterest: number;
  outstandingPenalty: number;
  totalOutstanding: number;
  status: LoanStatus;
  createdAt: string;
  disbursedAt?: string;
  closedAt?: string;
}
export type InterestType = 'FLAT' | 'REDUCING';

export type LoanStatus = 'CREATED' | 'ACTIVE' | 'CLOSED' | 'DEFAULTED';

export interface CreateLoanRequest {
  borrowerId: string;
  principal: number;
  interestRate: number;
  interestType: InterestType;
  tenureMonths: number;
}

export interface DisburseLoanRequest {
  amount: number;
  mode: PaymentMode;
  reference?: string;
}

// ============ Repayment Schedule Types ============
export interface RepaymentSchedule {
  id: string;
  emiNo: number;
  dueDate: string;
  principalDue: number;
  interestDue: number;
  totalDue: number;
  principalPaid: number;
  interestPaid: number;
  penaltyPaid: number;
  principalRemaining: number;
  interestRemaining: number;
  status: EmiStatus;
}

export type EmiStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'OVERDUE';

// ============ Payment Types ============
export interface Payment {
  id: string;
  loanId: string;
  amountPaid: number;
  paymentDate: string;
  mode: PaymentMode;
  reference?: string;
  createdAt: string;
  allocations: PaymentAllocation[];
}

export interface PaymentAllocation {
  type: AllocationType;
  amount: number;
}

export type PaymentMode = 'CASH' | 'UPI' | 'BANK' | 'CHEQUE';

export type AllocationType = 'PENALTY' | 'INTEREST' | 'PRINCIPAL';

export interface RecordPaymentRequest {
  amount: number;
  paymentDate: string;
  mode: PaymentMode;
  reference?: string;
}

// ============ Report Types ============
export interface PortfolioSummary {
  totalDisbursed: number;
  outstanding: number;
  totalCollected: number;
  activeLoans: number;
  closedLoans: number;
  defaultedLoans: number;
  totalBorrowers: number;
  totalLoans: number;
}
export interface LoanStatement {
  loanId: string;
  borrowerName: string;
  principal: number;
  totalInterest: number;
  totalPayable: number;
  totalPaid: number;
  totalOutstanding: number;
  status: string;
  schedule: RepaymentSchedule[];
  payments: Payment[];
}

export interface CollectionsSummary {
  startDate: string;
  endDate: string;
  principalCollected: number;
  interestCollected: number;
  penaltyCollected: number;
  totalCollected: number;
}

export interface OverdueLoan {
  loanId: string;
  borrowerName: string;
  borrowerPhone: string;
  principalOutstanding: number;
  interestOutstanding: number;
  penaltyOutstanding: number;
  totalOutstanding: number;
  daysOverdue: number;
  overdueEmiCount: number;
}

// ============ Pagination Types ============
export interface PageRequest {
  page: number;
  size: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
}

// ============ Error Types ============
export interface ApiError {
  errorCode: string;
  message: string;
  timestamp: string;
  details?: Record<string, string>;
}
