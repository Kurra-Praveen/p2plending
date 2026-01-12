/**
 * Authentication Redux slice
 * Manages auth state, login/logout actions
 */

import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { authApi } from '../../api/auth.api';
import { tokenManager } from '../../api/axios';
import { logger } from '../../utils/logger';
import type { User, LoginRequest, LoginResponse, UserRole, RegisterRequest } from '../../types';
import type { PayloadAction } from '@reduxjs/toolkit';

const MODULE = 'AuthSlice';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

// Try to restore user from token (basic restoration, user info from JWT payload)
const getInitialState = (): AuthState => {
  const token = tokenManager.getToken();
  let user: User | null = null;

  if (token) {
    try {
      // Decode JWT to get user info (for display only, NOT for authorization)
      const payload = JSON.parse(atob(token.split('.')[1]));
      user = {
        token: token,
        name: payload.name || payload.sub, // Fallback to subject if name not present
        role: payload.role as UserRole,
      };
      logger.info(MODULE, 'Restored user session from token', { role: user.role });
    } catch {
      logger.warn(MODULE, 'Failed to decode token, clearing');
      tokenManager.removeToken();
    }
  }

  return {
    user,
    token,
    isAuthenticated: !!token,
    isLoading: false,
    error: null,
  };
};

// Async thunk for login
export const login = createAsyncThunk<LoginResponse, LoginRequest, { rejectValue: string }>(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    logger.info(MODULE, `Login thunk executing for: ${credentials.email}`);

    try {
      const response = await authApi.login(credentials);
      logger.info(MODULE, 'Login thunk success');
      return response;
    } catch (error: unknown) {
      logger.error(MODULE, 'Login thunk failed', error);

      // Extract error message
      const err = error as { response?: { data?: { message?: string } } };
      const message = err.response?.data?.message || 'Login failed. Please try again.';
      return rejectWithValue(message);
    }
  }
);

// Async thunk for register
export const register = createAsyncThunk<LoginResponse, RegisterRequest, { rejectValue: string }>(
  'auth/register',
  async (userData, { rejectWithValue }) => {
    logger.info(MODULE, `Register thunk executing for: ${userData.email}`);

    try {
      const response = await authApi.register(userData);
      logger.info(MODULE, 'Register thunk success');
      return response;
    } catch (error: unknown) {
      logger.error(MODULE, 'Register thunk failed', error);

      // Extract error message
      const err = error as { response?: { data?: { message?: string } } };
      const message = err.response?.data?.message || 'Registration failed. Please try again.';
      return rejectWithValue(message);
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState: getInitialState(),
  reducers: {
    logout: (state) => {
      logger.info(MODULE, 'Logout action dispatched');
      authApi.logout();
      state.user = null;
      state.token = null;
      state.isAuthenticated = false;
      state.error = null;
    },
    clearError: (state) => {
      state.error = null;
    },
    setUser: (state, action: PayloadAction<User>) => {
      state.user = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        logger.debug(MODULE, 'Login pending');
        state.isLoading = true;
        state.error = null;
      })
      .addCase(login.fulfilled, (state, action) => {
        logger.info(MODULE, 'Login fulfilled', { role: action.payload.role });
        state.isLoading = false;
        state.isAuthenticated = true;
        // Construct user object from response
        state.user = {
          token: action.payload.token,
          name: action.payload.name,
          role: action.payload.role,
        };
        state.token = action.payload.token;
        state.error = null;
      })
      .addCase(login.rejected, (state, action) => {
        logger.error(MODULE, 'Login rejected', action.payload);
        state.isLoading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = action.payload || 'Login failed';
      })
      .addCase(register.pending, (state) => {
        logger.debug(MODULE, 'Register pending');
        state.isLoading = true;
        state.error = null;
      })
      .addCase(register.fulfilled, (state, action) => {
        logger.info(MODULE, 'Register fulfilled', { role: action.payload.role });
        state.isLoading = false;
        state.isAuthenticated = true;
        // Construct user object from response
        state.user = {
          token: action.payload.token,
          name: action.payload.name,
          role: action.payload.role,
        };
        state.token = action.payload.token;
        state.error = null;
      })
      .addCase(register.rejected, (state, action) => {
        logger.error(MODULE, 'Register rejected', action.payload);
        state.isLoading = false;
        state.isAuthenticated = false;
        state.user = null;
        state.token = null;
        state.error = action.payload || 'Registration failed';
      });
  },
});

export const { logout, clearError, setUser } = authSlice.actions;
export default authSlice.reducer;
