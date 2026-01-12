/**
 * Payment API module
 * Handles all payment-related API calls
 */

import axiosInstance from './axios';
import { logger } from '../utils/logger';
import type { Payment, RecordPaymentRequest, PageResponse, PageRequest } from '../types';

const MODULE = 'PaymentAPI';

export const paymentApi = {
  /**
   * Record a payment for a loan
   */
  record: async (loanId: string, data: RecordPaymentRequest): Promise<Payment> => {
    logger.info(MODULE, `Recording payment for loan: ${loanId}`, { amount: data.amount });

    try {
      const response = await axiosInstance.post<Payment>(`/loans/${loanId}/payments`, data);
      logger.info(MODULE, `Payment recorded: ${response.data.id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to record payment for loan: ${loanId}`, error);
      throw error;
    }
  },

  /**
   * Get payments for a loan with pagination
   */
  getByLoan: async (loanId: string, params?: PageRequest): Promise<PageResponse<Payment>> => {
    logger.debug(MODULE, `Fetching payments for loan: ${loanId}`, params);

    try {
      const response = await axiosInstance.get<PageResponse<Payment>>(`/loans/${loanId}/payments`, { params });
      logger.debug(MODULE, `Fetched ${response.data.content.length} payments`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch payments for loan: ${loanId}`, error);
      throw error;
    }
  },

  /**
   * Get all payments for a loan (no pagination)
   */
  getAllByLoan: async (loanId: string): Promise<Payment[]> => {
    logger.debug(MODULE, `Fetching all payments for loan: ${loanId}`);

    try {
      const response = await axiosInstance.get<Payment[]>(`/loans/${loanId}/payments/all`);
      logger.debug(MODULE, `Fetched ${response.data.length} payments`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch all payments for loan: ${loanId}`, error);
      throw error;
    }
  },

  /**
   * Get all payments globally with pagination
   */
  getAll: async (params?: PageRequest): Promise<PageResponse<Payment>> => {
    logger.debug(MODULE, 'Fetching all payments', params);

    try {
      const response = await axiosInstance.get<PageResponse<Payment>>('/payments', { params });
      logger.debug(MODULE, `Fetched ${response.data.content.length} payments`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch all payments', error);
      throw error;
    }
  },
};

export default paymentApi;
