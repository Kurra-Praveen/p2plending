/**
 * Axios HTTP client configuration
 * Handles authentication tokens, error interceptors, and logging
 */

import axios from 'axios';
import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { logger } from '../utils/logger';
import type { ApiError } from '../types';

const MODULE = 'AxiosClient';

// API base URL from environment or default
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

logger.info(MODULE, `Initializing Axios client with base URL: ${API_BASE_URL}`);

// Create axios instance
const axiosInstance: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Token storage key
const TOKEN_KEY = 'llms_auth_token';

// Token management functions
export const tokenManager = {
  getToken: (): string | null => {
    const token = sessionStorage.getItem(TOKEN_KEY);
    logger.debug(MODULE, `Getting token: ${token ? 'exists' : 'not found'}`);
    return token;
  },

  setToken: (token: string): void => {
    logger.info(MODULE, 'Setting auth token');
    sessionStorage.setItem(TOKEN_KEY, token);
  },

  removeToken: (): void => {
    logger.info(MODULE, 'Removing auth token');
    sessionStorage.removeItem(TOKEN_KEY);
  },

  hasToken: (): boolean => {
    return !!sessionStorage.getItem(TOKEN_KEY);
  },
};

// Request interceptor - adds auth token
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenManager.getToken();

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    logger.debug(MODULE, `Request: ${config.method?.toUpperCase()} ${config.url}`, {
      params: config.params,
      hasAuth: !!token,
    });

    return config;
  },
  (error: AxiosError) => {
    logger.error(MODULE, 'Request interceptor error', error);
    return Promise.reject(error);
  }
);

// Response interceptor - handles errors
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    logger.debug(MODULE, `Response: ${response.status} ${response.config.url}`, {
      dataSize: JSON.stringify(response.data).length,
    });
    return response;
  },
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status;
    const url = error.config?.url;
    const apiError = error.response?.data;

    logger.error(MODULE, `Response error: ${status} ${url}`, {
      errorCode: apiError?.errorCode,
      message: apiError?.message,
    });

    // Handle specific status codes
    if (status === 401) {
      logger.warn(MODULE, 'Unauthorized - clearing token and redirecting to login');
      tokenManager.removeToken();
      // Dispatch logout event for global handling
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }

    if (status === 403) {
      logger.warn(MODULE, 'Forbidden - user does not have permission');
      window.dispatchEvent(new CustomEvent('auth:forbidden'));
    }

    return Promise.reject(error);
  }
);

export default axiosInstance;
