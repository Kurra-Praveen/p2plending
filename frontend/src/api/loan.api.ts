/**
 * Loan API module
 * Handles all loan-related API calls
 */

import axiosInstance from './axios';
import { logger } from '../utils/logger';
import type {
  Loan,
  CreateLoanRequest,
  DisburseLoanRequest,
  RepaymentSchedule,
  PageResponse,
  PageRequest,
  LoanStatus,
} from '../types';

const MODULE = 'LoanAPI';

export const loanApi = {
  /**
   * Create a new loan
   */
  create: async (data: CreateLoanRequest): Promise<Loan> => {
    logger.info(MODULE, `Creating loan for borrower: ${data.borrowerId}`);

    try {
      const response = await axiosInstance.post<Loan>('/loans', data);
      logger.info(MODULE, `Loan created: ${response.data.id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to create loan', error);
      throw error;
    }
  },

  /**
   * Get loan by ID
   */
  getById: async (id: string): Promise<Loan> => {
    logger.debug(MODULE, `Fetching loan: ${id}`);

    try {
      const response = await axiosInstance.get<Loan>(`/loans/${id}`);
      logger.debug(MODULE, `Loan fetched: ${response.data.id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch loan: ${id}`, error);
      throw error;
    }
  },

  /**
   * Get all loans with pagination
   * Handles filtering by status or borrowerId
   */
  getAll: async (params?: PageRequest & { status?: LoanStatus; borrowerId?: string }): Promise<PageResponse<Loan>> => {
    logger.debug(MODULE, 'Fetching loans list', params);

    try {
      let url = '/loans';
      const requestParams: any = { ...params };

      // Determine the correct endpoint based on filters
      // Note: We prioritize borrowerId, then status, then default
      if (params?.borrowerId) {
        url = `/loans/borrower/${params.borrowerId}`;
        delete requestParams.borrowerId;
      } else if (params?.status) {
        url = `/loans/status/${params.status}`;
        delete requestParams.status;
      }

      const response = await axiosInstance.get<PageResponse<Loan>>(url, { params: requestParams });
      logger.debug(MODULE, `Fetched ${response.data.content.length} loans`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch loans', error);
      throw error;
    }
  },

  /**
   * Get loans by status
   */
  getByStatus: async (status: LoanStatus): Promise<Loan[]> => {
    logger.debug(MODULE, `Fetching loans by status: ${status}`);

    try {
      const response = await axiosInstance.get<Loan[]>(`/loans/status/${status}`);
      logger.debug(MODULE, `Fetched ${response.data.length} loans with status ${status}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch loans by status: ${status}`, error);
      throw error;
    }
  },

  /**
   * Disburse a loan
   */
  disburse: async (id: string, data: DisburseLoanRequest): Promise<Loan> => {
    logger.info(MODULE, `Disbursing loan: ${id}`);

    try {
      const response = await axiosInstance.post<Loan>(`/loans/${id}/disburse`, data);
      logger.info(MODULE, `Loan disbursed: ${id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to disburse loan: ${id}`, error);
      throw error;
    }
  },

  /**
   * Get repayment schedule for a loan
   */
  getSchedule: async (id: string): Promise<RepaymentSchedule[]> => {
    logger.debug(MODULE, `Fetching schedule for loan: ${id}`);

    try {
      const response = await axiosInstance.get<RepaymentSchedule[]>(`/loans/${id}/schedule`);
      logger.debug(MODULE, `Fetched ${response.data.length} EMI schedules`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch schedule for loan: ${id}`, error);
      throw error;
    }
  },

  /**
   * Close a loan
   */
  close: async (id: string): Promise<void> => {
    logger.info(MODULE, `Closing loan: ${id}`);

    try {
      await axiosInstance.post(`/loans/${id}/close`);
      logger.info(MODULE, `Loan closed: ${id}`);
    } catch (error) {
      logger.error(MODULE, `Failed to close loan: ${id}`, error);
      throw error;
    }
  },
};

export default loanApi;
