/**
 * Borrower API module
 * Handles all borrower-related API calls
 */

import axiosInstance from './axios';
import { logger } from '../utils/logger';
import type {
  Borrower,
  CreateBorrowerRequest,
  UpdateBorrowerRequest,
  PageResponse,
  PageRequest,
  BorrowerStatus,
} from '../types';

const MODULE = 'BorrowerAPI';

export const borrowerApi = {
  /**
   * Create a new borrower
   */
  create: async (data: CreateBorrowerRequest): Promise<Borrower> => {
    logger.info(MODULE, `Creating borrower: ${data.fullName}`);

    try {
      const response = await axiosInstance.post<Borrower>('/borrowers', data);
      logger.info(MODULE, `Borrower created: ${response.data.id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to create borrower', error);
      throw error;
    }
  },

  /**
   * Get borrower by ID
   */
  getById: async (id: string): Promise<Borrower> => {
    logger.debug(MODULE, `Fetching borrower: ${id}`);

    try {
      const response = await axiosInstance.get<Borrower>(`/borrowers/${id}`);
      logger.debug(MODULE, `Borrower fetched: ${response.data.fullName}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to fetch borrower: ${id}`, error);
      throw error;
    }
  },

  /**
   * Get all borrowers with pagination
   * Handles filtering by status if provided
   */
  getAll: async (params?: PageRequest & { status?: BorrowerStatus }): Promise<PageResponse<Borrower>> => {
    logger.debug(MODULE, 'Fetching borrowers list', params);

    try {
      let url = '/borrowers';
      const requestParams = { ...params };

      // If status is provided, use the specific endpoint and remove from query params
      if (params?.status) {
        url = `/borrowers/status/${params.status}`;
        delete requestParams.status;
      }

      const response = await axiosInstance.get<PageResponse<Borrower>>(url, { params: requestParams });
      logger.debug(MODULE, `Fetched ${response.data.content.length} borrowers`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, 'Failed to fetch borrowers', error);
      throw error;
    }
  },

  /**
   * Update borrower
   */
  update: async (id: string, data: UpdateBorrowerRequest): Promise<Borrower> => {
    logger.info(MODULE, `Updating borrower: ${id}`);

    try {
      const response = await axiosInstance.put<Borrower>(`/borrowers/${id}`, data);
      logger.info(MODULE, `Borrower updated: ${id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to update borrower: ${id}`, error);
      throw error;
    }
  },

  /**
   * Block borrower
   */
  block: async (id: string): Promise<Borrower> => {
    logger.info(MODULE, `Blocking borrower: ${id}`);

    try {
      const response = await axiosInstance.post<Borrower>(`/borrowers/${id}/block`);
      logger.info(MODULE, `Borrower blocked: ${id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to block borrower: ${id}`, error);
      throw error;
    }
  },

  /**
   * Unblock borrower
   */
  unblock: async (id: string): Promise<Borrower> => {
    logger.info(MODULE, `Unblocking borrower: ${id}`);

    try {
      const response = await axiosInstance.post<Borrower>(`/borrowers/${id}/unblock`);
      logger.info(MODULE, `Borrower unblocked: ${id}`);
      return response.data;
    } catch (error) {
      logger.error(MODULE, `Failed to unblock borrower: ${id}`, error);
      throw error;
    }
  },

  /**
   * Delete borrower (soft delete)
   */
  delete: async (id: string): Promise<void> => {
    logger.info(MODULE, `Deleting borrower: ${id}`);

    try {
      await axiosInstance.delete(`/borrowers/${id}`);
      logger.info(MODULE, `Borrower deleted: ${id}`);
    } catch (error) {
      logger.error(MODULE, `Failed to delete borrower: ${id}`, error);
      throw error;
    }
  },
};

export default borrowerApi;
