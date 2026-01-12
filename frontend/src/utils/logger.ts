/**
 * Logger utility for consistent debugging across the application
 * Provides structured logging with timestamps and log levels
 */

type LogLevel = 'DEBUG' | 'INFO' | 'WARN' | 'ERROR';

interface LogEntry {
  timestamp: string;
  level: LogLevel;
  module: string;
  message: string;
  data?: unknown;
}

const isDevelopment = import.meta.env.DEV;

const formatTimestamp = (): string => {
  return new Date().toISOString();
};

const formatLog = (entry: LogEntry): string => {
  return `[${entry.timestamp}] [${entry.level}] [${entry.module}] ${entry.message}`;
};

const log = (level: LogLevel, module: string, message: string, data?: unknown): void => {
  if (!isDevelopment && level === 'DEBUG') {
    return; // Skip debug logs in production
  }

  const entry: LogEntry = {
    timestamp: formatTimestamp(),
    level,
    module,
    message,
    data,
  };

  const formattedMessage = formatLog(entry);

  switch (level) {
    case 'DEBUG':
      console.debug(formattedMessage, data ?? '');
      break;
    case 'INFO':
      console.info(formattedMessage, data ?? '');
      break;
    case 'WARN':
      console.warn(formattedMessage, data ?? '');
      break;
    case 'ERROR':
      console.error(formattedMessage, data ?? '');
      break;
  }
};

export const logger = {
  debug: (module: string, message: string, data?: unknown) => log('DEBUG', module, message, data),
  info: (module: string, message: string, data?: unknown) => log('INFO', module, message, data),
  warn: (module: string, message: string, data?: unknown) => log('WARN', module, message, data),
  error: (module: string, message: string, data?: unknown) => log('ERROR', module, message, data),
};

export default logger;
