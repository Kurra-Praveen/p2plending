/**
 * Currency Utility Functions
 *
 * The backend stores all monetary values in PAISE (smallest currency unit).
 * 1 Rupee = 100 Paise
 *
 * These utilities ensure consistent conversion between:
 * - Backend values (paise) for API communication
 * - Frontend display values (rupees) for user interface
 */

/**
 * Convert paise to rupees
 * @param paise - Amount in paise (from backend)
 * @returns Amount in rupees
 */
export const paiseToRupees = (paise: number): number => {
  return paise / 100;
};

/**
 * Convert rupees to paise
 * @param rupees - Amount in rupees (from user input)
 * @returns Amount in paise (for backend API)
 */
export const rupeesToPaise = (rupees: number): number => {
  // Round to avoid floating point precision issues
  return Math.round(rupees * 100);
};

/**
 * Format paise value as INR currency string for display
 * Shows full precision including paise when present
 * @param paise - Amount in paise (from backend)
 * @returns Formatted currency string (e.g., "₹1,00,000" or "₹1,00,000.50")
 */
export const formatCurrency = (paise: number): string => {
  const rupees = paiseToRupees(paise);

  // Check if there are paise (decimal portion)
  const hasPaise = rupees % 1 !== 0;

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: hasPaise ? 2 : 0,
    maximumFractionDigits: 2,
  }).format(rupees);
};

/**
 * Format paise value as INR currency string without paise (whole rupees only)
 * @param paise - Amount in paise (from backend)
 * @returns Formatted currency string (e.g., "₹1,00,000")
 */
export const formatCurrencyWhole = (paise: number): string => {
  const rupees = paiseToRupees(paise);

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(rupees);
};

/**
 * Format paise value as a plain number string in rupees (no currency symbol)
 * Useful for input fields
 * @param paise - Amount in paise (from backend)
 * @returns Formatted number string (e.g., "1,00,000.50")
 */
export const formatAmount = (paise: number): string => {
  const rupees = paiseToRupees(paise);

  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(rupees);
};

/**
 * Parse a rupee amount string to paise
 * Handles formatted strings like "1,00,000.50"
 * @param amountStr - Amount string in rupees
 * @returns Amount in paise
 */
export const parseAmountToPaise = (amountStr: string): number => {
  // Remove currency symbol and commas
  const cleanedStr = amountStr.replace(/[₹,\s]/g, '');
  const rupees = parseFloat(cleanedStr);

  if (isNaN(rupees)) {
    return 0;
  }

  return rupeesToPaise(rupees);
};
