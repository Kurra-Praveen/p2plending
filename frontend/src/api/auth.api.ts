/**
 * Authentication API module
 * Handles login, register, and token management
 */

import axiosInstance, { tokenManager } from './axios';
import { logger } from '../utils/logger';
import type { LoginRequest, LoginResponse, RegisterRequest } from '../types';

const MODULE = 'AuthAPI';

export const authApi = {
  /**
   * Login with email and password
   */
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    logger.info(MODULE, `Attempting login for: ${credentials.email}`);

    try {
      const response = await axiosInstance.post<LoginResponse>('/auth/login', credentials);

      logger.info(MODULE, `Login successful for: ${credentials.email}`);

      // Store the token
      tokenManager.setToken(response.data.token);

      return response.data;
    } catch (error) {
      logger.error(MODULE, `Login failed for: ${credentials.email}`, error);
      throw error;
    }
  },

  /**
   * Register a new user
   */
  register: async (userData: RegisterRequest): Promise<LoginResponse> => {
    logger.info(MODULE, `Attempting registration for: ${userData.email}`);

    try {
      const response = await axiosInstance.post<LoginResponse>('/auth/register', userData);

      logger.info(MODULE, `Registration successful for: ${userData.email}`);

      // Auto-login after register (set token)
      tokenManager.setToken(response.data.token);

      return response.data;
    } catch (error) {
      logger.error(MODULE, `Registration failed for: ${userData.email}`, error);
      throw error;
    }
  },

  /**
   * Logout - clears token
   */
  logout: (): void => {
    logger.info(MODULE, 'Logging out user');
    tokenManager.removeToken();
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated: (): boolean => {
    const hasToken = tokenManager.hasToken();
    logger.debug(MODULE, `Auth check: ${hasToken ? 'authenticated' : 'not authenticated'}`);
    return hasToken;
  },

  /**
   * Get stored token
   */
  getToken: (): string | null => {
    return tokenManager.getToken();
  },
};

export default authApi;
