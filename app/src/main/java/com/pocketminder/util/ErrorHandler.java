package com.pocketminder.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Centralized error handling and user feedback
 */
public class ErrorHandler {
    private static final String TAG = "ErrorHandler";

    /**
     * Handle and log exceptions with user-friendly messages
     */
    public static void handleError(Context context, Exception e, String operation) {
        Log.e(TAG, "Error during " + operation, e);

        String userMessage = getUserFriendlyMessage(e, operation);

        if (context != null) {
            ThreadManager.getInstance().executeMain(() ->
                Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
            );
        }
    }

    /**
     * Get user-friendly error message
     */
    private static String getUserFriendlyMessage(Exception e, String operation) {
        if (e instanceof UnknownHostException) {
            return "No internet connection. Please check your network.";
        } else if (e instanceof SocketTimeoutException) {
            return "Connection timeout. Please try again.";
        } else if (e instanceof IOException) {
            return "Network error occurred. Please check your connection.";
        } else if (e instanceof SecurityException) {
            return "Permission denied. Please grant required permissions.";
        } else {
            return "An error occurred during " + operation + ". Please try again.";
        }
    }

    /**
     * Retry logic with exponential backoff
     */
    public static <T> T retryOperation(RetryableOperation<T> operation, int maxAttempts) throws Exception {
        Exception lastException = null;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < maxAttempts) {
                    long delay = (long) (Math.pow(2, attempt) * 1000); // Exponential backoff
                    Log.w(TAG, "Attempt " + attempt + " failed, retrying in " + delay + "ms", e);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        throw lastException;
    }

    /**
     * Interface for retryable operations
     */
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Check if exception is recoverable
     */
    public static boolean isRecoverable(Exception e) {
        return e instanceof IOException ||
               e instanceof SocketTimeoutException ||
               e instanceof UnknownHostException;
    }
}
