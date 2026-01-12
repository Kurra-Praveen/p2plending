# Lender Loan Management System (LLMS) - Frontend

The frontend application for the Lender Loan Management System, built with React, TypeScript, and Vite. This application provides a secure interface for lenders to manage borrowers, loans, payments, and view reports.

## 🚀 Getting Started

### Prerequisites

- Node.js (v18 or higher)
- npm or yarn

### Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure environment variables:
   - Create a `.env` file in the frontend root (copy from `.env.example` if available)
   - Set the API URL:
     ```
     VITE_API_URL=http://localhost:8080/api/v1
     ```

### Running the Application

Start the development server:
```bash
npm run dev
```
The application will be available at `http://localhost:5173`.

### Building for Production

Build the application for deployment:
```bash
npm run build
```
The output will be in the `dist` directory.

## 🏗 Project Structure

```
src/
├── api/            # API integration layer (Axios + Service calls)
├── app/            # Global app configuration (Store, Router, AuthGuard)
├── assets/         # Static assets (Images, Fonts)
├── components/     # Reusable UI components
│   ├── charts/     # Recharts components
│   ├── forms/      # Form elements
│   ├── layout/     # MainLayout, Sidebar, Header
│   └── tables/     # Data tables
├── features/       # Feature-based modules
│   ├── auth/       # Login & Authentication
│   ├── borrowers/  # Borrower management
│   ├── dashboard/  # Main dashboard
│   ├── loans/      # Loan management
│   ├── payments/   # Payment recording & history
│   └── reports/    # Reporting & Analytics
├── types/          # TypeScript type definitions (DTOs)
└── utils/          # Helper functions & Logger
```

## 🛠 Tech Stack

- **Framework:** React 19
- **Language:** TypeScript
- **Build Tool:** Vite
- **UI Library:** Material-UI (MUI) v6
- **State Management:** Redux Toolkit
- **Routing:** React Router v7
- **Forms:** React Hook Form + Zod Validation
- **Charts:** Recharts
- **HTTP Client:** Axios

## 🔐 Key Features

- **Authentication:** JWT-based secure login with role-based access control.
- **Dashboard:** Real-time overview of portfolio performance (Disbursed, Outstanding, etc.).
- **Borrower Management:** Create, view, block, and manage borrowers.
- **Loan Management:** comprehensive loan creation with support for:
  - Reducing Balance & Flat Rate interest
  - Flexible tenure
  - Automatic EMI schedule generation
- **Payment Processing:** Record payments via multiple modes (Cash, UPI, Bank) with auto-allocation (Penalty → Interest → Principal).
- **Reports:**
  - Collection Summary
  - Overdue Loans tracking
  - Loan Statements

## 🧪 Testing

Run the test suite (once implemented):
```bash
npm test
```

## 📝 Coding Standards

- **TypeScript:** Strict type checking is enabled. Avoid `any`.
- **Components:** Functional components with Hooks.
- **State:** Local state for UI, Redux for global data (Auth, User).
- **Styling:** MUI `sx` prop or styled components.
