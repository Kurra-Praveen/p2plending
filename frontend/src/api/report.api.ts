/**
 * Report API module
 * Handles all reporting-related API calls
 */

import axiosInstance from './axios';
import { logger } from '../utils/logger';
import type { PortfolioSummary, LoanStatement, CollectionsSummary, OverdueLoan } from '../types';

const MODULE = 'ReportAPI';

export const reportApi = {
  /**
   * Get portfolio summary
   */
  getPortfolioSummary: async (): Promise<PortfolioSummary> => {
    logger.info(MODULE, 'Fetching portfolio summary');

    try {
      const response = await axiosInstance.get<PortfolioSummary>('/reports/portfolio');
      logger.info(MODULE, 'Portfolio summary fetched');
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch portfolio summary', error);
      throw error;
    }
  },

  /**
   * Get loan statement
   */
  getLoanStatement: async (loanId: string): Promise<LoanStatement> => {
    logger.debug(MODULE, `Fetching loan statement: ${loanId}`);

    try {
      const response = await axiosInstance.get<LoanStatement>(`/reports/loans/${loanId}/statement`);
      logger.debug(MODULE, `Loan statement fetched: ${loanId}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch loan statement: ${loanId}`, error);
      throw error;
    }
  },

  /**
   * Get collections summary
   */
  getCollectionsSummary: async (startDate: string, endDate: string): Promise<CollectionsSummary> => {
    logger.info(MODULE, `Fetching collections summary: ${startDate} to ${endDate}`);

    try {
      const response = await axiosInstance.get<CollectionsSummary>('/reports/collections', {
        params: { startDate, endDate },
      });
      logger.info(MODULE, 'Collections summary fetched');
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch collections summary', error);
      throw error;
    }
  },

  /**
   * Get overdue loans
   */
  getOverdueLoans: async (): Promise<OverdueLoan[]> => {
    logger.info(MODULE, 'Fetching overdue loans');

    try {
      const response = await axiosInstance.get<OverdueLoan[]>('/reports/overdue');
      logger.info(MODULE, `Fetched ${response.data.length} overdue loans`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch overdue loans', error);
      throw error;
    }
  },
};

export default reportApi;
